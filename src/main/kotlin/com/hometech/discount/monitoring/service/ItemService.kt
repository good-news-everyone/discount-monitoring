package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.ItemSubscriber
import com.hometech.discount.monitoring.domain.repository.ItemRepository
import com.hometech.discount.monitoring.domain.repository.ItemSubscribersRepository
import com.hometech.discount.monitoring.domain.repository.UserRepository
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class ItemService(
    private val checkDiscountService: CheckDiscountService,
    private val itemRepository: ItemRepository,
    private val userRepository: UserRepository,
    private val itemSubscriberRepository: ItemSubscribersRepository
) {

    fun createItem(url: String, user: BotUser): Item {
        userRepository.save(user)
        val item = if (itemRepository.existsByUrl(url)) {
            itemRepository.findOneByUrl(url)
        } else {
            val itemInfo = checkDiscountService.parseItemInfo(url)
            itemRepository.save(itemInfo.toEntity())
        }
        createSubscription(user, item)
        return item
    }

    fun createSubscription(user: BotUser, item: Item) {
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
}
