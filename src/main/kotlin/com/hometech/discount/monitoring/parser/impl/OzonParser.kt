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
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class OzonParser(private val objectMapper: ObjectMapper) : Parser {

    val cookies: MutableMap<String, String> = ConcurrentHashMap()

    override fun getType(): ParserType = ParserType.OZON

    override fun getItemInfo(url: String): ItemInfo {
        checkCookies()
        val response = Jsoup.connect(url)
            .cookies(cookies)
            .execute()
        cookies.putAll(response.cookies())
        val productInfo = response.parse().info()
        return ItemInfo(
            url = url,
            name = productInfo.name,
            price = productInfo.offer.price,
            priceCurrency = productInfo.offer.priceCurrency
        )
    }

    override fun parsePrice(url: String): BigDecimal = Jsoup.connect(url).get().info().offer.price

    private fun Document.info(): Product {
        val infoArray = this.getElementsByAttributeValue("type", "application/ld+json")
            ?.firstOrNull()
            ?.data()
        return objectMapper
            .readValue(infoArray, Product::class.java)
    }

    private fun checkCookies() {
        if (cookies.isEmpty()) return
        val tokenExpiration = Instant.parse(cookies["token-expiration"])
        if (tokenExpiration.isBefore(Instant.now())) cookies.clear()
    }
}
