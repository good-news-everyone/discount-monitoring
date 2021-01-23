package com.hometech.discount.monitoring.domain.model

import com.hometech.discount.monitoring.domain.entity.Item
import java.math.BigDecimal

data class ItemInfo(
    val url: String,
    val name: String,
    val price: BigDecimal,
    val priceCurrency: String,
    val additionalInfo: AdditionalInfo
) {
    fun toEntity(): Item {
        return Item(
            url = url,
            name = name,
            priceCurrency = priceCurrency,
            price = price
        ).also {
            it.additionalInfo = this.additionalInfo
        }
    }
}
