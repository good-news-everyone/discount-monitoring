package com.hometech.discount.monitoring.domain.model

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

data class MessageBody(
    val chatId: Long,
    val text: String
) {
    fun toMultivaluedMap(): MultiValueMap<String, String> {
        return LinkedMultiValueMap<String, String>().apply {
            add("chat_id", chatId.toString())
            add("text", text)
        }
    }
}
