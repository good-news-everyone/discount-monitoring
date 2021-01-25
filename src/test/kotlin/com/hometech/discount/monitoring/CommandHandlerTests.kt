package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.ItemSubscriber
import com.hometech.discount.monitoring.helper.ZARA_URL
import com.hometech.discount.monitoring.helper.randomInt
import com.hometech.discount.monitoring.helper.randomItemInfo
import com.hometech.discount.monitoring.helper.randomLong
import com.hometech.discount.monitoring.helper.randomString
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.service.CommandHandler
import com.hometech.discount.monitoring.service.CommandHandler.Companion.SUCCESS
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Contact
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import kotlin.math.absoluteValue

@ObsoleteCoroutinesApi
class CommandHandlerTests : BaseIntegrationTest() {

    @Autowired
    lateinit var commandHandler: CommandHandler

    @BeforeEach
    fun cleanUpDatabase() {
        subscribersRepository.deleteAll()
        userRepository.deleteAll()
        itemRepository.deleteAll()
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
        val item = randomItemInfo().toEntity()
        val update = createUpdate("/goods")
        val user = update.message.getUser()

        createRelations(user, item)
        val goods = commandHandler.handleCommand(update)

        goods.shouldContain(item.name)
        goods.shouldContain(item.url)
    }

    @Test
    fun `should unsubscribe single item`() {
        val item1 = randomItemInfo().toEntity()
        val item2 = randomItemInfo(url = "$ZARA_URL/another").toEntity()
        val update = createUpdate("/unsubscribe ${item1.url}")
        val user = update.message.getUser()

        createRelations(user, item1)
        createRelations(user, item2)

        commandHandler.handleCommand(update).shouldBe(SUCCESS)
        itemRepository.count().shouldBe(1)
        subscribersRepository.count().shouldBe(1)
    }

    @Test
    fun `should unsubscribe all items`() {
        val item1 = randomItemInfo().toEntity()
        val item2 = randomItemInfo(url = "$ZARA_URL/another").toEntity()
        val update = createUpdate("/unsubscribe_all")
        val user = update.message.getUser()

        createRelations(user, item1)
        createRelations(user, item2)

        commandHandler.handleCommand(update).shouldBe(SUCCESS)
        itemRepository.count().shouldBe(0)
        subscribersRepository.count().shouldBe(0)
    }

    private fun createRelations(user: BotUser, item: Item) {
        userRepository.save(user)
        itemRepository.save(item)
        subscribersRepository.save(
            ItemSubscriber(itemId = item.id!!, userId = user.id.toLong())
        )
    }

    private fun createUpdate(text: String): Update {
        val contact = Contact().apply {
            this.phoneNumber = randomString()
        }
        val user = User().apply {
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
                it.userName = randomString()
            }
        }
        return Update().apply {
            this.message = message
        }
    }

    fun Message.getUser(): BotUser {
        return BotUser(
            id = this.from.id,
            chatId = randomLong().absoluteValue,
            firstName = this.from.firstName,
            isBot = this.from.isBot,
            lastName = this.from.lastName,
            userName = this.from.userName,
            isBlockedBy = false,
            contact = this.contact.phoneNumber
        )
    }
}