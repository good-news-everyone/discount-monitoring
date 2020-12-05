package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.parser.impl.HmParser
import com.hometech.discount.monitoring.parser.impl.ZaraParser
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.Assert

@SpringBootTest(classes = [MonitoringApplication::class])
class MonitoringApplicationTests {

    @Autowired
    lateinit var hmParser: HmParser

    @Autowired
    lateinit var zaraParser: ZaraParser

    @Test
    fun contextLoads() {
    }

    @Test
    fun checkHmAvailability() {
        val item = hmParser.getItemInfo("https://www2.hm.com/ru_ru/productpage.0791428001.html")
        Assert.notNull(item, "item is null")
        Assert.notNull(item.price, "item.price is null")
        Assert.notNull(item.priceCurrency, "item.priceCurrency is null")
        Assert.notNull(item.url, "item.url is null")
        Assert.notNull(item.name, "item.name is null")
    }

    @Test
    fun checkZaraAvailability() {
        zaraParser.getItemInfo(
            "https://www.zara.com/ru/ru/%D0%BD%D0%B5%D0%BF%D1%80%D0%BE%D0%BC%D0%BE%D0%BA%D0%B0%D0%B5%D0%BC%D0%B0%D1%8F-%D0%BF%D0%B0%D1%80%D0%BA%D0%B0-%D0%BE%D0%B2%D0%B5%D1%80%D1%81%D0%B0%D0%B9%D0%B7-%C2%AB2-%D0%B2-1%C2%BB-p04432714.html?v1=82077675&v2=1549280"
        )
    }
}
