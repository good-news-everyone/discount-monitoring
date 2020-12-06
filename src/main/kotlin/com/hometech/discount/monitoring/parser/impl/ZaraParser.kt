package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.parser.Product
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ZaraParser(private val objectMapper: ObjectMapper) : Parser {
    override fun getType(): ParserType = ParserType.ZARA

    override fun getItemInfo(url: String): ItemInfo {
        val productInfo = Jsoup.connect(url).get().toProductInfo()
        return ItemInfo(
            url = url,
            name = productInfo.name,
            price = productInfo.offer.price,
            priceCurrency = productInfo.offer.priceCurrency
        )
    }

    override fun parsePrice(url: String): BigDecimal = Jsoup.connect(url).get().toProductInfo().offer.price

    private fun Document.toProductInfo(): Product {
        val infoArray = this.getElementById("product")
            ?.getElementsByAttributeValue("type", "application/ld+json")
            ?.firstOrNull()
            ?.data()
        return objectMapper
            .readValue(infoArray, Array<Product>::class.java)
            .firstOrNull() ?: throw RuntimeException("No info.")
    }
}
