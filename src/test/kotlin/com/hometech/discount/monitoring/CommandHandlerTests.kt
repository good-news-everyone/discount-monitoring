package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.exposed.entity.UserTable
import com.hometech.discount.monitoring.domain.exposed.entity.getUser
import com.hometech.discount.monitoring.helper.ZARA_URL
import com.hometech.discount.monitoring.helper.createRelations
import com.hometech.discount.monitoring.helper.randomInt
import com.hometech.discount.monitoring.helper.randomItem
import com.hometech.discount.monitoring.helper.randomString
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.service.CommandHandler
import com.hometech.discount.monitoring.service.CommandHandler.Companion.SUCCESS
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Contact
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.math.absoluteValue
import org.telegram.telegrambots.meta.api.objects.User as TelegramUser

@ObsoleteCoroutinesApi
class CommandHandlerTests : BaseIntegrationTest() {

    @Autowired
    lateinit var commandHandler: CommandHandler

    @BeforeEach
    fun cleanUpDatabase() {
        transaction {
            ItemSubscriptionTable.deleteAll()
            UserTable.deleteAll()
            ItemTable.deleteAll()
        }
    }

    @Test
    fun `should return welcome message`() {
        val help = createUpdate("/help")
        commandHandler.handleCommand(help).shouldBe(CommandHandler.welcomeMessage)
        val start = createUpdate("/start")
        commandHandler.handleCommand(start).shouldBe(CommandHandler.welcomeMessage)
    }

    @Test
    fun `should return shops`() {
        val update = createUpdate("/shops")
        commandHandler.handleCommand(update).shouldBe(ParserType.allShops.joinToString(separator = "\n"))
    }

    @Test
    fun `should not handle invalid command`() {
        val update = createUpdate("this is wrong command")
        commandHandler.handleCommand(update).shouldBe(CommandHandler.BAD_COMMAND)
    }

    @Test
    fun `should return items`() {
        val (item, update) = transaction {
            val item = randomItem()
            val update = createUpdate("/goods")
            val user = update.message.getUser()
            createRelations(user, item)
            Pair(item, update)
        }

        val goods = commandHandler.handleCommand(update)

        goods.shouldContain(item.name)
        goods.shouldContain(item.url)
    }

    @Test
    fun `should unsubscribe single item`() {
        val update = transaction {
            val item1 = randomItem()
            val item2 = randomItem(url = "$ZARA_URL/another")
            val update = createUpdate("/unsubscribe ${item1.url}")
            val user = update.message.getUser()

            createRelations(user, item1)
            createRelations(user, item2)
            update
        }

        commandHandler.handleCommand(update).shouldBe(SUCCESS)

        transaction {
            Item.count().shouldBe(1)
            ItemSubscription.count().shouldBe(1)
        }
    }

    @Test
    fun `should unsubscribe all items`() {
        val update = transaction {
            val item1 = randomItem()
            val item2 = randomItem(url = "$ZARA_URL/another")
            val update = createUpdate("/unsubscribe_all")
            val user = update.message.getUser()

            createRelations(user, item1)
            createRelations(user, item2)
            update
        }

        commandHandler.handleCommand(update).shouldBe(SUCCESS)
        transaction {
            Item.count().shouldBe(0)
            ItemSubscription.count().shouldBe(0)
        }
    }

    private fun createUpdate(text: String): Update {
        val contact = Contact().apply {
            this.phoneNumber = randomString()
        }
        val user = TelegramUser().apply {
            this.firstName = randomString()
            this.lastName = randomString()
            this.isBot = false
            this.userName = randomString()
            this.id = randomInt().absoluteValue
        }
        val message = Message().apply {
            this.contact = contact
            this.text = text
            this.from = user
            this.chat = Chat().also {
                it.id = randomInt().absoluteValue.toLong()
                it.userName = randomString()
            }
        }
        return Update().apply {
            this.message = message
        }
    }
}
