package com.hometech.discount.monitoring.service.chat

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.domain.exposed.entity.Message
import com.hometech.discount.monitoring.domain.exposed.entity.MessageDirection
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.service.ItemService
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import java.time.LocalDateTime
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage

@ObsoleteCoroutinesApi
@Component
@Profile("production")
class IncomingMessageListener(
    private val applicationProperties: ApplicationProperties,
    private val itemService: ItemService,
    private val commandHandler: CommandHandler,
    private val keyboardProvider: KeyboardProvider,
    private val callbackHandler: CallbackHandler
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
            else -> {
                reply(
                    chatId = update.chatId(),
                    message = BAD_COMMAND_MESSAGE,
                    replyMarkup = keyboardProvider.baseCommands
                )
            }
        }
        saveMessage(update, user)
    }

    private fun handleCallback(update: Update) {
        val message = callbackHandler.handleCallback(update)
        reply(
            chatId = update.chatId(),
            message = message
        )
    }

    private fun handleUrl(update: Update, user: User) {
        val chatId = update.chatId()
        try {
            val item = itemService.createItem(update.message.text, user)
            val text = if (item.additionalInfo.sizes.isNullOrEmpty()) REQUEST_ACCEPT_MESSAGE
            else "$REQUEST_ACCEPT_MESSAGE. \n$CHOOSE_SOMETHING_MESSAGE"
            reply(
                chatId = chatId,
                message = text,
                replyMarkup = item.additionalInfo.sizes?.let { keyboardProvider.asButtons(item.id.value, it) }
            )
        } catch (e: Exception) {
            logger.error {
                reply(chatId = chatId, message = SOMETHING_WRONG_MESSAGE)
            }
        }
    }

    private fun handleCommand(update: Update) {
        val data = commandHandler.handleCommand(update)
        reply(chatId = update.chatId(), message = data.message, replyMarkup = data.replyKeyboard)
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

    enum class MessageType {
        URL, COMMAND, TRASH
    }
}

private const val BAD_COMMAND_MESSAGE = "В сообщении нет ссылки или команды 💩"
private const val SOMETHING_WRONG_MESSAGE = "Что то пошло не так 💩"
private const val REQUEST_ACCEPT_MESSAGE = "Запрос принят! Мы оповестим о изменении цены."
private const val CHOOSE_SOMETHING_MESSAGE = "Хотите отслеживать что-то конкретное?"
