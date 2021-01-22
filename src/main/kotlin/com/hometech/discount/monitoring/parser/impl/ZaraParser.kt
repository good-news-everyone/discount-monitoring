package com.hometech.discount.monitoring.parser.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.parser.Product
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class ZaraParser(private val objectMapper: ObjectMapper) : Parser {
    override fun getType(): ParserType = ParserType.ZARA

    override fun getItemInfo(url: String): ItemInfo {
        val document = Jsoup.connect(url).get()
        val productInfo = document.toProductInfo()
        val sizeInfos = document.sizeInfos()
        return ItemInfo(
            url = url,
            name = productInfo.name,
            price = productInfo.offer.price,
            priceCurrency = productInfo.offer.priceCurrency,
            additionalInfo = AdditionalInfo(sizeInfos)
        )
    }

    private fun Document?.toProductInfo(): Product {
        if (this == null) throw RuntimeException("Empty result")
        val infos = this.getElementsByAttributeValue("type", "application/ld+json")
            .firstOrNull()
            ?.data() ?: throw RuntimeException("No info present.")
        return objectMapper
            .readValue(infos, Array<Product>::class.java)
            .first()
    }

    private fun Document?.sizeInfos(): List<SizeInfo> {
        if (this == null) throw RuntimeException("Empty result")
        val content = this.getElementsByAttributeValue("type", "text/javascript").first {
            it.data().trim().startsWith(SIZES_INFO_MARKER)
        }.data()
        val matcher = Pattern.compile(SIZES_REGEX).matcher(content)
        matcher.find()
        val sizesString = matcher.group().replace(SIZES_REPLACEMENT, "")
        return (objectMapper.readValue(sizesString, List::class.java) as List<Map<String, String>>)
            .map {
                SizeInfo(
                    name = requireNotNull(it["name"]),
                    availability = it["availability"] == "in_stock"
                )
            }
    }
}

private const val SIZES_REGEX = "(\"sizes\":\\[[^\\[\\]]*\\])"
private const val SIZES_INFO_MARKER = "window.zara.appConfig"
private const val SIZES_REPLACEMENT = "\"sizes\":"
