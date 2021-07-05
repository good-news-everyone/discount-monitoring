package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.RemoveOutdatedItemEvent
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.exposed.entity.User
import com.hometech.discount.monitoring.domain.exposed.entity.UserTable
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.helper.ZARA_URL
import com.hometech.discount.monitoring.helper.createRelations
import com.hometech.discount.monitoring.helper.randomItem
import com.hometech.discount.monitoring.helper.randomItemInfo
import com.hometech.discount.monitoring.helper.randomUser
import com.hometech.discount.monitoring.helper.shouldBeEqualsIgnoreScale
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.service.CheckDiscountService
import com.hometech.discount.monitoring.service.ItemService
import com.hometech.discount.monitoring.service.NotifyService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.HttpStatusException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@ObsoleteCoroutinesApi
class ItemServiceTests : BaseIntegrationTest() {

    @MockkBean
    lateinit var checkDiscountService: CheckDiscountService

    @MockkBean
    lateinit var notifyService: NotifyService

    @Autowired
    lateinit var itemService: ItemService

    @BeforeEach
    fun init() {
        transaction {
            ItemSubscriptionTable.deleteAll()
            UserTable.deleteAll()
            ItemTable.deleteAll()
        }
    }

    @Test
    fun `should create item`() {
        val user = transaction { randomUser() }
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info

        itemService.createItem(ZARA_URL, user)

        transaction {
            val items = Item.all()
            items.count().shouldBe(1)
            checkItem(items.first(), info)

            val users = User.all()
            users.count().shouldBe(1)
            checkUser(user, users.first())

            val subscriptions = ItemSubscription.all()
            subscriptions.count().shouldBe(1)
            subscriptions.first().item.id.value.shouldBe(items.first().id.value)
            subscriptions.first().subscriber.id.value.shouldBe(user.id.value)
        }
    }

    @Test
    fun `should add subscription only if other users subscribed on item`() {
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info
        val (firstSubscriber, secondSubscriber) = transaction {
            Pair(randomUser(), randomUser())
        }

        itemService.createItem(ZARA_URL, firstSubscriber)

        transaction {
            Item.all().count().shouldBe(1)
            User.all().count().shouldBe(2)
            ItemSubscription.all().count().shouldBe(1)
        }

        itemService.createItem(ZARA_URL, secondSubscriber)

        transaction {
            Item.all().count().shouldBe(1)
            User.all().count().shouldBe(2)
            ItemSubscription.all().count().shouldBe(2)
        }
    }

    @Test
    fun `should remove item if only one users subscribed on item`() {
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info
        val firstSubscriber = transaction { randomUser() }

        val item = itemService.createItem(ZARA_URL, firstSubscriber)
        val subscription = transaction { ItemSubscription.findByItem(item.id.value).first().id.value }
        itemService.removeSubscriptionById(subscription)

        transaction {
            Item.all().count().shouldBe(0)
            User.all().count().shouldBe(1)
            ItemSubscription.all().count().shouldBe(0)
        }
    }

    @Test
    fun `should remove subscription only if multiple users subscribed on item`() {
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info
        val (firstSubscriber, secondSubscriber) = transaction {
            Pair(randomUser(), randomUser())
        }

        val item = itemService.createItem(ZARA_URL, firstSubscriber)
        itemService.createItem(ZARA_URL, secondSubscriber)
        val subscription = transaction { ItemSubscription.findByItem(item.id.value).first().id.value }

        itemService.removeSubscriptionById(subscription)

        transaction {
            Item.all().count().shouldBe(1)
            User.all().count().shouldBe(2)
            val subscriptions = ItemSubscription.all()
            subscriptions.count().shouldBe(1)
            subscriptions.first().subscriber.id.value.shouldBe(secondSubscriber.id.value)
        }
    }

    @Test
    fun `should find items by userId`() {
        val anotherUrl = "$ZARA_URL/another"
        val info1 = randomItemInfo()
        val info2 = randomItemInfo(url = anotherUrl)
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info1
        every { checkDiscountService.parseItemInfo(anotherUrl) } returns info2

        val (firstSubscriber, secondSubscriber) = transaction {
            Pair(randomUser(), randomUser())
        }

        itemService.createItem(ZARA_URL, firstSubscriber)
        itemService.createItem(anotherUrl, secondSubscriber)

        transaction {
            val items = User.findById(firstSubscriber.id.value).shouldNotBeNull().items
            items.count().shouldBe(1)
            checkItem(items.first(), info1)
        }
    }

    @Test
    fun `should remove outdated zara item`() {
        val item = transaction {
            val item = randomItem()
            val user = randomUser()
            createRelations(user, item)
            item
        }
        every { notifyService.notifyUsersAboutItemDeletion(any()) } just Runs
        itemService.removeOutdatedItem(RemoveOutdatedItemEvent(item))
        transaction {
            Item.all().shouldBeEmpty()
            ItemSubscription.all().shouldBeEmpty()
        }
    }

    @Test
    fun `should cleanup invalid zara urls`() {
        val anotherUrl = "$ZARA_URL/another"
        val info1 = randomItemInfo()
        val info2 = randomItemInfo(url = anotherUrl)
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info1
        every { checkDiscountService.parseItemInfo(anotherUrl) } returns info2

        val user = transaction { randomUser() }

        itemService.createItem(ZARA_URL, user)
        itemService.createItem(anotherUrl, user)

        every { checkDiscountService.parseItemInfo(anotherUrl) } throws HttpStatusException("", 410, anotherUrl)
        every { notifyService.notifyUsersAboutItemDeletion(any()) } just Runs

        transaction {
            itemService.cleanUpItems()
            val items = Item.all()
            items.count().shouldBe(1)
            checkItem(items.first(), info1)

            val subscriptions = ItemSubscription.all()
            subscriptions.count().shouldBe(1)
            subscriptions.first().item.id.value.shouldBe(items.first().id.value)
        }
    }

    private fun checkItem(item: Item, info: ItemInfo) {
        item.url.shouldBe(info.url)
        item.name.shouldBe(info.name)
        item.priceCurrency.shouldBe(info.priceCurrency)
        item.currentPrice.shouldBeEqualsIgnoreScale(info.price)
        item.lowestPrice.shouldBeEqualsIgnoreScale(info.price)
        item.highestPrice.shouldBeEqualsIgnoreScale(info.price)
        item.initialPrice.shouldBeEqualsIgnoreScale(info.price)
        item.type.shouldBe(ParserType.ZARA)
    }

    private fun checkUser(expected: User, actual: User) {
        expected.id.shouldBe(actual.id)
        expected.chatId.shouldBe(actual.chatId)
        expected.firstName.shouldBe(actual.firstName)
        expected.isBot.shouldBe(actual.isBot)
        expected.lastName.shouldBe(actual.lastName)
        expected.userName.shouldBe(actual.userName)
        expected.contact.shouldBe(actual.contact)
        expected.isBlockedBy.shouldBe(actual.isBlockedBy)
    }
}
