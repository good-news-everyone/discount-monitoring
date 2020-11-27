package com.hometech.discount.monitoring.parser

import com.hometech.discount.monitoring.domain.entity.Item
import java.math.BigDecimal

interface Parser {
    fun getType(): ParserType
    fun getItemInfo(url: String): ItemInfo
    fun parsePrice(url: String): BigDecimal
}

data class ItemInfo(
    val url: String,
    val name: String,
    val price: BigDecimal
) {
    fun toEntity(): Item {
        return Item(url, name, price)
    }
}
