package com.hometech.discount.monitoring.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.Message
import com.hometech.discount.monitoring.domain.exposed.entity.MessageDirection
import com.hometech.discount.monitoring.domain.exposed.entity.SubscriptionMetadata
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.domain.model.SubscriptionForSize
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.time.LocalDateTime
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage

@ObsoleteCoroutinesApi
@Component
@Profile("production")
class IncomingMessageListener(
    private val applicationProperties: ApplicationProperties,
    private val itemService: ItemService,
    private val commandHandler: CommandHandler,
    private val objectMapper: ObjectMapper
) : TelegramLongPollingBot() {

    private val logger = KotlinLogging.logger { }
    private val regex = Regex("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$")

    override fun getBotToken(): String = applicationProperties.bot.token

    override fun getBotUsername(): String = applicationProperties.bot.name

    override fun onUpdateReceived(update: Update) {
        val user = transaction { User.findByUpdateOrCreate(update) }
        if (update.hasMessage()) handleMessage(update, user)
        else handleCallback(update)
    }

    private fun handleMessage(update: Update, user: User) {
        when (update.message.messageType()) {
            MessageType.URL -> handleUrl(update, user)
            MessageType.COMMAND -> handleCommand(update)
            else -> reply(update.chatId(), "В сообщении нет ссылки или команды :(")
        }
        saveMessage(update, user)
    }

    private fun handleCallback(update: Update) {
        val request = update.callbackQuery.data.asObject<SubscriptionForSize>()
        val subscribedSizes = transaction {
            ItemSubscription.findByItemAndUser(
                itemId = request.itemId,
                userId = update.callbackQuery.from.id
            ).apply {
                val sizes = this.metadata?.sizes ?: listOf()
                if (sizes.contains(request.sizeName).not()) {
                    this.metadata = SubscriptionMetadata(sizes = sizes + request.sizeName)
                }
            }.metadata.nonNull().sizes ?: listOf()
        }
        reply(
            chatId = update.chatId(),
            message = "Принято! Теперь отслеживаем только эти размеры: \n${subscribedSizes.joinToString("\n")}"
        )
    }

    private fun handleUrl(update: Update, user: User) {
        val chatId = update.chatId()
        try {
            val item = itemService.createItem(update.message.text, user)
            val text = if (item.additionalInfo.sizes.isNullOrEmpty()) "Запрос принят! Мы оповестим о изменении цены."
            else "Запрос принят! Мы оповестим о изменении цены. \nХотите отслеживать что-то конкретное?"
            reply(
                chatId = chatId,
                message = text,
                replyMarkup = item.additionalInfo.sizes?.toButtons(itemId = item.id.value)
            )
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

    private fun reply(
        chatId: String,
        message: String,
        replyMarkup: ReplyKeyboard? = null
    ) {
        val toSend = SendMessage().apply {
            this.chatId = chatId
            this.text = message
            if (replyMarkup != null) this.replyMarkup = replyMarkup
        }
        execute(toSend)
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

    private fun List<SizeInfo>.toButtons(itemId: Long): InlineKeyboardMarkup {
        val buttons = this.map {
            val row = InlineKeyboardButton().apply {
                this.text = it.name
                this.callbackData = SubscriptionForSize(sizeName = it.name, itemId = itemId).asJson()
            }
            listOf(row)
        }
        return InlineKeyboardMarkup().apply {
            this.keyboard = buttons
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
        return if (this.hasMessage()) this.message.chatId.toString()
        else this.callbackQuery.message.chatId.toString()
    }

    fun Any.asJson(): String = objectMapper.writeValueAsString(this)

    private inline fun <reified T> String.asObject(): T {
        return objectMapper.readValue(this)
    }

    enum class MessageType {
        URL, COMMAND, TRASH
    }
}
