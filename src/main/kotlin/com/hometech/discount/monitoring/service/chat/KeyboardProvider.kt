package com.hometech.discount.monitoring.service.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.domain.model.SubscriptionForSize
import com.hometech.discount.monitoring.domain.model.UnsubscribeItem
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class KeyboardProvider(private val objectMapper: ObjectMapper) {

    val baseCommands = ReplyKeyboardMarkup().apply {
        this.selective = true
        this.resizeKeyboard = true
        this.oneTimeKeyboard = true
        val keyboard = listOf(
            KeyboardRow().also { it.add("/help") },
            KeyboardRow().also { it.add("/shops") },
            KeyboardRow().also { it.add("/goods") },
        )
        this.keyboard = keyboard
    }

    fun userItems(userId: Long): InlineKeyboardMarkup {
        val buttons = transaction {
            ItemSubscriptionTable
                .leftJoin(ItemTable)
                .select { ItemSubscriptionTable.subscriber eq userId }
                .map {
                    val subscriptionId = it[ItemSubscriptionTable.id].value
                    val item = Item.wrapRow(it)
                    val row = InlineKeyboardButton().apply {
                        val callbackData = UnsubscribeItem(subscriptionId = subscriptionId)
                        this.text = item.name
                        this.callbackData = objectMapper.toJson(callbackData)
                    }
                    listOf(row)
                }
        }
        return InlineKeyboardMarkup().apply {
            this.keyboard = buttons
        }
    }

    fun asButtons(itemId: Long, sizes: List<SizeInfo>): InlineKeyboardMarkup {
        val buttons = sizes.map {
            val row = InlineKeyboardButton().apply {
                val callbackData = SubscriptionForSize(sizeName = it.name, itemId = itemId)
                this.text = it.name
                this.callbackData = objectMapper.toJson(callbackData)
            }
            listOf(row)
        }
        return InlineKeyboardMarkup().apply {
            this.keyboard = buttons
        }
    }
}

private fun ObjectMapper.toJson(any: Any): String = this.writeValueAsString(any)
