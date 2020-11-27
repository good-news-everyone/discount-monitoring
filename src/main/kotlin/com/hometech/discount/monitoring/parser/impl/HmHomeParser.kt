package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.parser.ItemInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.parser.getTitle
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class HmHomeParser(private val objectMapper: ObjectMapper) : Parser {

    override fun getType(): ParserType = ParserType.HM_HOME

    override fun getItemInfo(url: String): ItemInfo {
        val doc = Jsoup.connect(url).get()
        val priceJson = doc.getElementById("product-schema")?.data()
        val price = readPrice(priceJson)
        return ItemInfo(
            url = url,
            price = price,
            name = doc.getTitle()
        )
    }

    override fun parsePrice(url: String): BigDecimal {
        val response = Jsoup.connect(url).get()
        val priceJson = response.getElementById("product-schema")?.data()
        return readPrice(priceJson)
    }

    private fun readPrice(data: String?): BigDecimal {
        val offers = objectMapper.readValue(data, Map::class.java)["offers"] as List<Map<String, Object>>?
        val price = offers?.firstOrNull()?.get("price") as String?
        if (price.isNullOrEmpty()) throw RuntimeException("Price not found")
        return BigDecimal(price)
    }
}
