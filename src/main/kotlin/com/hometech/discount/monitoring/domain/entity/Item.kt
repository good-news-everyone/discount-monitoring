package com.hometech.discount.monitoring.domain.entity

import com.hometech.discount.monitoring.parser.ParserType
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "items")
class Item(
    val url: String,
    val name: String,
    val priceCurrency: String,
    price: BigDecimal
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    @Enumerated(EnumType.STRING)
    val type: ParserType = ParserType.findByUrl(url)
    var initialPrice: BigDecimal = price
    var lowestPrice: BigDecimal = price
    var currentPrice: BigDecimal = price
    var highestPrice: BigDecimal = price
    var timeAdded: LocalDateTime = LocalDateTime.now()

    fun setNewPrice(newPrice: BigDecimal) {
        this.currentPrice = newPrice
        if (currentPrice < lowestPrice)
            this.lowestPrice = newPrice
        if (currentPrice > highestPrice)
            this.highestPrice = newPrice
    }
}
