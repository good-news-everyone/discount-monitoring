package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.entity.PriceChange
import com.hometech.discount.monitoring.domain.model.ItemPriceWrapper
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

    fun notifyUsers(notifyingItems: List<ItemPriceWrapper>) {
        notifyingItems
            .filter { it.priceChange != null && it.priceChange.priceChange != PriceChange.NONE }
            .forEach {
                val user = userRepository.findAllUsersSubscribedOnItem(
                    requireNotNull(it.item.id)
                )
                val request = HttpEntity<MultiValueMap<String, String>>(
                    MessageBody(user.chatId, buildMessage(it)).toMultivaluedMap(),
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
    }

    private fun buildMessage(wrapper: ItemPriceWrapper): String {
        return """Цена ${wrapper.priceChange!!.priceChange.literal} на ${calculatePercentage(wrapper.priceChange.priceNow, wrapper.priceChange.priceBefore)}%!
              |Было - ${wrapper.priceChange.priceBefore.setScale(2, RoundingMode.HALF_UP)}
              |Стало - ${wrapper.priceChange.priceNow.setScale(2, RoundingMode.HALF_UP)}
              |${wrapper.item.url}""".trimMargin()
    }

    private fun calculatePercentage(now: BigDecimal, before: BigDecimal): BigDecimal {
        return ((BigDecimal.ONE - now.divide(before, MathContext.DECIMAL32)).abs() * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
    }
}
