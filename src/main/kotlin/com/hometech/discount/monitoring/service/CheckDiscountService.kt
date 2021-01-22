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
import java.util.concurrent.TimeUnit

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
    fun recheckAllItems() {
        val recheckedItems = measureExecution {
            runBlocking(Dispatchers.Default) { recheckAll() }
        }
        saveItemsAndLogs(recheckedItems)
        notifyService.notifyUsers(recheckedItems)
    }

    private suspend fun recheckAll(): List<ItemChangeWrapper> {
        return itemRepository.findAll().parallelMap {
            val changeWrapper = try {
                recheckItem(it)
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

    private fun saveItemsAndLogs(recheckedItems: List<ItemChangeWrapper>) {
        val priceLogs = mutableListOf<PriceLog>()
        val additionalLogs = mutableListOf<AdditionalInfoLog>()
        val changedItems = mutableListOf<Item>()
        recheckedItems.forEach {
            if (it.itemChange != null) {
                priceLogs.add(it.itemChange.priceLog)
                additionalLogs.add(it.itemChange.additionalInfoLog)
                if (it.isItemChanged()) changedItems.add(it.item)
            }
        }
        priceLogRepository.saveAll(priceLogs)
        additionalInfoLogRepository.saveAll(additionalLogs)
        itemRepository.saveAll(changedItems)
    }

    private fun recheckItem(item: Item): ChangeWrapper {
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

    private inline fun <T> measureExecution(function: () -> T): T {
        val startTime = System.nanoTime()
        return function.invoke().also {
            val difference = System.nanoTime() - startTime
            logger.info("Recheck all items took ${TimeUnit.NANOSECONDS.toMillis(difference)}ms")
        }
    }
}
