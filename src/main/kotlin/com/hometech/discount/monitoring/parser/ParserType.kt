package com.hometech.discount.monitoring.parser

import java.net.URI

enum class ParserType(val baseUrl: String) {
    HM_HOME("www2.hm.com");

    companion object {
        private val associatedByUrl = ParserType.values().associateBy { it.baseUrl }

        fun findByUrl(url: String): ParserType {
            val baseUrl = URI.create(url).host
            return associatedByUrl[baseUrl] ?: throw RuntimeException("No suitable parser found for URL $url!")
        }
    }
}
