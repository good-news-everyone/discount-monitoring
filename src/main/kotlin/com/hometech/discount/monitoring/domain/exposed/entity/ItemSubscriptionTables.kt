package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.jsonb
import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and

object ItemSubscriptionTable : LongIdTable("item_subscribers") {
    val item = reference("item_id", ItemTable)
    val subscriber = reference("user_id", UserTable)
    val metadata = jsonb(name = "metadata", klass = SubscriptionMetadata::class, nullable = true).nullable()
}

class ItemSubscription(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<ItemSubscription>(ItemSubscriptionTable, "Item subscriptions") {
        fun findByItems(items: Iterable<Long>): SizedIterable<ItemSubscription> {
            return ItemSubscription.find { ItemSubscriptionTable.item inList items }
        }

        fun findByItem(itemId: Long) = findByItems(listOf(itemId))

        fun findByItemAndUser(itemId: Long, userId: Long): ItemSubscription {
            return ItemSubscription.find {
                (ItemSubscriptionTable.item eq itemId).and(ItemSubscriptionTable.subscriber eq userId)
            }.firstOrNull() ?: throw NoSuchElementException("Subscription not found for userId = '$userId' and itemId = '$itemId")
        }
    }

    var item by Item referencedOn ItemSubscriptionTable.item
    var subscriber by User referencedOn ItemSubscriptionTable.subscriber
    var metadata by ItemSubscriptionTable.metadata
}

data class SubscriptionMetadata(val sizes: List<String>? = null)
