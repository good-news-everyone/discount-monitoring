package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.domain.RemoveOutdatedItemEvent
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.exposed.extensions.lowerCaseText
import com.hometech.discount.monitoring.parser.ParserType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.HttpStatusException
import org.springframework.context.event.EventListener
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@OptIn(DelicateCoroutinesApi::class)
@ObsoleteCoroutinesApi
@Service
class ItemService(
    private val checkDiscountService: CheckDiscountService,
    private val notifyService: NotifyService
) {

    private val log = KotlinLogging.logger { }

    fun createItem(url: String, user: User): Item {
        log.debug { "user request subscription for item $url" }
        return transaction {
            val itemByUrl = Item.find { ItemTable.url.lowerCaseText() eq url.lowercase() }.firstOrNull()
            val item = if (itemByUrl != null) {
                log.debug { "item already exists, creating subscription" }
                itemByUrl
            } else {
                val itemInfo = checkDiscountService.parseItemInfo(url)
                log.debug { "item not exists, creating creating new item" }
                itemInfo.toEntity()
            }
            createSubscription(user, item)
            item
        }
    }

    private fun createSubscription(user: User, item: Item) {
        val existingSubscription = ItemSubscription.find {
            (ItemSubscriptionTable.item eq item.id) and (ItemSubscriptionTable.subscriber eq user.id)
        }.firstOrNull()
        if (existingSubscription == null)
            ItemSubscription.new {
                this.item = item
                this.subscriber = user
            }
    }

    fun removeSubscriptionById(subscriptionId: Long) {
        transaction {
            val found = ItemSubscription.find { ItemSubscriptionTable.id eq subscriptionId }.firstOrNull()
            if (found != null) {
                ItemSubscriptionTable.deleteWhere { ItemSubscriptionTable.id eq subscriptionId }
            } else {
                throw EmptyResultDataAccessException(1)
            }
            val itemId = found.item.id.value
            if (ItemSubscription.findByItem(itemId).empty()) {
                ItemTable.deleteWhere { ItemTable.id eq itemId }
            }
        }
    }

    fun clearSubscriptions(userId: Long) {
        transaction {
            val user = requireNotNull(User.findById(userId))
            ItemSubscription.find { ItemSubscriptionTable.subscriber eq user.id }.forEach {
                val item = it.item
                it.delete()
                if (ItemSubscription.findByItem(item.id.value).empty()) item.delete()
            }
        }
    }

    @EventListener
    fun removeOutdatedItem(event: RemoveOutdatedItemEvent) {
        notifyService.notifyUsersAboutItemDeletion(event.item)
        transaction {
            ItemSubscriptionTable.deleteWhere { ItemSubscriptionTable.item eq event.item.id }
            ItemTable.deleteWhere { ItemTable.id eq event.item.id }
        }
    }

    @ObsoleteCoroutinesApi
    @Scheduled(cron = "0 0 23 * * *")
    fun cleanUpItems() {
        val notAvailableItems = transaction {
            Item.find { ItemTable.type eq ParserType.ZARA }.mapNotNull {
                try {
                    checkDiscountService.parseItemInfo(it.url)
                    null
                } catch (ex: HttpStatusException) {
                    if (ex.statusCode == 410) it
                    else null
                }
            }
        }
        notAvailableItems.forEach {
            log.warn {
                "Item ${it.url} not available anymore. Users will be notified and all subscriptions will be deleted."
            }
            notifyService.notifyUsersAboutItemDeletion(it)
        }
        val itemIds = notAvailableItems.map { it.id }
        transaction {
            ItemSubscriptionTable.deleteWhere { ItemSubscriptionTable.item inList itemIds }
            ItemTable.deleteWhere { ItemTable.id inList notAvailableItems.map { it.id } }
        }
    }
}
