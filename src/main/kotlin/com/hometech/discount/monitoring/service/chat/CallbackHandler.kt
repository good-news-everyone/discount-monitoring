package com.hometech.discount.monitoring.service.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.SubscriptionMetadata
import com.hometech.discount.monitoring.domain.model.BaseButtonCallback
import com.hometech.discount.monitoring.domain.model.SubscriptionForSize
import com.hometech.discount.monitoring.domain.model.UnsubscribeItem
import com.hometech.discount.monitoring.service.ItemService
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@ObsoleteCoroutinesApi
@Component
class CallbackHandler(
    private val objectMapper: ObjectMapper,
    private val itemService: ItemService
) {

    fun handleCallback(update: Update): String {
        return when (val callback = update.callbackQuery.data.asObject<BaseButtonCallback>()) {
            is SubscriptionForSize -> handleVariationSubscription(update, callback)
            is UnsubscribeItem -> unsubscribeItem(callback)
        }
    }

    private fun handleVariationSubscription(update: Update, request: SubscriptionForSize): String {
        val sizesText = transaction {
            ItemSubscription.findByItemAndUser(
                itemId = request.id,
                userId = update.callbackQuery.from.id
            ).apply {
                val sizes = this.metadata?.sizes ?: listOf()
                if (sizes.contains(request.size).not()) {
                    this.metadata = SubscriptionMetadata(sizes = sizes + request.size)
                }
            }.metadata.nonNull().sizes ?: listOf()
        }.sorted().joinToString("\n")
        return "$CHOSEN_VARIATIONS_MESSAGE \n$sizesText"
    }

    private fun unsubscribeItem(callback: UnsubscribeItem): String {
        return try {
            transaction { itemService.removeSubscriptionById(callback.id) }
            CommandHandler.SUCCESS_COMMAND_REPLY
        } catch (e: EmptyResultDataAccessException) {
            CommandHandler.BAD_COMMAND_REPLY
        }
    }

    private inline fun <reified T> String.asObject(): T {
        return objectMapper.readValue(this)
    }
}

private const val CHOSEN_VARIATIONS_MESSAGE = "Принято! Теперь отслеживаем только эти размеры:"
