package com.hometech.discount.monitoring.domain.model

import com.hometech.discount.monitoring.domain.exposed.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChange
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChangeLog
import java.math.BigDecimal
import java.time.LocalDateTime

data class ItemChangeWrapper(val item: Item, val itemChange: ChangeWrapper?) {
    fun isItemChanged(): Boolean {
        return itemChange?.priceLog?.priceChange != PriceChange.NONE ||
            itemChange.additionalInfoLog.infoNow != itemChange.additionalInfoLog.infoBefore
    }
}

data class ChangeWrapper(val priceLog: PriceLogView, val additionalInfoLog: AdditionalInfoLogView)

data class PriceLogView(
    val item: Item,
    val priceBefore: BigDecimal,
    val priceNow: BigDecimal
) {
    val priceChange = PriceChange.resolvePriceChange(priceBefore, priceNow)

    fun hasChanges() = priceChange != PriceChange.NONE

    fun createEntity(): PriceChangeLog {
        val view = this
        return PriceChangeLog.new {
            this.item = view.item
            this.priceBefore = view.priceBefore
            this.priceNow = view.priceNow
            this.priceChange = view.priceChange
            this.timeChecked = LocalDateTime.now()
        }
    }
}

class AdditionalInfoLogView(
    val item: Item,
    val infoBefore: AdditionalInfo,
    val infoNow: AdditionalInfo
) {
    fun difference(): String {
        return if (infoBefore.sizes != null || infoNow.sizes != null) {
            val sizesBefore = infoBefore.sizes?.associateBy { it.name } ?: mapOf()
            val sizesNow = infoNow.sizes?.associateBy { it.name } ?: mapOf()
            sizesNow.entries.mapNotNull {
                if (it.value.availability != sizesBefore[it.key]?.availability) {
                    it.value.availabilityMessage()
                } else null
            }.joinToString(separator = "\n")
        } else ""
    }

    private fun SizeInfo.availabilityMessage(): String {
        return if (this.availability)
            "Размер ${this.name} появился в наличии!"
        else "Размера ${this.name} больше нет в наличии!"
    }

    fun hasChanges(): Boolean = infoBefore != infoNow

    fun createEntity(): AdditionalInfoLog {
        val view = this
        return AdditionalInfoLog.new {
            this.item = view.item
            this.infoNow = view.infoNow
            this.infoBefore = view.infoBefore
            this.timeChecked = LocalDateTime.now()
        }
    }
}
