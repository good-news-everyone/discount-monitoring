package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.ItemSubscriber
import com.hometech.discount.monitoring.domain.repository.ItemRepository
import com.hometech.discount.monitoring.domain.repository.ItemSubscribersRepository
import com.hometech.discount.monitoring.domain.repository.UserRepository
import com.hometech.discount.monitoring.parser.ParserType
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jsoup.HttpStatusException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ObsoleteCoroutinesApi
@Service
class ItemService(
    private val checkDiscountService: CheckDiscountService,
    private val itemRepository: ItemRepository,
    private val userRepository: UserRepository,
    private val itemSubscriberRepository: ItemSubscribersRepository,
    private val notifyService: NotifyService
) {

    private val log = KotlinLogging.logger { }

    fun createItem(url: String, user: BotUser): Item {
        userRepository.save(user)
        log.debug { "user request subscription for item $url" }
        val item = if (itemRepository.existsByUrl(url)) {
            log.debug { "item already exists, creating subscription" }
            itemRepository.findOneByUrl(url)
        } else {
            val itemInfo = checkDiscountService.parseItemInfo(url)
            log.debug { "item not exists, creating creating new item" }
            itemRepository.save(itemInfo.toEntity())
        }
        createSubscription(user, item)
        return item
    }

    private fun createSubscription(user: BotUser, item: Item) {
        val subscriptionExists = itemSubscriberRepository.existsByItemIdAndUserId(
            requireNotNull(item.id),
            user.id.toLong()
        )
        if (subscriptionExists.not()) itemSubscriberRepository.save(
            ItemSubscriber(
                itemId = requireNotNull(item.id),
                userId = user.id.toLong()
            )
        )
    }

    fun findItemsByUser(userId: Int): List<Item> = itemRepository.findItemsByUserId(userId.toLong())

    @Transactional
    fun removeSubscriptionByUrl(url: String, userId: Int) {
        if (itemSubscriberRepository.countSubscriptionsByUrlAndUserId(url, userId.toLong()) != 0) {
            itemSubscriberRepository.removeSubscriptionByUrlAndUserId(url, userId.toLong())
        } else {
            throw EmptyResultDataAccessException(1)
        }
        itemRepository.deleteItemsWithoutSubscriptions()
    }

    @Transactional
    fun clearSubscriptions(userId: Int) {
        itemSubscriberRepository.clearUserSubscriptions(userId.toLong())
        itemRepository.deleteItemsWithoutSubscriptions()
    }

    @ObsoleteCoroutinesApi
    @Transactional
    @Scheduled(cron = "0 0 23 * * *")
    fun cleanUpItems() {
        val notAvailableItems = itemRepository.findAllByType(ParserType.ZARA).mapNotNull {
            try {
                checkDiscountService.parseItemInfo(it.url)
                null
            } catch (ex: HttpStatusException) {
                if (ex.statusCode == 410) it
                else null
            }
        }
        notAvailableItems.forEach {
            log.warn {
                "Item ${it.url} not available anymore. Users will be notified and all subscriptions will be deleted."
            }
            notifyService.notifyUsersAboutItemDeletion(it)
        }
        val itemIds = notAvailableItems.mapNotNull { it.id }
        itemSubscriberRepository.deleteAllByItemIdIn(itemIds)
        itemRepository.deleteAll(notAvailableItems)
    }
}
