package com.hometech.discount.monitoring.parser

import com.hometech.discount.monitoring.common.nonNull
import com.hometech.discount.monitoring.common.parallelMap
import com.hometech.discount.monitoring.domain.exposed.entity.Proxy
import com.hometech.discount.monitoring.domain.exposed.entity.ProxyTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ProxyDictionary {

    private val log = KotlinLogging.logger {}
    private val proxies = mutableListOf<ProxyConnectionInfo>()

    fun init() {
        log.debug { "Proxy scraper : warm up proxies cache.." }
        val proxies = transaction { Proxy.all().toList() }
            .map {
                ProxyConnectionInfo(ip = it.ip, port = it.port)
            }
        this.proxies.addAll(proxies)
    }

    @Scheduled(cron = "0 15/45 * * * *")
    fun checkProxyList() {
        log.debug { "Proxy scraper : Retrieving proxies list..." }
        val proxies = runBlocking(Dispatchers.IO) {
            Jsoup.connect(PROXY_LIST_URL).execute().body().toProxyList()
                .parallelMap {
                    Pair(it, isProxyAvailable(it.ip, it.port))
                }
                .groupBy({ it.second }, { it.first })[true].nonNull()
        }
        log.debug { "Proxy scraper : ${proxies.size} is available" }
        this.proxies.clear()
        this.proxies.addAll(proxies)
        transaction {
            ProxyTable.deleteAll()
            proxies.forEach {
                Proxy.new {
                    this.ip = it.ip
                    this.port = it.port
                    this.timestamp = LocalDateTime.now()
                }
            }
        }
    }

    fun random(): ProxyConnectionInfo {
        if (proxies.isEmpty()) init()
        if (proxies.isEmpty()) checkProxyList()
        return proxies.random()
    }

    private fun String.toProxyList(): List<ProxyConnectionInfo> {
        return this.trim().split("\n")
            .map {
                val split = it.trim().split(":")
                val ip = split[0]
                val port = split[1].toInt()
                ProxyConnectionInfo(ip = ip, port = port)
            }
    }

    private suspend fun isProxyAvailable(host: String, port: Int): Boolean {
        return try {
            Jsoup.connect("https://www.zara.com/ru/ru/z-kompaniya-corp1391.html").timeout(10000).proxy(host, port).get()
            log.debug { "Proxy scraper : $host:$port is available!" }
            true
        } catch (ex: Exception) {
            log.debug { "Proxy scraper : $host:$port is not available!" }
            false
        }
    }
}

private const val PROXY_LIST_URL = "https://api.proxyscrape.com/v2/?request=getproxies&protocol=http&timeout=10000&country=all&ssl=yes&anonymity=all"

data class ProxyConnectionInfo(val ip: String, val port: Int)
