package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
    findAndRegisterModules()
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

@Component
class MangoParser : Parser {

    private val titleRegex = Regex("\\s\\|.*")
    private val utmRegex = Regex("\\.html.*")

    override fun getType(): ParserType = ParserType.MANGO

    override fun getItemInfo(url: String): ItemInfo {
        val document = Jsoup.connect(url).get()
        val productInfo = document.toProductInfo()
        val title = document.title().replace(titleRegex, "")
        val productDetails = productInfo.ecommerce.detail.products.first()
        return ItemInfo(
            url = url.cleanUTM(),
            price = productDetails.salePrice,
            priceCurrency = productDetails.currency,
            name = title,
            additionalInfo = AdditionalInfo(sizes = productDetails.sizeInfos())
        )
    }

    private fun Document?.toProductInfo(): TopLevelProduct {
        if (this == null) throw RuntimeException("Empty result")
        val data = this.getElementsByTag("script").first { it.data().contains("originalPrice") }.data()
        val json = "{" + data.substringAfter("{").substringBeforeLast("}") + "}"
        return objectMapper.readValue(json)
    }

    private data class TopLevelProduct(
        val ecommerce: Product
    )

    private data class Product(
        val detail: ProductDetails
    )

    private data class ProductDetails(
        val products: List<ProductInfo>
    )

    private data class ProductInfo(
        val salePrice: BigDecimal,
        val currency: String,
        val sizeAvailability: String?,
        val sizeNoAvailability: String?
    ) {
        fun sizeInfos(): List<SizeInfo> {
            return sizeAvailability.toSizeInfos(true) + sizeNoAvailability.toSizeInfos(false)
        }

        private fun String?.toSizeInfos(isAvailable: Boolean): List<SizeInfo> {
            if (this == null || this == NO_SIZES) return emptyList()
            return split(",").map { SizeInfo(it, isAvailable) }
        }
    }

    private fun String.cleanUTM() = this.replace(utmRegex, ".html")
}

private const val NO_SIZES = "ninguno"
