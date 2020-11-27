package com.hometech.discount.monitoring.configuration

import com.hometech.discount.monitoring.parser.Parser
import com.hometech.discount.monitoring.parser.ParserType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ParserConfiguration {

    @Bean
    fun parsersMap(parsers: List<Parser>): ParserResolver {
        return ParserResolver(parsers)
    }
}

class ParserResolver(parsers: List<Parser>) {
    private val parsers: Map<ParserType, Parser> = parsers.associateBy { it.getType() }

    fun findByUrl(url: String): Parser {
        val type = ParserType.findByUrl(url)
        return parsers[type] ?: throw RuntimeException("No suitable parser found for URL $url")
    }
}
