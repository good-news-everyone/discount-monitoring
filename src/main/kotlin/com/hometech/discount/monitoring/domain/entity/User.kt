package com.hometech.discount.monitoring.domain.entity

import org.telegram.telegrambots.meta.api.objects.Message
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users")
class BotUser(
    @Id
    val id: Int,
    val chatId: Long,
    val firstName: String?,
    val isBot: Boolean,
    val lastName: String?,
    val userName: String?
)

fun Message.getUser(): BotUser {
    return BotUser(
        id = this.from.id,
        chatId = this.chatId,
        firstName = this.from.firstName,
        isBot = this.from.isBot,
        lastName = this.from.lastName,
        userName = this.from.userName,
    )
}
