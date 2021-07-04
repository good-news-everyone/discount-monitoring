package com.hometech.discount.monitoring.domain.model

import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.domain.exposed.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChange
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChangeLog
import com.hometech.discount.monitoring.domain.exposed.entity.SubscriptionMetadata
import java.math.BigDecimal
import java.time.LocalDateTime

data class ItemChangeWrapper(val item: Item, val itemChange: ChangeWrapper?) {
    fun isItemChanged(): Boolean {
        return isPriceChanged() || isAdditionalInfoChanged()
    }

    fun isPriceChanged() = itemChange?.priceLog?.hasChanges() ?: false
    fun isAdditionalInfoChanged() = itemChange?.additionalInfoLog?.infoNow != itemChange?.additionalInfoLog?.infoBefore

    fun changesFromSubscription(subscriptionMetadata: SubscriptionMetadata?): List<SizeInfo> {
        val difference = difference().associateBy { it.name }
        return if (subscriptionMetadata?.sizes != null && subscriptionMetadata.sizes.isNotEmpty()) {
            subscriptionMetadata.sizes.mapNotNull { difference[it] }
        } else difference.values.toList()
    }

    private fun difference(): List<SizeInfo> {
        return if (this.isItemChanged() && isAdditionalInfoChanged()) {
            val infoBefore = this.itemChange?.additionalInfoLog?.infoBefore ?: AdditionalInfo(listOf())
            val infoNow = this.itemChange?.additionalInfoLog?.infoNow ?: AdditionalInfo(listOf())
            infoNow.sizes.nonNull().mapNotNull {
                if (infoBefore.sizes.nonNull().first { size -> it.name == size.name } != it) it else null
            }
        } else listOf()
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

fun List<SizeInfo>.toAvailabilityMessage() = this.joinToString(separator = "\n") { it.availabilityMessage() }

private fun SizeInfo.availabilityMessage(): String {
    return if (this.availability)
        "Размер ${this.name} появился в наличии!"
    else "Размера ${this.name} больше нет в наличии!"
}
