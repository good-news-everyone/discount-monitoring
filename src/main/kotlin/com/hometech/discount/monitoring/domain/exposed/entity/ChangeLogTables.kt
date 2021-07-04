package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.entity.exposed.enum
import com.hometech.discount.monitoring.domain.exposed.extensions.jsonb
import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.math.BigDecimal

object AdditionalInfoLogTable : LongIdTable("additional_info_changelog") {
    val item = reference("item_id", ItemTable)
    val timeChecked = datetime("time_checked")
    val infoBefore = jsonb(name = "info_before", klass = AdditionalInfo::class, nullable = true)
    val infoNow = jsonb(name = "info_now", klass = AdditionalInfo::class, nullable = true)
}

class AdditionalInfoLog(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<AdditionalInfoLog>(AdditionalInfoLogTable, "Additional info changelog")

    var item by Item referencedOn AdditionalInfoLogTable.item
    var timeChecked by AdditionalInfoLogTable.timeChecked
    var infoBefore by AdditionalInfoLogTable.infoBefore
    var infoNow by AdditionalInfoLogTable.infoNow

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
}

object PriceChangeLogTable : LongIdTable("price_changelog") {
    val item = reference("item_id", ItemTable)
    val timeChecked = datetime("time_checked")
    val priceChange = enum<PriceChange>("price_change")
    val priceBefore = decimal("price_before", 10, 2)
    val priceNow = decimal("price_now", 10, 2)
}

class PriceChangeLog(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<PriceChangeLog>(PriceChangeLogTable, "Price changelog")

    var item by Item referencedOn PriceChangeLogTable.item
    var timeChecked by PriceChangeLogTable.timeChecked
    var priceChange by PriceChangeLogTable.priceChange
    var priceBefore by PriceChangeLogTable.priceBefore
    var priceNow by PriceChangeLogTable.priceNow

    fun hasChanges() = priceChange != PriceChange.NONE
}

enum class PriceChange(val literal: String) {
    UP("повысилась"),
    DOWN("снизилась"),
    NONE("не изменилась");

    companion object {
        fun resolvePriceChange(old: BigDecimal, new: BigDecimal): PriceChange {
            return when (old.compareTo(new)) {
                -1 -> UP
                1 -> DOWN
                else -> NONE
            }
        }
    }
}
