package com.hometech.discount.monitoring.domain.entity

import org.telegram.telegrambots.meta.api.objects.Message
import javax.persistence.Column
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
    val userName: String?,
    val contact: String?,

    @Column(name = "is_blocked_by")
    var isBlockedBy: Boolean
) {
    override fun toString(): String {
        return "User(id = $id, chatId = $chatId, firstName = $firstName, isBot = $isBot, lastName = $lastName, userName = $userName, contact = $contact)"
    }
}

fun Message.getUser(): BotUser {
    return BotUser(
        id = this.from.id,
        chatId = this.chatId,
        firstName = this.from.firstName,
        isBot = this.from.isBot,
        lastName = this.from.lastName,
        userName = this.from.userName,
        isBlockedBy = false,
        contact = this.contact.phoneNumber
    )
}
