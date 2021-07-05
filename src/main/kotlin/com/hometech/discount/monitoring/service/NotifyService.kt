package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.Message
import com.hometech.discount.monitoring.domain.exposed.entity.MessageDirection
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChange
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.MessageBody
import com.hometech.discount.monitoring.domain.model.PriceLogView
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.domain.model.toAvailabilityMessage
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDateTime

@ObsoleteCoroutinesApi
@Component
class NotifyService(
    private val restTemplate: RestTemplate,
    applicationProperties: ApplicationProperties
) {
    @Autowired
    private lateinit var itemService: ItemService

    private val log = KotlinLogging.logger { }
    private val uri = "https://api.telegram.org/bot${applicationProperties.bot.token}/sendMessage"

    @Async
    fun notifyUsers(notifyingItems: List<ItemChangeWrapper>) {
        val items = notifyingItems
            .filter { it.itemChange != null && it.isItemChanged() }
            .associateBy { it.item.id.value }
        val itemToSubscription = transaction {
            ItemSubscription.findByItems(items.keys)
                .with(ItemSubscription::subscriber, ItemSubscription::item)
                .groupBy({ it.item.id.value }, { it })
        }
        items.values.forEach { wrapper ->
            itemToSubscription[wrapper.item.id.value]
                .nonNull()
                .forEach { sendNotification(wrapper, it) }
        }
    }

    private fun sendNotification(wrapper: ItemChangeWrapper, subscription: ItemSubscription) {
        try {
            val difference = wrapper.changesFromSubscription(subscription.metadata)
            if (difference.isNotEmpty() || wrapper.isPriceChanged()) {
                val messageBody = buildMessage(wrapper, difference)
                transaction {
                    val user = subscription.subscriber
                    sendMessage(user, messageBody)
                    saveMessage(user, messageBody)
                }
            }
        } catch (ex: HttpClientErrorException) {
            log.error { ex }
            if (ex.statusCode == HttpStatus.FORBIDDEN) {
                transaction { setBlockedBy(subscription.subscriber) }
            }
        }
    }

    fun notifyUsersAboutItemDeletion(item: Item) {
        transaction {
            ItemSubscription.findByItems(listOf(item.id.value)).forEach {
                val message = """
                $ITEM_NOT_AVAILABLE_MESSAGE
                ${item.url}
                """.trimIndent()
                sendMessage(it.subscriber, message)
                saveMessage(it.subscriber, message)
            }
        }
    }

    fun sendMessageToAllUsers(message: String) {
        transaction {
            User.findAll().forEach {
                try {
                    sendMessage(it, message)
                } catch (ex: HttpClientErrorException) {
                    if (ex.statusCode == HttpStatus.FORBIDDEN) setBlockedBy(it)
                }
            }
        }
    }

    fun sendMessageToUser(userId: Int, message: String) {
        transaction {
            val user = User.findById(userId.toLong()) ?: throw NoSuchElementException("User with id = $userId not found")
            try {
                sendMessage(user, message)
            } catch (ex: HttpClientErrorException) {
                if (ex.statusCode == HttpStatus.FORBIDDEN) setBlockedBy(user)
            }
        }
    }

    fun setBlockedBy(user: User) {
        transaction { user.isBlockedBy = true }
        itemService.clearSubscriptions(userId = user.id.value)
        log.warn { "Blocked by $user! All subscriptions will be removed" }
    }

    private fun sendMessage(user: User, message: String) {
        val request = HttpEntity<MultiValueMap<String, String>>(
            MessageBody(user.chatId, message).toMultivaluedMap(),
            HttpHeaders().apply {
                this.contentType = MediaType.APPLICATION_FORM_URLENCODED
            }
        )
        restTemplate.exchange(
            uri,
            HttpMethod.POST,
            request,
            String::class.java
        )
    }

    private fun saveMessage(user: User, message: String) {
        Message.new {
            this.user = user
            this.message = message
            this.direction = MessageDirection.OUTBOUND
            this.timestamp = LocalDateTime.now()
        }
    }

    private fun buildMessage(wrapper: ItemChangeWrapper, difference: List<SizeInfo>): String {
        return listOf(
            messageOnPriceChange(wrapper.item, requireNotNull(wrapper.itemChange?.priceLog)),
            messageOnAdditionalInfoChange(difference),
            wrapper.item.url
        )
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")
    }

    private fun messageOnPriceChange(item: Item, priceChange: PriceLogView): String {
        if (priceChange.priceChange == PriceChange.NONE) return ""
        return """Цена ${priceChange.priceChange.literal} на ${calculatePercentage(priceChange.priceNow, priceChange.priceBefore)}%!
              |Было - ${priceChange.priceBefore.setScale(2, RoundingMode.HALF_UP)} ${item.priceCurrency}
              |Стало - ${priceChange.priceNow.setScale(2, RoundingMode.HALF_UP)} ${item.priceCurrency}"""
            .trimMargin()
    }

    private fun messageOnAdditionalInfoChange(difference: List<SizeInfo>): String {
        if (difference.isEmpty()) return ""
        return difference.toAvailabilityMessage()
    }

    private fun calculatePercentage(now: BigDecimal, before: BigDecimal): BigDecimal {
        return ((BigDecimal.ONE - now.divide(before, MathContext.DECIMAL32)).abs() * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
    }
}

private const val ITEM_NOT_AVAILABLE_MESSAGE = "Товар более не доступен в магазине. Все уведомления по нему будут отключены."
