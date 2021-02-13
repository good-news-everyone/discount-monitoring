package com.hometech.discount.monitoring.domain.model

import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.parser.ParserType
import java.math.BigDecimal
import java.time.LocalDateTime

data class ItemInfo(
    val url: String,
    val name: String,
    val price: BigDecimal,
    val priceCurrency: String,
    val additionalInfo: AdditionalInfo
) {
    fun toEntity(): Item {
        val view = this
        return Item.new {
            this.url = view.url
            this.name = view.name
            this.priceCurrency = view.priceCurrency
            this.currentPrice = view.price
            this.lowestPrice = view.price
            this.highestPrice = view.price
            this.initialPrice = view.price
            this.additionalInfo = view.additionalInfo
            this.timeAdded = LocalDateTime.now()
            this.type = ParserType.findByUrl(view.url)
        }
    }
}
