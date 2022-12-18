package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import com.hometech.discount.monitoring.domain.exposed.extensions.enum
import com.hometech.discount.monitoring.domain.exposed.extensions.jsonb
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.parser.ParserType
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.math.BigDecimal

object ItemTable : LongIdTable("items") {
    val name = text("name")
    val url = text("url")
    val type = enum<ParserType>("type")
    val initialPrice = decimal("initial_price", precision = 10, scale = 2)
    val lowestPrice = decimal("lowest_price", precision = 10, scale = 2)
    val highestPrice = decimal("highest_price", precision = 10, scale = 2)
    val currentPrice = decimal("current_price", precision = 10, scale = 2)
    val priceCurrency = text("price_currency")
    val timeAdded = datetime("time_added")
    val additionalInfo = jsonb(name = "additional_info", klass = AdditionalInfo::class, nullable = true)
}

class Item(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<Item>(ItemTable, "Item info")

    var name by ItemTable.name
    var url by ItemTable.url
    var type by ItemTable.type
    var initialPrice by ItemTable.initialPrice
    var lowestPrice by ItemTable.lowestPrice
    var highestPrice by ItemTable.highestPrice
    var currentPrice by ItemTable.currentPrice
    var priceCurrency by ItemTable.priceCurrency
    var timeAdded by ItemTable.timeAdded
    var additionalInfo by ItemTable.additionalInfo

    fun setNewPrice(newPrice: BigDecimal) {
        this.currentPrice = newPrice
        if (currentPrice < lowestPrice)
            this.lowestPrice = newPrice
        if (currentPrice > highestPrice)
            this.highestPrice = newPrice
    }

    override fun toString(): String {
        return "Item(id = $id, type = $type, url = $url)"
    }
}
