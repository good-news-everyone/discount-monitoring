package com.hometech.discount.monitoring.service

import com.hometech.discount.monitoring.configuration.ApplicationProperties
import com.hometech.discount.monitoring.configuration.ParserResolver
import com.hometech.discount.monitoring.domain.exposed.entity.Item
import com.hometech.discount.monitoring.domain.model.AdditionalInfoLogView
import com.hometech.discount.monitoring.domain.model.ChangeWrapper
import com.hometech.discount.monitoring.domain.model.ItemChangeWrapper
import com.hometech.discount.monitoring.domain.model.ItemInfo
import com.hometech.discount.monitoring.domain.model.PriceLogView
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@ObsoleteCoroutinesApi
@Service
class CheckDiscountService(
    private val parserResolver: ParserResolver,
    private val notifyService: NotifyService,
    applicationProperties: ApplicationProperties
) {
    private val coroutineDispatcher = newFixedThreadPoolContext(
        nThreads = applicationProperties.threadsCount,
        name = "Items recheck context"
    )
    private val log = KotlinLogging.logger {}

    fun parseItemInfo(url: String): ItemInfo {
        return parserResolver.findByUrl(url).getItemInfo(url)
    }

    @Scheduled(fixedDelay = 2 * 1000 * 60) // раз в 2 минуты запускаем джобу по обновлению товаров
    fun recheckAllItems() {
        val recheckedItems = measureExecution {
            runBlocking(context = coroutineDispatcher) { recheckAll() }
        }
        transaction { saveLogs(recheckedItems) }
        notifyService.notifyUsers(recheckedItems)
    }

    private suspend fun recheckAll(): List<ItemChangeWrapper> {
        return transaction { Item.all().toList() }
            .parallelMap {
                val changeWrapper = try {
                    recheckItem(it)
                } catch (e: Exception) {
                    log.error { "Error occurred while parsing item info $it. Error: ${e.javaClass} ${e.message}" }
                    log.trace { e.printStackTrace() }
                    null
                } ?: return@parallelMap ItemChangeWrapper(it, null)
                transaction {
                    if (changeWrapper.priceLog.priceNow != it.currentPrice) {
                        it.setNewPrice(changeWrapper.priceLog.priceNow)
                    }
                    if (changeWrapper.additionalInfoLog.infoNow != it.additionalInfo) {
                        it.additionalInfo = changeWrapper.additionalInfoLog.infoNow
                    }
                }
                ItemChangeWrapper(it, changeWrapper)
            }
    }

    private fun saveLogs(recheckedItems: List<ItemChangeWrapper>) {
        recheckedItems.forEach {
            if (it.itemChange != null) {
                if (it.itemChange.priceLog.hasChanges()) it.itemChange.priceLog.createEntity()
                if (it.itemChange.additionalInfoLog.hasChanges()) it.itemChange.additionalInfoLog.createEntity()
            }
        }
    }

    private fun recheckItem(item: Item): ChangeWrapper {
        val url = item.url
        val itemInfo = parserResolver.findByUrl(url).getItemInfo(url)
        val priceLog = PriceLogView(
            item = item,
            priceBefore = item.currentPrice,
            priceNow = itemInfo.price
        )
        val infoLog = AdditionalInfoLogView(
            item = item,
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
            log.info("Recheck all items took ${TimeUnit.NANOSECONDS.toSeconds(difference)} seconds")
        }
    }
}
