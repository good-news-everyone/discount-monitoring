package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.exposed.entity.Message
import com.hometech.discount.monitoring.domain.exposed.entity.MessageDirection
import com.hometech.discount.monitoring.domain.exposed.entity.MessageTable
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.exposed.entity.UserTable
import com.hometech.discount.monitoring.domain.exposed.extensions.findOrException
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.AdditionalInfoLogView
import com.hometech.discount.monitoring.domain.model.ChangeWrapper
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.PriceLogView
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.helper.createRelations
import com.hometech.discount.monitoring.helper.randomItem
import com.hometech.discount.monitoring.helper.randomString
import com.hometech.discount.monitoring.helper.randomUser
import com.hometech.discount.monitoring.helper.stubSendMessage
import com.hometech.discount.monitoring.helper.verifySendMessage
import com.hometech.discount.monitoring.service.ITEM_NOT_AVAILABLE_MESSAGE
import com.hometech.discount.monitoring.service.NotifyService
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

val MESSAGE_PRICE_DOWN_M_NOT_AVAILABLE = """
    Цена снизилась на 90.00%!
    Было - 10.00 RUB
    Стало - 1.00 RUB
    Размера M больше нет в наличии!
    https://www.zara.com
""".trimIndent()

@ObsoleteCoroutinesApi
class NotifyServiceTests : BaseIntegrationTest() {

    @Autowired
    lateinit var notifyService: NotifyService

    @BeforeEach
    fun init() {
        transaction {
            ItemSubscriptionTable.deleteAll()
            MessageTable.deleteAll()
            UserTable.deleteAll()
            ItemTable.deleteAll()
        }
    }

    @Test
    fun `should send notification`() {
        val (change, chatId) = transaction {
            val item = randomItem()
            val user = randomUser()
            val change = ChangeWrapper(
                priceLog = PriceLogView(item, priceBefore = BigDecimal.TEN, priceNow = BigDecimal.ONE),
                additionalInfoLog = AdditionalInfoLogView(
                    item = item,
                    infoBefore = AdditionalInfo(listOf(SizeInfo("M", true))),
                    infoNow = AdditionalInfo(listOf(SizeInfo("M", false)))
                )
            )
            createRelations(user, item)
            ItemChangeWrapper(item, change) to user.chatId
        }
        stubSendMessage(chatId, MESSAGE_PRICE_DOWN_M_NOT_AVAILABLE)
        notifyService.notifyUsers(listOf(change))
        verifySendMessage(chatId, MESSAGE_PRICE_DOWN_M_NOT_AVAILABLE)
        transaction {
            Message.all()
                .also { it.count() shouldBe 1L }
                .single().also {
                    it.direction shouldBe MessageDirection.OUTBOUND
                    it.message shouldBe MESSAGE_PRICE_DOWN_M_NOT_AVAILABLE
                }
        }
    }

    @Test
    fun `notify about item deletion`() {
        val (item, chatId) = transaction {
            val item = randomItem()
            val user = randomUser()
            createRelations(user, item)
            item to user.chatId
        }
        val message = """
            $ITEM_NOT_AVAILABLE_MESSAGE
            ${item.url}
        """.trimIndent()
        stubSendMessage(chatId, message)
        notifyService.notifyUsersAboutItemDeletion(item)
        verifySendMessage(chatId, message)
        transaction {
            Message.all()
                .also { it.count() shouldBe 1L }
                .single().also {
                    it.direction shouldBe MessageDirection.OUTBOUND
                    it.message shouldBe message
                }
        }
    }

    @Test
    fun `should send message to all users`() {
        val chatId = transaction { randomUser().chatId }
        val message = randomString()
        stubSendMessage(chatId, message)
        notifyService.sendMessageToAllUsers(message)
        verifySendMessage(chatId, message)
    }

    @Test
    fun `should send message to user`() {
        val (userId, chatId) = transaction { randomUser().let { it.id.value to it.chatId } }
        val message = randomString()
        stubSendMessage(chatId, message)
        notifyService.sendMessageToUser(userId.toInt(), message)
        verifySendMessage(chatId, message)
    }

    @Test
    fun `should not send message to user if user block bot`() {
        val (userId, chatId) = transaction { randomUser().let { it.id.value to it.chatId } }
        val message = randomString()
        stubSendMessage(chatId, message, 403)
        notifyService.sendMessageToUser(userId.toInt(), message)
        verifySendMessage(chatId, message)
        transaction {
            User.findOrException(userId).also { it.isBlockedBy!!.shouldBeTrue() }
        }
    }
}
