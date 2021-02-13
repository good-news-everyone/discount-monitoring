package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object ItemSubscriptionTable : LongIdTable("item_subscribers") {
    val item = reference("item_id", ItemTable)
    val subscriber = reference("user_id", UserTable)
}

class ItemSubscription(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<ItemSubscription>(ItemSubscriptionTable, "Item subscriptions")

    var item by Item referencedOn ItemSubscriptionTable.item
    var subscriber by User referencedOn ItemSubscriptionTable.subscriber
}
