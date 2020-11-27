package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.entity.getUser
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class IncomingMessageListener(
    private val applicationProperties: ApplicationProperties,
    private val itemService: ItemService,
    private val commandHandler: CommandHandler
) : TelegramLongPollingBot() {

    private val regex = Regex("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$")

    override fun getBotToken(): String = applicationProperties.bot.token

    override fun getBotUsername(): String = applicationProperties.bot.name

    override fun onUpdateReceived(update: Update) {
        when (update.message.messageType()) {
            MessageType.URL -> handleUrl(update)
            MessageType.COMMAND -> handleCommand(update)
            else -> reply(update.chatId(), "В сообщении нет ссылки или команды :(")
        }
    }

    private fun handleUrl(update: Update) {
        val chatId = update.chatId()
        try {
            itemService.createItem(update.message.text, update.message.getUser())
            reply(chatId, "Запрос принят! Мы оповестим о изменении цены.")
        } catch (e: RuntimeException) {
            reply(chatId, "Что то пошло не так :(")
        }
    }

    private fun handleCommand(update: Update) {
        val message = commandHandler.handleCommand(update)
        reply(update.chatId(), message)
    }

    private fun reply(chatId: String, message: String) {
        execute(
            SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build()
        )
    }

    private fun Message.messageType(): MessageType {
        if (this.text.isCommand()) return MessageType.COMMAND
        if (this.isUrl()) return MessageType.URL
        return MessageType.TRASH
    }

    private fun String.isCommand(): Boolean {
        return CommandHandler.commands.any { this.trimStart().startsWith(it) }
    }

    private fun Message.isUrl(): Boolean {
        return this.text.contains(regex)
    }

    private fun Update.chatId(): String {
        return this.message.chatId.toString()
    }

    enum class MessageType {
        URL, COMMAND, TRASH
    }
}
