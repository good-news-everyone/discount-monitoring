package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.PriceChange
import com.hometech.discount.monitoring.domain.entity.PriceLog
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.MessageBody
import com.hometech.discount.monitoring.domain.repository.UserRepository
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Component
class NotifyService(
    private val restTemplate: RestTemplate,
    private val userRepository: UserRepository,
    applicationProperties: ApplicationProperties
) {

    private val uri = "https://api.telegram.org/bot${applicationProperties.bot.token}/sendMessage"

    fun notifyUsers(notifyingItems: List<ItemChangeWrapper>) {
        notifyingItems
            .filter { it.isItemChanged() }
            .forEach { wrapper ->
                val users = userRepository.findAllUsersSubscribedOnItem(
                    requireNotNull(wrapper.item.id)
                )
                users.forEach {
                    sendMessage(it, buildMessage(wrapper))
                }
            }
    }

    fun sendMessageToAllUsers(message: String) {
        userRepository.findAll().forEach { sendMessage(it, message) }
    }

    fun sendMessageToUser(userId: Int, message: String) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User with id = $userId not found") }
        sendMessage(user, message)
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

    private fun buildMessage(wrapper: ItemChangeWrapper): String {
        return listOf(
            messageOnPriceChange(wrapper.item, requireNotNull(wrapper.itemChange?.priceLog)),
            messageOnAdditionalInfoChange(requireNotNull(wrapper.itemChange?.additionalInfoLog)),
            wrapper.item.url

        ).filter { it.isNotEmpty() }.joinToString(separator = "\n")
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
