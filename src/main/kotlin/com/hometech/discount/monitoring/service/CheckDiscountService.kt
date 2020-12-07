package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ParserResolver
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.PriceChange
import com.hometech.discount.monitoring.domain.entity.PriceLog
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.ItemPriceWrapper
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
    private val logRepository: PriceLogRepository,
    private val notifyService: NotifyService
) {

    private val logger = KotlinLogging.logger {}

    fun parseItemInfo(url: String): ItemInfo {
        return parserResolver.findByUrl(url).getItemInfo(url)
    }

    @Scheduled(fixedDelay = 5 * 1000 * 60) // раз в 5 минут обновляем цены
    fun recheckAllPrices() {
        val recheckedItems = runBlocking(Dispatchers.Default) {
            itemRepository.findAll().parallelMap {
                val priceLog = try {
                    recheckPrice(it)
                } catch (e: Exception) {
                    logger.error { e }
                    null
                }
                val newPrice = priceLog?.priceNow ?: return@parallelMap ItemPriceWrapper(it, null)
                if (newPrice != it.currentPrice) {
                    it.setNewPrice(newPrice)
                }
                ItemPriceWrapper(it, priceLog)
            }
        }
        logRepository.saveAll(
            recheckedItems.mapNotNull { it.priceChange }
        )
        itemRepository.saveAll(
            recheckedItems.filter { it.priceChange?.priceChange != PriceChange.NONE }.map { it.item }
        )
        notifyService.notifyUsers(recheckedItems)
    }

    fun recheckPrice(item: Item): PriceLog {
        val url = item.url
        val newPrice = parserResolver.findByUrl(url).parsePrice(url)
        return PriceLog(
            itemId = requireNotNull(item.id),
            priceBefore = item.currentPrice,
            priceNow = newPrice
        )
    }

    suspend fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> = coroutineScope {
        map { async { f(it) } }.awaitAll()
    }
}
