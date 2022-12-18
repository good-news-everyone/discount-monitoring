package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.impl.HmParser
import com.hometech.discount.monitoring.parser.impl.MangoParser
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
    lateinit var mangoParser: MangoParser

    @Test
    fun checkHmAvailability() {
        val item = hmParser.getItemInfo("https://www2.hm.com/tr_tr/productpage.1125047002.html")
        assertionsOf(item)
        item.additionalInfo.sizes?.isNotEmpty()
    }

    @Test
    fun checkZaraAvailability() {
        val item = zaraParser.getItemInfo(
            "https://www.zara.com/tr/en/water-and-wind-protection-puffer-jacket-with-a-hood-p03046022.html?v1=236048523&v2=2113372"
        )
        assertionsOf(item)
        item.additionalInfo.sizes?.isNotEmpty()
    }

    @Test
    fun checkMangoAvailability() {
        val item = mangoParser.getItemInfo("https://shop.mango.com/ru/женская/купальники-бикини/бюстгальтер-от-бикини-с-цветочным-принтом_87066316.html?c=99&utm_source=product-share&utm_medium=app-IOS")
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
