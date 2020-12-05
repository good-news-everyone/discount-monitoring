package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.parser.getTitle
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class HmParser(private val objectMapper: ObjectMapper) : Parser {

    override fun getType(): ParserType = ParserType.HM_HOME

    override fun getItemInfo(url: String): ItemInfo {
        val doc = Jsoup.connect(url).get()
        val offers = doc.getElementById("product-schema")?.data().toOffers()
        return ItemInfo(
            url = url,
            price = offers.readPrice(),
            priceCurrency = offers.readCurrency(),
            name = doc.getTitle()
        )
    }

    override fun parsePrice(url: String): BigDecimal {
        val response = Jsoup.connect(url).get()
        return response.getElementById("product-schema")?.data().toOffers().readPrice()
    }

    private fun String?.toOffers(): List<Map<String, Any?>> {
        return objectMapper.readValue(this, Map::class.java)["offers"] as List<Map<String, Any?>>
    }

    private fun List<Map<String, Any?>>.readPrice(): BigDecimal {
        val price = this.firstOrNull()?.get("price") as String?
        if (price.isNullOrEmpty()) throw RuntimeException("Price not found")
        return BigDecimal(price)
    }

    private fun List<Map<String, Any?>>.readCurrency(): String {
        val currency = this.firstOrNull()?.get("priceCurrency") as String?
        if (currency.isNullOrEmpty()) throw RuntimeException("Price currency not found")
        return currency
    }
}
