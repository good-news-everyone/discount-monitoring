package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.domain.OutdatedItemException
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.parser.ProxyDictionary
import mu.KotlinLogging
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class ZaraParser(
    private val objectMapper: ObjectMapper,
    private val proxyDictionary: ProxyDictionary
) : Parser {
    private val log = KotlinLogging.logger {}

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
        val info = this.getElementsByAttributeValue("type", "text/javascript").first {
            it.data().trim().startsWith(SIZES_INFO_MARKER)
        }.data().substringAfter(PRODUCT_INFO_MARKER)
        return objectMapper.readValue(info, ProductParseData::class.java).nonNull()
    }

    // до лучших времен
    private fun getDocumentWithRetries(url: String): Document {
        // 3 retries
        (1..3).forEach {
            val (host, port) = proxyDictionary.random()
            try {
                return Jsoup.connect(url).proxy(host, port).get()
            } catch (ex: Exception) {
                log.error { "${ex.javaClass} for proxy = $host:$port. Retry #$it for item $url" }
            }
        }
        log.warn { "Retrieving item info without proxy" }
        return Jsoup.connect(url).get()
    }
}

private const val SIZES_INFO_MARKER = "window.zara.appConfig"
private const val PRODUCT_INFO_MARKER = "window.zara.viewPayload = "

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
