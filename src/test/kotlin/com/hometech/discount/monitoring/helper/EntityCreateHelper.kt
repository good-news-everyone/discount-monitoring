package com.hometech.discount.monitoring.helper

import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import java.math.BigDecimal
import kotlin.math.absoluteValue

fun randomItemInfo(
    url: String = ZARA_URL,
    price: BigDecimal = randomBigDecimal(),
    additionalInfo: AdditionalInfo = AdditionalInfo()
): ItemInfo {
    return ItemInfo(
        url = url,
        name = randomString(),
        price = price,
        priceCurrency = randomString(),
        additionalInfo = additionalInfo
    )
}

fun randomUser(): BotUser {
    return BotUser(
        id = randomInt().absoluteValue,
        chatId = randomLong(),
        isBot = false,
        isBlockedBy = true,
        firstName = randomString(),
        lastName = randomString(),
        userName = randomString(),
        contact = randomString()
    )
}

const val ZARA_URL = "https://www.zara.com"
