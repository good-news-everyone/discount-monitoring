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
import java.util.concurrent.ConcurrentHashMap

@Component
class OzonParser(private val objectMapper: ObjectMapper) : Parser {

    val cookies: MutableMap<String, String> = ConcurrentHashMap(initialCookies)

    override fun getType(): ParserType = ParserType.OZON

    override fun getItemInfo(url: String): ItemInfo {
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

    override fun parsePrice(url: String): BigDecimal {
        val response = Jsoup.connect(url)
            .cookies(cookies)
            .execute()
        cookies.putAll(response.cookies())
        return response.parse().info().offer.price
    }

    private fun Document.info(): Product {
        val infoArray = this.getElementsByAttributeValue("type", "application/ld+json")
            ?.firstOrNull()
            ?.data()
        return objectMapper
            .readValue(infoArray, Product::class.java)
    }

    companion object {
        private val initialCookies = mapOf(
            Pair("__Secure-ab-group", "28"),
            Pair("__Secure-session-id", "mbFm/xWwSq2PD6WTl9xeMg"),
            Pair("__Secure-ext_xcid", "e10248f31d1c03770e86759a88685ed6"),
            Pair("__Secure-user-id", "50085194"),
            Pair("__Secure-access-token", "3.0.mbFm/xWwSq2PD6WTl9xeMg.28.l8cMBQAAAABfzdWBAC1MKKN3ZWKgAICQoA..20201207091057.vMwvL1Z0ezIOrtOlMv5IqSywaGmg6OmhJUrpqyhMyDA"),
            Pair("__Secure-refresh-token", "3.0.mbFm/xWwSq2PD6WTl9xeMg.28.l8cMBQAAAABfzdWBAC1MKKN3ZWKgAICQoA..20201207091057.NRlV_IVsvPugrgy7DZ1xQRemucHgrLFdvhUDfNJ81p0"),
            Pair("__Secure-token-expiration", "2020-12-07T11:23:03.9426131+03:00")
        )
    }
}
