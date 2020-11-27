package com.hometech.discount.monitoring.domain.model

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

data class MessageBody(
    val chatId: Long,
    val text: String
) {
    fun toMultivaluedMap(): MultiValueMap<String, String> {
        val map: MultiValueMap<String, String> = LinkedMultiValueMap()
        map.add("chat_id", chatId.toString())
        map.add("text", text)
        return map
    }
}
