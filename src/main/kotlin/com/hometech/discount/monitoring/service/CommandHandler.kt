package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.parser.ParserType
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@ObsoleteCoroutinesApi
@Component
class CommandHandler(private val itemService: ItemService) {

    fun handleCommand(update: Update): String {
        val args = update.message.text.trim().split(" ")
        if (args.isEmpty()) return BAD_COMMAND
        return when (args[0]) {
            "/help", "/start" -> welcomeMessage
            "/shops" -> allShops()
            "/goods" -> goods(update.userId())
            "/unsubscribe" -> if (args.size != 2 && args[1].isEmpty()) BAD_COMMAND else unsubscribe(args[1], update.userId())
            "/unsubscribe_all" -> unsubscribeAll(update.userId())
            else -> BAD_COMMAND
        }
    }

    private fun allShops(): String {
        return ParserType.allShops.joinToString(separator = "\n")
    }

    private fun goods(userId: Long): String {
        return transaction {
            User.findById(userId)?.items?.joinToString(separator = "\n") { it.asString() } ?: ""
        }
    }

    private fun unsubscribe(url: String, userId: Long): String {
        return try {
            transaction { itemService.removeSubscriptionByUrl(url, userId) }
            SUCCESS
        } catch (e: EmptyResultDataAccessException) {
            BAD_COMMAND
        }
    }

    private fun unsubscribeAll(userId: Long): String {
        transaction { itemService.clearSubscriptions(userId) }
        return SUCCESS
    }

    private fun Item.asString(): String = "\uD83D\uDCB8 ${this.name}, ${this.url}"
    private fun Update.userId(): Long = this.message.from.id

    companion object {
        val welcomeMessage = """Привет! Меня зовут Бот.
            | Я помогаю отслеживать скидки на товары в определенных магазинах.
            | Как я работаю? Для того чтобы я отслеживал для вас скидки на товар, нужно всего лишь скинуть мне ссылку на страницу товара, например: 'https://some-shop.com/item123'
            | Если всё получится, то я отвечу сообщением 'Запрос принят! Мы оповестим о изменении цены.'.
            | В противном же случае, я скажу, что у меня не получилось отследить товар.
            | 📌 Чтобы еще раз прочитать это сообщение, просто вызови команду '/help'
            | 📌 Чтобы увидеть список доступных магазинов, введи команду '/shops'
            | 📌 Чтобы увидеть товары, на которые ты подписан, введи '/goods'
            | 📌 Чтобы отписаться от товара, просто введи '/unsubscribe https://some-shop.com/item123'
            | 📌 Чтобы отписаться от всех товаров, введи '/unsubscribe_all'
        """.trimMargin()

        val commands = listOf("/start", "/help", "/shops", "/goods", "/unsubscribe", "/unsubscribe_all")

        const val BAD_COMMAND = "Команда не точная %("
        const val SUCCESS = "Успешно!"
    }
}
