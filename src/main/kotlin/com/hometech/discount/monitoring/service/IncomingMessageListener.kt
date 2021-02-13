package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.exposed.entity.Message
import com.hometech.discount.monitoring.domain.exposed.entity.MessageDirection
import com.hometech.discount.monitoring.domain.exposed.entity.User
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.LocalDateTime
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage

@ObsoleteCoroutinesApi
@Component
@Profile("production")
class IncomingMessageListener(
    private val applicationProperties: ApplicationProperties,
    private val itemService: ItemService,
    private val commandHandler: CommandHandler
) : TelegramLongPollingBot() {

    private val logger = KotlinLogging.logger { }
    private val regex = Regex("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$")

    override fun getBotToken(): String = applicationProperties.bot.token

    override fun getBotUsername(): String = applicationProperties.bot.name

    override fun onUpdateReceived(update: Update) {
        val user = transaction { User.findByMessageOrCreateIfNone(update.message) }
        when (update.message.messageType()) {
            MessageType.URL -> handleUrl(update, user)
            MessageType.COMMAND -> handleCommand(update)
            else -> reply(update.chatId(), "В сообщении нет ссылки или команды :(")
        }
        saveMessage(update, user)
    }

    private fun handleUrl(update: Update, user: User) {
        val chatId = update.chatId()
        try {
            itemService.createItem(update.message.text, user)
            reply(chatId, "Запрос принят! Мы оповестим о изменении цены.")
        } catch (e: Exception) {
            logger.error {
                reply(chatId, "Что то пошло не так :(")
            }
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

    private fun saveMessage(update: Update, user: User) {
        transaction {
            Message.new {
                this.direction = MessageDirection.INBOUND
                this.message = update.message.text
                this.user = user
                this.timestamp = LocalDateTime.now()
            }
        }
    }

    private fun TelegramMessage.messageType(): MessageType {
        if (this.text.isCommand()) return MessageType.COMMAND
        if (this.isUrl()) return MessageType.URL
        return MessageType.TRASH
    }

    private fun String.isCommand(): Boolean {
        return CommandHandler.commands.any { this.trimStart().startsWith(it) }
    }

    private fun TelegramMessage.isUrl(): Boolean {
        return this.text.contains(regex)
    }

    private fun Update.chatId(): String {
        return this.message.chatId.toString()
    }

    enum class MessageType {
        URL, COMMAND, TRASH
    }
}
