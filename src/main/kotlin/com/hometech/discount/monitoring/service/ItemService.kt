package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.parser.ParserType
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.HttpStatusException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
            val itemByUrl = Item.find { ItemTable.url.lowerCase() eq url.toLowerCase() }.firstOrNull()
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

    fun removeSubscriptionByUrl(url: String, userId: Int) {
        transaction {
            val itemByUrl = Item.find { ItemTable.url.lowerCase() eq url.toLowerCase() }.first()
            val op = (ItemSubscriptionTable.subscriber inList listOf(userId.toLong()))
                .and(ItemSubscriptionTable.item inList listOf(itemByUrl.id))
            val subscription = ItemSubscription.find { op }.firstOrNull()
            if (subscription != null) {
                subscription.delete()
            } else {
                throw EmptyResultDataAccessException(1)
            }
            if (itemByUrl.subscribers.empty()) itemByUrl.delete()
        }
    }

    fun clearSubscriptions(userId: Long) {
        transaction {
            val user = requireNotNull(User.findById(userId))
            ItemSubscription.find { ItemSubscriptionTable.subscriber eq user.id }.forEach {
                val item = it.item
                it.delete()
                if (item.subscribers.empty()) item.delete()
            }
        }
    }

    @ObsoleteCoroutinesApi
    @Transactional
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
        val itemIds = notAvailableItems.mapNotNull { it.id }
        transaction {
            ItemSubscriptionTable.deleteWhere { ItemSubscriptionTable.item inList itemIds }
            ItemTable.deleteWhere { ItemTable.id inList notAvailableItems.map { it.id } }
        }
    }
}
