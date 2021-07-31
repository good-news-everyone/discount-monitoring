package com.hometech.discount.monitoring.service.chat

import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.model.CommandHandlingData
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.service.ItemService
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@ObsoleteCoroutinesApi
@Component
class CommandHandler(
    private val itemService: ItemService,
    private val keyboardProvider: KeyboardProvider
) {

    fun handleCommand(update: Update): CommandHandlingData {
        val args = update.message.text.trim().split(" ")
        if (args.isEmpty()) return CommandHandlingData(BAD_COMMAND_REPLY, keyboardProvider.baseCommands)
        return when (args[0].lowercase()) {
            HELP_COMMAND, START_COMMAND -> CommandHandlingData(welcomeMessage)
            SHOPS_COMMAND -> CommandHandlingData(allShops())
            GOODS_COMMAND -> {
                val items = goods(update.userId())
                val text = if (items.isEmpty()) GOODS_EMPTY_COMMAND_REPLY else GOODS_FOUND_COMMAND_REPLY
                CommandHandlingData(text, keyboardProvider.itemsToUrlButtons(items))
            }
            UNSUBSCRIBE_ALL_COMMAND -> CommandHandlingData(unsubscribeAll(update.userId()))
            UNSUBSCRIBE_COMMAND -> CommandHandlingData(UNSUBSCRIBE_COMMAND_REPLY, keyboardProvider.userItems(userId = update.userId()))
            else -> CommandHandlingData(BAD_COMMAND_REPLY, keyboardProvider.baseCommands)
        }
    }

    private fun allShops(): String {
        return ParserType.allShops.joinToString(separator = "\n")
    }

    private fun goods(userId: Long): List<Item> {
        return transaction {
            User.findById(userId)?.items?.toList() ?: listOf()
        }
    }
    private fun unsubscribeAll(userId: Long): String {
        transaction { itemService.clearSubscriptions(userId) }
        return SUCCESS_COMMAND_REPLY
    }

    private fun Update.userId(): Long = this.message.from.id

    companion object {
        val welcomeMessage = """Привет! Меня зовут Бот.
            |Я помогаю отслеживать скидки на товары в определенных магазинах.
            |Как я работаю? Для того чтобы я отслеживал для вас скидки на товар, нужно всего лишь скинуть мне ссылку на страницу товара, например: 'https://some-shop.com/item123'
            |Если всё получится, то я отвечу сообщением 'Запрос принят! Мы оповестим о изменении цены.'.
            |В противном же случае, я скажу, что у меня не получилось отследить товар.
            | 📌 Чтобы еще раз прочитать это сообщение, просто вызови команду '/help'
            | 📌 Чтобы увидеть список доступных магазинов, введи команду '/shops'
            | 📌 Чтобы увидеть товары, на которые ты подписан, введи '/goods'
            | 📌 Чтобы отписаться от товара, просто введи '/unsubscribe https://some-shop.com/item123'
            | 📌 Чтобы отписаться от всех товаров, введи '/unsubscribe_all'
        """.trimMargin()

        val commands = listOf(START_COMMAND, HELP_COMMAND, SHOPS_COMMAND, GOODS_COMMAND, UNSUBSCRIBE_ALL_COMMAND, UNSUBSCRIBE_COMMAND)

        const val BAD_COMMAND_REPLY = "Команда не точная 💩"
        const val SUCCESS_COMMAND_REPLY = "Успешно!"
        const val GOODS_EMPTY_COMMAND_REPLY = "У вас пока нет товаров."
        const val GOODS_FOUND_COMMAND_REPLY = "Вот какие товары мы нашли по вашему запросу:"
        const val UNSUBSCRIBE_COMMAND_REPLY = "Выберете товар, от которого хотели бы отписаться (действие не может быть отменено, будьте внимательны):"
    }
}

const val START_COMMAND = "/start"
const val HELP_COMMAND = "/help"
const val SHOPS_COMMAND = "/shops"
const val GOODS_COMMAND = "/goods"
const val UNSUBSCRIBE_COMMAND = "/unsubscribe"
const val UNSUBSCRIBE_ALL_COMMAND = "/unsubscribe_all"
