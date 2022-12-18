package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS
import com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES
import com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.parser.Offer
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

private val objectMapper = jacksonObjectMapper().apply {
    findAndRegisterModules()
    configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true)
    enable(ALLOW_UNQUOTED_FIELD_NAMES, ALLOW_COMMENTS, ALLOW_SINGLE_QUOTES)
    disable(FAIL_ON_UNKNOWN_PROPERTIES)
}

@Component
class HmParser : Parser {

    override fun getType(): ParserType = ParserType.HM_HOME

    override fun getItemInfo(url: String): ItemInfo {
        val document = Jsoup.connect(url).get()
        val productInfo = document.toProductInfo()
        val sizeInfos = findAvailableSizes(document, productInfo)
        return ItemInfo(
            url = url,
            price = productInfo.offer.price,
            priceCurrency = productInfo.offer.priceCurrency,
            name = productInfo.name,
            additionalInfo = AdditionalInfo(sizeInfos)
        )
    }

    private fun Document?.toProductInfo(): Product {
        if (this == null) throw RuntimeException("Empty result")
        return getElementById("product-schema")?.data()
            .nonNull()
            .let { objectMapper.readValue(it) }
    }

    private fun Document?.productId(): String {
        if (this == null) throw RuntimeException("Empty result")
        // parse js injection
        return getElementsByTag("script")
            .first { it.data().contains("trackAddToCart") }
            .data()
            .lines()
            .first { it.trim().startsWith("product_id") }
            .split("'")
            .drop(1)
            .first()
    }

    private fun Document?.findSizes(sku: String): List<SizeCodeInfo> {
        if (this == null) throw RuntimeException("Empty result")
        // parse js injection
        val jsonString = getElementsByTag("script")
            .first { it.data().contains("productArticleDetails") }
            .data()
            .lines().dropWhile { it.trim().startsWith("var productArticleDetails").not() }
            .drop(1)
            .filter { !it.contains("isDesktop") }
            .toMutableList()
            .apply { add(0, "{") }
            .joinToString(separator = "\n")
        return (objectMapper.readTree(jsonString)[sku]["sizes"] as ArrayNode).map {
            SizeCodeInfo(
                code = it["sizeCode"].textValue(),
                name = it["name"].textValue()
            )
        }
    }

    private fun findAvailableSizes(document: Document, productInfo: Product): List<SizeInfo> {
        val productId = document.productId()
        val sizes = document.findSizes(productInfo.sku)
        val sizesResponse = Jsoup.connect(productId.toSizesApiUrl())
            .ignoreContentType(true)
            .execute()
            .body() ?: throw RuntimeException("Empty result")
        val availability = objectMapper
            .readValue<AvailableSizesResponse>(sizesResponse)
            .availability
        return sizes.map {
            SizeInfo(
                name = it.name,
                availability = availability.contains(it.code)
            )
        }
    }

    private fun String.toSizesApiUrl(): String {
        return "https://www2.hm.com/hmwebservices/service/product/ru/availability/$this.json"
    }

    private class Product(val name: String, offers: List<Offer>, val sku: String) {
        val offer: Offer = offers.first()
    }

    private data class SizeCodeInfo(
        val code: String,
        val name: String
    )

    private data class AvailableSizesResponse(val availability: Set<String>)
}
