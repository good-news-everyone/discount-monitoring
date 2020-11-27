package com.hometech.discount.monitoring.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
@Component
class ApplicationProperties {
    var bot: BotProperties = BotProperties()
}

class BotProperties(
    var name: String = "",
    var token: String = ""
)
