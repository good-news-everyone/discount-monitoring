package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.ProxyDictionary
import com.hometech.discount.monitoring.parser.impl.HmParser
import com.hometech.discount.monitoring.parser.impl.MangoParser
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

    @Autowired
    lateinit var mangoParser: MangoParser

    @Autowired
    lateinit var proxyParser: ProxyDictionary

    @Test
    fun checkHmAvailability() {
        val item = hmParser.getItemInfo("https://www2.hm.com/ru_ru/productpage.0791428001.html")
        assertionsOf(item)
        item.additionalInfo.sizes?.isNotEmpty()
    }

    @Test
    fun checkZaraAvailability() {
        val item = zaraParser.getItemInfo(
            "https://www.zara.com/ru/ru/%D1%83%D0%BA%D0%BE%D1%80%D0%BE%D1%87%D0%B5%D0%BD%D0%BD%D0%B0%D1%8F-%D0%B4%D0%B6%D0%B8%D0%BD%D1%81%D0%BE%D0%B2%D0%B0%D1%8F-%D0%BA%D1%83%D1%80%D1%82%D0%BA%D0%B0-p04877045.html?v1=108957968&v2=1718095"
        )
        assertionsOf(item)
        item.additionalInfo.sizes?.isNotEmpty()
    }

    @Test
    fun checkOzonAvailability() {
        val item = ozonParser.getItemInfo("https://www.ozon.ru/context/detail/id/149090110/?tab=reviews")
        assertionsOf(item)
    }

    @Test
    fun checkMangoAvailability() {
        val item = mangoParser.getItemInfo("https://shop.mango.com/ru/женская/купальники-бикини/бюстгальтер-от-бикини-с-цветочным-принтом_87066316.html?c=99&utm_source=product-share&utm_medium=app-IOS")
        assertionsOf(item)
    }

//    @Test
    fun checkProxyParser() {
        proxyParser.checkProxyList()
        proxyParser.random()
    }

    fun assertionsOf(item: ItemInfo) {
        item.shouldNotBeNull()
        item.price.shouldNotBeNull()
        item.priceCurrency.shouldNotBeNull()
        item.url.shouldNotBeNull()
        item.name.shouldNotBeNull()
    }
}
