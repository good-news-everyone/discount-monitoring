package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ParserResolver
import com.hometech.discount.monitoring.domain.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.PriceLog
import com.hometech.discount.monitoring.domain.model.ChangeWrapper
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.repository.AdditionalInfoLogRepository
import com.hometech.discount.monitoring.domain.repository.ItemRepository
import com.hometech.discount.monitoring.domain.repository.PriceLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CheckDiscountService(
    private val parserResolver: ParserResolver,
    private val itemRepository: ItemRepository,
    private val priceLogRepository: PriceLogRepository,
    private val additionalInfoLogRepository: AdditionalInfoLogRepository,
    private val notifyService: NotifyService
) {

    private val logger = KotlinLogging.logger {}

    fun parseItemInfo(url: String): ItemInfo {
        return parserResolver.findByUrl(url).getItemInfo(url)
    }

    @Scheduled(fixedDelay = 5 * 1000 * 60) // раз в 5 минут обновляем информацию о товарах
    fun recheckAllPrices() {
        val recheckedItems = runBlocking(Dispatchers.Default) {
            itemRepository.findAll().parallelMap {
                val changeWrapper = try {
                    recheckPrice(it)
                } catch (e: Exception) {
                    logger.error { e }
                    null
                } ?: return@parallelMap ItemChangeWrapper(it, null)
                if (changeWrapper.priceLog.priceNow != it.currentPrice) {
                    it.setNewPrice(changeWrapper.priceLog.priceNow)
                }
                if (changeWrapper.additionalInfoLog.infoNow != it.additionalInfo) {
                    it.additionalInfo = changeWrapper.additionalInfoLog.infoNow
                }
                ItemChangeWrapper(it, changeWrapper)
            }
        }
        val logsToSave = recheckedItems.mapNotNull { it.itemChange }
        priceLogRepository.saveAll(
            logsToSave.map { it.priceLog }
        )
        additionalInfoLogRepository.saveAll(
            logsToSave.map { it.additionalInfoLog }
        )
        itemRepository.saveAll(
            recheckedItems.filter { it.isItemChanged() }.map { it.item }
        )
        notifyService.notifyUsers(recheckedItems)
    }

    private fun recheckPrice(item: Item): ChangeWrapper {
        val url = item.url
        val itemInfo = parserResolver.findByUrl(url).getItemInfo(url)
        val priceLog = PriceLog(
            itemId = requireNotNull(item.id),
            priceBefore = item.currentPrice,
            priceNow = itemInfo.price
        )
        val infoLog = AdditionalInfoLog(
            itemId = requireNotNull(item.id),
            infoBefore = item.additionalInfo,
            infoNow = itemInfo.additionalInfo
        )
        return ChangeWrapper(
            priceLog = priceLog,
            additionalInfoLog = infoLog
        )
    }

    suspend fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> = coroutineScope {
        map { async { f(it) } }.awaitAll()
    }
}
