package com.hometech.discount.monitoring.domain.entity

import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "price_changelog")
class PriceLog(
    val itemId: Long,
    val priceBefore: BigDecimal,
    val priceNow: BigDecimal
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    val priceChange = PriceChange.resolvePriceChange(priceBefore, priceNow)
    val timeChecked = LocalDateTime.now()
}

enum class PriceChange(val literal: String) {
    UP("повысилась"),
    DOWN("снизилась"),
    NONE("не изменилась");

    companion object {
        fun resolvePriceChange(old: BigDecimal, new: BigDecimal): PriceChange {
            if (old == new) return NONE
            if (old < new) return UP
            return DOWN
        }
    }
}
