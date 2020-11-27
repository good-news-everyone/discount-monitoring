package com.hometech.discount.monitoring.parser

import org.jsoup.nodes.Document
import java.net.URI

fun Document.getTitle(): String = requireNotNull(this.title())

fun Document.getHost(): String {
    val uri = URI.create(this.baseUri())
    return uri.host
}
