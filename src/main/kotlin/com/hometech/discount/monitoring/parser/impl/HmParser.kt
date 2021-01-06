package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.parser.Offer
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class HmParser(private val objectMapper: ObjectMapper) : Parser {

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
        val data = this.getElementById("product-schema")?.data()
        requireNotNull(data)
        return objectMapper.readValue(data, Product::class.java)
    }

    private fun Document?.productId(): String {
        if (this == null) throw RuntimeException("Empty result")
        val data = this.getElementsByTag("script").first {
            it.data().contains("trackAddToCart")
        }.data()
        return data.split("\n").first {
            it.trim().startsWith("product_id")
        }.split("'").drop(1).first()
    }

    private fun Document?.findSizes(sku: String): List<SizeCodeInfo> {
        if (this == null) throw RuntimeException("Empty result")
        val data = this.getElementsByTag("script").first {
            it.data().contains("productArticleDetails")
        }.data()
        val strings = data.split("\n").dropWhile { it.trim().startsWith("var productArticleDetails").not() }
            .drop(1)
            .toMutableList()
        strings.add(0, "{")
        val jsonString = strings.joinToString(separator = "\n") {
            it.trim()
                .replace("'", "\"")
                .replace(Regex("isDesktop.* \".*\""), "\"\"")
        }
            .replace("}\n,\n{", "},\n{")
        val skuInfo = objectMapper.readValue(jsonString, HashMap::class.java)[sku] as Map<String, Any>
        return (skuInfo["sizes"] as List<Map<String, Any>>).map {
            SizeCodeInfo(
                code = it["sizeCode"] as String,
                name = it["name"] as String
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
        val availability = objectMapper.readValue(sizesResponse, AvailableSizesResponse::class.java).availability
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
