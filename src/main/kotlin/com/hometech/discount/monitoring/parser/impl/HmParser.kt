package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.Offer
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class HmParser(private val objectMapper: ObjectMapper) : Parser {

    override fun getType(): ParserType = ParserType.HM_HOME

    override fun getItemInfo(url: String): ItemInfo {
        val productInfo = Jsoup.connect(url)
            .get()
            .toProductInfo()
        return ItemInfo(
            url = url,
            price = productInfo.offer.price,
            priceCurrency = productInfo.offer.priceCurrency,
            name = productInfo.name
        )
    }

    override fun parsePrice(url: String): BigDecimal {
        return Jsoup.connect(url)
            .get()
            .toProductInfo()
            .offer
            .price
    }

    private fun Document?.toProductInfo(): Product {
        if (this == null) throw RuntimeException("Empty result")
        val data = this.getElementById("product-schema")?.data()
        requireNotNull(data)
        return objectMapper.readValue(data, Product::class.java)
    }

    private class Product(val name: String, offers: List<Offer>) {
        val offer: Offer = offers.first()
    }
}
