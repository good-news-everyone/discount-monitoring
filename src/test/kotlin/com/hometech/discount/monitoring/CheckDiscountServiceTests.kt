package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.configuration.ParserResolver
import com.hometech.discount.monitoring.domain.entity.ItemSubscriber
import com.hometech.discount.monitoring.domain.entity.PriceChange
import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.hometech.discount.monitoring.domain.repository.AdditionalInfoLogRepository
import com.hometech.discount.monitoring.domain.repository.PriceLogRepository
import com.hometech.discount.monitoring.helper.ZARA_URL
import com.hometech.discount.monitoring.helper.randomItemInfo
import com.hometech.discount.monitoring.helper.randomUser
import com.hometech.discount.monitoring.parser.impl.ZaraParser
import com.hometech.discount.monitoring.service.CheckDiscountService
import com.hometech.discount.monitoring.service.NotifyService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@ObsoleteCoroutinesApi
class CheckDiscountServiceTests : BaseIntegrationTest() {

    @Autowired
    lateinit var checkDiscountService: CheckDiscountService
    @Autowired
    lateinit var priceLogRepository: PriceLogRepository
    @Autowired
    lateinit var additionalInfoRepository: AdditionalInfoLogRepository

    @MockkBean
    lateinit var parserResolver: ParserResolver
    @MockkBean
    lateinit var zaraParser: ZaraParser

    @MockkBean
    lateinit var notifyService: NotifyService

    @BeforeEach
    fun cleanUpDatabase() {
        subscribersRepository.deleteAll()
        userRepository.deleteAll()
        additionalInfoRepository.deleteAll()
        priceLogRepository.deleteAll()
        itemRepository.deleteAll()
    }

    @Test
    fun `should find price difference`() {
        val item = itemRepository.save(randomItemInfo(price = BigDecimal.TEN).toEntity())
        val user = userRepository.save(randomUser())
        subscribersRepository.save(ItemSubscriber(itemId = item.id!!, userId = user.id.toLong()))

        val info = ItemInfo(url = ZARA_URL, name = item.name, priceCurrency = item.priceCurrency, price = BigDecimal.ONE, additionalInfo = AdditionalInfo())
        every { zaraParser.getItemInfo(item.url) } returns info
        every { notifyService.notifyUsers(any()) } just Runs
        every { parserResolver.findByUrl(any()) } returns zaraParser

        checkDiscountService.recheckAllItems()
        val logs = priceLogRepository.findAll()
        logs.size.shouldBe(1)
        logs.first().itemId.shouldBe(item.id)
        logs.first().priceBefore.shouldBe(BigDecimal.TEN)
        logs.first().priceNow.shouldBe(BigDecimal.ONE)
        logs.first().priceChange.shouldBe(PriceChange.DOWN)

        additionalInfoRepository.count().shouldBe(0)

        val updatedItem = itemRepository.findById(item.id!!).orElseThrow()
        updatedItem.lowestPrice.shouldBe(BigDecimal.ONE)
        updatedItem.currentPrice.shouldBe(BigDecimal.ONE)
        updatedItem.initialPrice.shouldBe(BigDecimal.TEN)
        updatedItem.highestPrice.shouldBe(BigDecimal.TEN)
    }

    @Test
    fun `should find additional info difference`() {
        val additionalInfoBefore = AdditionalInfo(sizes = listOf(SizeInfo("xs", false)))
        val additionalInfoNow = AdditionalInfo(sizes = listOf(SizeInfo("xs", true)))
        val item = itemRepository.save(randomItemInfo(additionalInfo = additionalInfoBefore).toEntity())
        val user = userRepository.save(randomUser())
        subscribersRepository.save(ItemSubscriber(itemId = item.id!!, userId = user.id.toLong()))

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
        priceLogRepository.count().shouldBe(0)

        val additionalInfoLogs = additionalInfoRepository.findAll()
        additionalInfoLogs.size.shouldBe(1)
        additionalInfoLogs.first().infoBefore.shouldBe(additionalInfoBefore)
        additionalInfoLogs.first().infoNow.shouldBe(additionalInfoNow)
        additionalInfoLogs.first().itemId.shouldBe(item.id)

        val updatedItem = itemRepository.findById(item.id!!).orElseThrow()
        updatedItem.additionalInfo.shouldBe(additionalInfoNow)
    }
}
