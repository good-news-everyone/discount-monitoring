package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.Message
import com.hometech.discount.monitoring.domain.exposed.entity.MessageDirection
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChange
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.model.AdditionalInfoLogView
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.MessageBody
import com.hometech.discount.monitoring.domain.model.PriceLogView
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
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
        notifyingItems
            .filter { it.itemChange != null && it.isItemChanged() }
            .forEach { wrapper ->
                val users = transaction { wrapper.item.subscribers }.toList()
                users.forEach {
                    try {
                        val messageBody = buildMessage(wrapper)
                        sendMessage(it, messageBody)
                        transaction { saveMessage(it, messageBody) }
                    } catch (ex: HttpClientErrorException) {
                        log.error { ex }
                        if (ex.statusCode == HttpStatus.FORBIDDEN) setBlockedBy(it)
                    }
                }
            }
    }

    fun notifyUsersAboutItemDeletion(item: Item) {
        transaction { item.subscribers }.forEach {
            val message = """
                $ITEM_NOT_AVAILABLE_MESSAGE
                ${item.url}
            """.trimIndent()
            sendMessage(it, message)
        }
    }

    fun sendMessageToAllUsers(message: String) {
        User.findAll().forEach { sendMessage(it, message) }
    }

    fun sendMessageToUser(userId: Int, message: String) {
        val user = User.findById(userId.toLong()) ?: throw NoSuchElementException("User with id = $userId not found")
        sendMessage(user, message)
    }

    fun setBlockedBy(user: User) {
        user.isBlockedBy = true
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
        }
    }

    private fun buildMessage(wrapper: ItemChangeWrapper): String {
        return listOf(
            messageOnPriceChange(wrapper.item, requireNotNull(wrapper.itemChange?.priceLog)),
            messageOnAdditionalInfoChange(requireNotNull(wrapper.itemChange?.additionalInfoLog)),
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

    private fun messageOnAdditionalInfoChange(additionalInfoChange: AdditionalInfoLogView): String {
        if (additionalInfoChange.infoBefore == additionalInfoChange.infoNow) return ""
        return additionalInfoChange.difference()
    }

    private fun calculatePercentage(now: BigDecimal, before: BigDecimal): BigDecimal {
        return ((BigDecimal.ONE - now.divide(before, MathContext.DECIMAL32)).abs() * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
    }
}

private const val ITEM_NOT_AVAILABLE_MESSAGE = "Товар более не доступен в магазине. Все уведомления по нему будут отключены."
