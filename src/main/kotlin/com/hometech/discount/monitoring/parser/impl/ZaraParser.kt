package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.domain.OutdatedItemException
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

private const val SIZES_INFO_MARKER = "window.zara.appConfig"
private const val PRODUCT_INFO_MARKER = "window.zara.viewPayload = "

private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
    findAndRegisterModules()
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

@Component
class ZaraParser : Parser {

    override fun getType(): ParserType = ParserType.ZARA

    override fun getItemInfo(url: String): ItemInfo {
        val document = try {
            Jsoup.connect(url).get()
        } catch (e: HttpStatusException) {
            throw OutdatedItemException(url)
        }
        val productInfo = document.toProductInfo()
        return ItemInfo(
            url = url,
            name = productInfo.name(),
            price = productInfo.price(),
            priceCurrency = productInfo.currency(),
            additionalInfo = AdditionalInfo(productInfo.sizes())
        )
    }

    private fun Document?.toProductInfo(): ProductParseData {
        if (this == null) throw RuntimeException("Empty result")
        val info = getElementsByAttributeValue("type", "text/javascript").first {
            it.data().trim().startsWith(SIZES_INFO_MARKER)
        }.data().substringAfter(PRODUCT_INFO_MARKER)
        return objectMapper.readValue<ProductParseData>(info).nonNull()
    }
}

private data class ProductParseData(
    val product: ProductInfo,
    val analyticsData: AnalyticsInfo
) {
    fun name() = product.name
    fun currency() = analyticsData.page.currency
    fun price(): BigDecimal = product.detail.colors.first().price.toBigDecimal().divide(BigDecimal(100), RoundingMode.HALF_UP)
    fun sizes() = product.detail.colors.first().sizes.map { SizeInfo(name = it.name, availability = it.isAvailable()) }
}

private data class ProductInfo(
    val name: String,
    val detail: ProductDetailInfo
)

private data class ProductDetailInfo(
    val colors: List<ProductDetailColorsInfo>
)

private data class ProductDetailColorsInfo(
    val price: Int,
    val sizes: List<ProductSizeInfo>
)

private data class ProductSizeInfo(
    val name: String,
    val availability: String
) {
    fun isAvailable() = availability == "in_stock"
}

private data class AnalyticsInfo(
    val page: AnalyticsPageInfo
)

private data class AnalyticsPageInfo(
    val currency: String
)
