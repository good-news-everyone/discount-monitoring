package com.hometech.discount.monitoring.helper

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import org.apache.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
import java.net.URLEncoder

private val mediaType = "$APPLICATION_FORM_URLENCODED_VALUE;charset=UTF-8"

fun stubSendMessage(chatId: Long, message: String, status: Int = 200) {
    stubFor(
        post(urlPathEqualTo("/botnot-valid/sendMessage"))
            .withHeader("Content-Type", equalTo(mediaType))
            .withRequestBody(equalTo("chat_id=$chatId&text=${message.encode()}"))
            .willReturn(
                aResponse()
                    .withStatus(status)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""{"success": "true"}""")
            )
    )
}

fun verifySendMessage(chatId: Long, message: String) {
    verify(
        postRequestedFor(urlPathEqualTo("/botnot-valid/sendMessage"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(mediaType))
            .withRequestBody(equalTo("chat_id=$chatId&text=${message.encode()}"))
    )
}

private fun String.encode() = URLEncoder.encode(this, Charsets.UTF_8)
