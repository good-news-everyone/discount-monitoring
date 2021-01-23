package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.impl.HmParser
import com.hometech.discount.monitoring.parser.impl.OzonParser
import com.hometech.discount.monitoring.parser.impl.ZaraParser
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IntegrationTests : BaseIntegrationTest() {

    @Autowired
    lateinit var hmParser: HmParser
    @Autowired
    lateinit var zaraParser: ZaraParser
    @Autowired
    lateinit var ozonParser: OzonParser

    @Test
    fun checkHmAvailability() {
        val item = hmParser.getItemInfo("https://www2.hm.com/ru_ru/productpage.0791428001.html")
        assertionsOf(item)
        item.additionalInfo.sizes?.isNotEmpty()
    }

    @Test
    fun checkZaraAvailability() {
        val item = zaraParser.getItemInfo(
            "https://www.zara.com/ru/ru/%D0%BD%D0%B5%D0%BF%D1%80%D0%BE%D0%BC%D0%BE%D0%BA%D0%B0%D0%B5%D0%BC%D0%B0%D1%8F-%D0%BF%D0%B0%D1%80%D0%BA%D0%B0-%D0%BE%D0%B2%D0%B5%D1%80%D1%81%D0%B0%D0%B9%D0%B7-%C2%AB2-%D0%B2-1%C2%BB-p04432714.html?v1=82077675&v2=1549280"
        )
        assertionsOf(item)
        item.additionalInfo.sizes?.isNotEmpty()
    }

    @Test
    fun checkOzonAvailability() {
        val item = ozonParser.getItemInfo("https://www.ozon.ru/context/detail/id/149090110/?tab=reviews")
        assertionsOf(item)
    }

    fun assertionsOf(item: ItemInfo) {
        item.shouldNotBeNull()
        item.price.shouldNotBeNull()
        item.priceCurrency.shouldNotBeNull()
        item.url.shouldNotBeNull()
        item.name.shouldNotBeNull()
    }
}
