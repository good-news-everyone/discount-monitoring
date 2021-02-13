package com.hometech.discount.monitoring.helper

import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.parser.ParserType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.math.absoluteValue

fun randomItemInfo(
    url: String = ZARA_URL,
    price: BigDecimal = randomBigDecimal(),
    additionalInfo: AdditionalInfo = AdditionalInfo()
): ItemInfo = ItemInfo(
    url = url,
    name = randomString(),
    price = price,
    priceCurrency = randomString(),
    additionalInfo = additionalInfo
)

fun randomItem(
    url: String = ZARA_URL,
    price: BigDecimal = randomBigDecimal(),
    additionalInfo: AdditionalInfo = AdditionalInfo()
): Item {
    return Item.new {
        this.url = url
        this.name = randomString()
        this.type = ParserType.findByUrl(ZARA_URL)
        this.currentPrice = price
        this.highestPrice = price
        this.lowestPrice = price
        this.initialPrice = price
        this.priceCurrency = randomString()
        this.additionalInfo = additionalInfo
        this.timeAdded = LocalDateTime.now()
    }
}

fun randomUser(): User {
    return User.new(randomInt().absoluteValue.toLong()) {
        this.chatId = randomLong()
        this.isBot = false
        this.isBlockedBy = true
        this.firstName = randomString()
        this.lastName = randomString()
        this.userName = randomString()
        this.contact = randomString()
    }
}

fun createRelations(user: User, item: Item) {
    ItemSubscription.new {
        this.item = item
        this.subscriber = user
    }
}

const val ZARA_URL = "https://www.zara.com"
