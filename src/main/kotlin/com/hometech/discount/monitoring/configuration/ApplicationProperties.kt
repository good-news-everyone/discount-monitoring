package com.hometech.discount.monitoring.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
@ConstructorBinding
data class ApplicationProperties(
    val bot: BotProperties = BotProperties(),
    val threadsCount: Int,
    val baseUrl: String = "https://api.telegram.org"
)

@ConstructorBinding
data class BotProperties(
    val name: String = "",
    val token: String = ""
)
