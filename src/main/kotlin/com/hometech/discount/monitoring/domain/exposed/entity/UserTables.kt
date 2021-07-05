package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage
import org.telegram.telegrambots.meta.api.objects.User as TelegramUser

object UserTable : LongIdTable("users") {
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val userName = text("user_name").nullable()
    val isBot = bool("is_bot")
    val isBlockedBy = bool("is_blocked_by").nullable()
    val chatId = long("chat_id")
    val contact = text("contact").nullable()
}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<User>(UserTable, "Telegram user info") {

        fun findByUpdateOrCreate(update: Update): User {
            val from = if (update.hasMessage()) update.message.from else update.callbackQuery.from
            val chatId = if (update.hasMessage()) update.message.chatId else update.callbackQuery.message.chatId
            return findByFromOrCreate(from, chatId, update.message?.contact?.phoneNumber)
        }

        fun findByFromOrCreate(
            from: TelegramUser,
            chatId: Long,
            contact: String? = null
        ): User {
            return findById(from.id)?.apply { this.isBlockedBy = false } ?: User.new(id = from.id) {
                this.chatId = chatId
                this.firstName = from.firstName
                this.isBot = from.isBot
                this.lastName = from.lastName
                this.userName = from.userName
                this.isBlockedBy = false
                this.contact = contact
            }
        }

        fun findAll(includeBlocked: Boolean = false): Iterable<User> {
            return if (includeBlocked) User.all() else User.find { UserTable.isBlockedBy eq false }
        }
    }

    var firstName by UserTable.firstName
    var lastName by UserTable.lastName
    var userName by UserTable.userName
    var isBot by UserTable.isBot
    var isBlockedBy by UserTable.isBlockedBy
    var chatId by UserTable.chatId
    var contact by UserTable.contact

    val items by Item via ItemSubscriptionTable

    override fun toString(): String {
        return "User(id = $id, chatId = $chatId, firstName = $firstName, isBot = $isBot, lastName = $lastName, userName = $userName, contact = $contact)"
    }
}

fun TelegramMessage.getUser(): User {
    return User.findByFromOrCreate(this.from, this.chatId)
}
