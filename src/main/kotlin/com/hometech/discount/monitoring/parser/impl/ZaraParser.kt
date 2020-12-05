package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ZaraParser(private val objectMapper: ObjectMapper) : Parser {
    override fun getType(): ParserType = ParserType.ZARA

    override fun getItemInfo(url: String): ItemInfo {
        val doc = Jsoup.connect(url).get()
        val offer = doc.info()
        return ItemInfo(
            url = url,
            name = offer["name"] as String,
            price = offer.price(),
            priceCurrency = offer.priceCurrency()
        )
    }

    override fun parsePrice(url: String): BigDecimal = Jsoup.connect(url).get().info().price()

    private fun Document.info(): Map<String, Any?> {
        val infoArray = this.getElementById("product")
            ?.getElementsByAttributeValue("type", "application/ld+json")
            ?.firstOrNull()
            ?.data()
        return objectMapper
            .readValue(infoArray, Array::class.java)
            .firstOrNull() as Map<String, Any?>? ?: throw RuntimeException("No info.")
    }

    private fun Map<String, Any?>.price(): BigDecimal {
        val priceString = (this["offers"] as Map<String, Any>)["price"] as String
        return BigDecimal(priceString)
    }

    private fun Map<String, Any?>.priceCurrency(): String = (this["offers"] as Map<String, Any>)["priceCurrency"] as String
}
