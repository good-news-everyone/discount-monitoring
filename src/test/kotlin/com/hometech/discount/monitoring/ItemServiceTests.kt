package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.entity.BotUser
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.helper.ZARA_URL
import com.hometech.discount.monitoring.helper.randomItemInfo
import com.hometech.discount.monitoring.helper.randomUser
import com.hometech.discount.monitoring.parser.ParserType
import com.hometech.discount.monitoring.service.CheckDiscountService
import com.hometech.discount.monitoring.service.ItemService
import com.hometech.discount.monitoring.service.NotifyService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import kotlinx.coroutines.ObsoleteCoroutinesApi
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
        subscribersRepository.deleteAll()
        userRepository.deleteAll()
        itemRepository.deleteAll()
    }

    @Test
    fun `should create item`() {
        val info = randomItemInfo()
        val user = randomUser()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info

        itemService.createItem(ZARA_URL, user)

        val items = itemRepository.findAll()
        items.size.shouldBe(1)
        checkItem(items.first(), info)

        val users = userRepository.findAll()
        users.size.shouldBe(1)
        checkUser(user, users.first())

        val subscriptions = subscribersRepository.findAll()
        subscriptions.size.shouldBe(1)
        subscriptions.first().itemId.shouldBe(items.first().id)
        subscriptions.first().userId.shouldBe(user.id)
    }

    @Test
    fun `should add subscription only if other users subscribed on item`() {
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info
        val firstSubscriber = randomUser()
        val secondSubscriber = randomUser()

        itemService.createItem(ZARA_URL, firstSubscriber)

        itemRepository.findAll().size.shouldBe(1)
        userRepository.findAll().size.shouldBe(1)
        subscribersRepository.findAll().size.shouldBe(1)

        itemService.createItem(ZARA_URL, secondSubscriber)

        itemRepository.findAll().size.shouldBe(1)
        userRepository.findAll().size.shouldBe(2)
        subscribersRepository.findAll().size.shouldBe(2)
    }

    @Test
    fun `should remove item if only one users subscribed on item`() {
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info
        val firstSubscriber = randomUser()

        itemService.createItem(ZARA_URL, firstSubscriber)
        itemService.removeSubscriptionByUrl(ZARA_URL, firstSubscriber.id)

        itemRepository.findAll().shouldBeEmpty()
        subscribersRepository.findAll().shouldBeEmpty()
        userRepository.findAll().size.shouldBe(1)
    }

    @Test
    fun `should remove subscription only if multiple users subscribed on item`() {
        val info = randomItemInfo()
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info
        val firstSubscriber = randomUser()
        val secondSubscriber = randomUser()

        itemService.createItem(ZARA_URL, firstSubscriber)
        itemService.createItem(ZARA_URL, secondSubscriber)

        itemService.removeSubscriptionByUrl(ZARA_URL, firstSubscriber.id)

        itemRepository.findAll().size.shouldBe(1)
        userRepository.findAll().size.shouldBe(2)
        val subscriptions = subscribersRepository.findAll()
        subscriptions.size.shouldBe(1)
        subscriptions.first().userId.shouldBe(secondSubscriber.id)
    }

    @Test
    fun `should find items by userId`() {
        val anotherUrl = "$ZARA_URL/another"
        val info1 = randomItemInfo()
        val info2 = randomItemInfo(url = anotherUrl)
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info1
        every { checkDiscountService.parseItemInfo(anotherUrl) } returns info2
        val firstSubscriber = randomUser()
        val secondSubscriber = randomUser()

        itemService.createItem(ZARA_URL, firstSubscriber)
        itemService.createItem(anotherUrl, secondSubscriber)

        val items = itemService.findItemsByUser(firstSubscriber.id)
        items.size.shouldBe(1)
        checkItem(items.first(), info1)
    }

    @Test
    fun `should cleanup invalid zara urls`() {
        val anotherUrl = "$ZARA_URL/another"
        val info1 = randomItemInfo()
        val info2 = randomItemInfo(url = anotherUrl)
        every { checkDiscountService.parseItemInfo(ZARA_URL) } returns info1
        every { checkDiscountService.parseItemInfo(anotherUrl) } returns info2

        val user = randomUser()

        itemService.createItem(ZARA_URL, user)
        itemService.createItem(anotherUrl, user)

        every { checkDiscountService.parseItemInfo(anotherUrl) } throws HttpStatusException("", 410, anotherUrl)
        every { notifyService.notifyUsersAboutItemDeletion(any()) } just Runs

        itemService.cleanUpItems()
        val items = itemRepository.findAll()
        items.size.shouldBe(1)
        checkItem(items.first(), info1)

        val subscriptions = subscribersRepository.findAll()
        subscriptions.size.shouldBe(1)
        subscriptions.first().itemId.shouldBe(items.first().id)
    }

    private fun checkItem(item: Item, info: ItemInfo) {
        item.url.shouldBe(info.url)
        item.name.shouldBe(info.name)
        item.priceCurrency.shouldBe(info.priceCurrency)
        item.currentPrice.shouldBe(info.price)
        item.lowestPrice.shouldBe(info.price)
        item.highestPrice.shouldBe(info.price)
        item.initialPrice.shouldBe(info.price)
        item.type.shouldBe(ParserType.ZARA)
    }

    private fun checkUser(expected: BotUser, actual: BotUser) {
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
