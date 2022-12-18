package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import com.hometech.discount.monitoring.domain.exposed.extensions.enum
import com.hometech.discount.monitoring.domain.exposed.extensions.jsonb
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
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
