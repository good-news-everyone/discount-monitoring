package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.configuration.ParserResolver
import com.hometech.discount.monitoring.domain.exposed.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.exposed.entity.AdditionalInfoLogTable
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscription
import com.hometech.discount.monitoring.domain.exposed.entity.ItemSubscriptionTable
import com.hometech.discount.monitoring.domain.exposed.entity.ItemTable
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChange
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChangeLog
import com.hometech.discount.monitoring.domain.exposed.entity.PriceChangeLogTable
import com.hometech.discount.monitoring.domain.exposed.entity.UserTable
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.helper.ZARA_URL
import com.hometech.discount.monitoring.helper.createRelations
import com.hometech.discount.monitoring.helper.randomItem
import com.hometech.discount.monitoring.helper.randomItemInfo
import com.hometech.discount.monitoring.helper.randomUser
import com.hometech.discount.monitoring.helper.shouldBeEqualsIgnoreScale
import com.hometech.discount.monitoring.parser.impl.ZaraParser
import com.hometech.discount.monitoring.service.CheckDiscountService
import com.hometech.discount.monitoring.service.NotifyService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@ObsoleteCoroutinesApi
class CheckDiscountServiceTests : BaseIntegrationTest() {

    @Autowired
    lateinit var checkDiscountService: CheckDiscountService

    @MockkBean
    lateinit var parserResolver: ParserResolver

    @MockkBean
    lateinit var zaraParser: ZaraParser

    @MockkBean
    lateinit var notifyService: NotifyService

    @BeforeEach
    fun cleanUpDatabase() {
        transaction {
            ItemSubscriptionTable.deleteAll()
            UserTable.deleteAll()
            AdditionalInfoLogTable.deleteAll()
            PriceChangeLogTable.deleteAll()
            ItemTable.deleteAll()
        }
    }

    @Test
    fun `should find price difference`() {
        val item = transaction {
            val item = randomItem(price = BigDecimal.TEN)
            val user = randomUser()
            ItemSubscription.new {
                this.item = item
                this.subscriber = user
            }
            item
        }

        val info = randomItemInfo(
            url = ZARA_URL,
            price = BigDecimal.ONE,
            additionalInfo = AdditionalInfo()
        )

        every { zaraParser.getItemInfo(item.url) } returns info
        every { notifyService.notifyUsers(any()) } just Runs
        every { parserResolver.findByUrl(any()) } returns zaraParser

        checkDiscountService.recheckAllItems()

        transaction {
            val logs = PriceChangeLog.all()
            logs.count().shouldBe(1)
            logs.first().item.id.value.shouldBe(item.id.value)
            logs.first().priceBefore.shouldBeEqualsIgnoreScale(BigDecimal.TEN)
            logs.first().priceNow.shouldBeEqualsIgnoreScale(BigDecimal.ONE)
            logs.first().priceChange.shouldBe(PriceChange.DOWN)

            AdditionalInfoLog.count().shouldBe(0)

            val updatedItem = requireNotNull(Item.findById(item.id))
            updatedItem.lowestPrice.shouldBeEqualsIgnoreScale(BigDecimal.ONE)
            updatedItem.currentPrice.shouldBeEqualsIgnoreScale(BigDecimal.ONE)
            updatedItem.initialPrice.shouldBeEqualsIgnoreScale(BigDecimal.TEN)
            updatedItem.highestPrice.shouldBeEqualsIgnoreScale(BigDecimal.TEN)
        }
    }

    @Test
    fun `should find additional info difference`() {
        val (item, additionalInfoNow, additionalInfoBefore) = transaction {
            val additionalInfoBefore = AdditionalInfo(sizes = listOf(SizeInfo("xs", false)))
            val additionalInfoNow = AdditionalInfo(sizes = listOf(SizeInfo("xs", true)))
            val item = randomItem(additionalInfo = additionalInfoBefore)
            val user = randomUser()
            createRelations(user, item)
            Triple(item, additionalInfoNow, additionalInfoBefore)
        }

        val info = ItemInfo(
            url = ZARA_URL,
            name = item.name,
            priceCurrency = item.priceCurrency,
            price = item.currentPrice,
            additionalInfo = additionalInfoNow
        )
        every { zaraParser.getItemInfo(item.url) } returns info
        every { notifyService.notifyUsers(any()) } just Runs
        every { parserResolver.findByUrl(any()) } returns zaraParser

        checkDiscountService.recheckAllItems()

        transaction {
            PriceChangeLog.count().shouldBe(0L)
            val additionalInfoLogs = AdditionalInfoLog.all()
            additionalInfoLogs.count().shouldBe(1L)
            additionalInfoLogs.first().infoBefore.shouldBe(additionalInfoBefore)
            additionalInfoLogs.first().infoNow.shouldBe(additionalInfoNow)
            additionalInfoLogs.first().item.id.value.shouldBe(item.id.value)

            val updatedItem = Item.findById(item.id).shouldNotBeNull()
            updatedItem.additionalInfo.shouldBe(additionalInfoNow)
        }
    }
}
