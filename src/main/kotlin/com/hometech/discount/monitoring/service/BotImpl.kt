package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.entity.getUser
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class BotImpl(
    val applicationProperties: ApplicationProperties,
    val itemService: ItemService
) : TelegramLongPollingBot() {

    private val regex = Regex("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$")

    override fun getBotToken(): String = applicationProperties.bot.token

    override fun getBotUsername(): String = applicationProperties.bot.name

    override fun onUpdateReceived(update: Update) {
        val message = update.message.text
        val isUrl = message.contains(regex)
        val chatId = update.message.chatId.toString()
        if (isUrl) {
            try {
                itemService.createItem(message, update.message.getUser())
                reply(chatId, "Запрос принят! Мы оповестим о изменении цены.")
            } catch (e: RuntimeException) {
                reply(chatId, "Что то пошло не так :(")
            }
        } else {
            reply(chatId, "Нет ссылки в сообщении :(")
        }
    }

    fun reply(chatId: String, message: String) {
        execute(
            SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build()
        )
    }
}
