package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.Message
import com.hometech.discount.monitoring.domain.entity.MessageDirection
import com.hometech.discount.monitoring.domain.entity.PriceChange
import com.hometech.discount.monitoring.domain.entity.PriceLog
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.MessageBody
import com.hometech.discount.monitoring.domain.repository.MessageRepository
import com.hometech.discount.monitoring.domain.repository.UserRepository
import com.hometech.discount.monitoring.domain.repository.findAll
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Component
class NotifyService(
    private val restTemplate: RestTemplate,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
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
                val users = userRepository.findAllUsersSubscribedOnItem(
                    requireNotNull(wrapper.item.id)
                )
                users.forEach {
                    try {
                        val messageBody = buildMessage(wrapper)
                        sendMessage(it, messageBody)
                        saveMessage(it, messageBody)
                    } catch (ex: HttpClientErrorException) {
                        log.error { ex }
                        if (ex.statusCode == HttpStatus.FORBIDDEN) setBlockedBy(it)
                    }
                }
            }
    }

    fun notifyUsersAboutItemDeletion(item: Item) {
        userRepository.findAllUsersSubscribedOnItem(item.id!!).forEach {
            val message = """
                $ITEM_NOT_AVAILABLE_MESSAGE
                ${item.url}
            """.trimIndent()
            sendMessage(it, message)
        }
    }

    fun sendMessageToAllUsers(message: String) {
        userRepository.findAll(includeBlockedBy = false).forEach { sendMessage(it, message) }
    }

    fun sendMessageToUser(userId: Int, message: String) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User with id = $userId not found") }
        sendMessage(user, message)
    }

    @Transactional
    fun setBlockedBy(user: BotUser) {
        user.isBlockedBy = true
        userRepository.save(user)
        itemService.clearSubscriptions(userId = user.id)
        log.warn { "Blocked by $user! All subscriptions well be removed" }
    }

    private fun sendMessage(user: BotUser, message: String) {
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

    private fun saveMessage(user: BotUser, message: String) {
        messageRepository.save(
            Message(user = user, message = message, direction = MessageDirection.OUTBOUND)
        )
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

    private fun messageOnPriceChange(item: Item, priceChange: PriceLog): String {
        if (priceChange.priceChange == PriceChange.NONE) return ""
        return """Цена ${priceChange.priceChange.literal} на ${calculatePercentage(priceChange.priceNow, priceChange.priceBefore)}%!
              |Было - ${priceChange.priceBefore.setScale(2, RoundingMode.HALF_UP)} ${item.priceCurrency}
              |Стало - ${priceChange.priceNow.setScale(2, RoundingMode.HALF_UP)} ${item.priceCurrency}"""
            .trimMargin()
    }

    private fun messageOnAdditionalInfoChange(additionalInfoChange: AdditionalInfoLog): String {
        if (additionalInfoChange.infoBefore == additionalInfoChange.infoNow) return ""
        return additionalInfoChange.difference()
    }

    private fun calculatePercentage(now: BigDecimal, before: BigDecimal): BigDecimal {
        return ((BigDecimal.ONE - now.divide(before, MathContext.DECIMAL32)).abs() * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
    }
}

private const val ITEM_NOT_AVAILABLE_MESSAGE = "Товар более не доступен в магазине. Все уведомления по нему будут отключены."
