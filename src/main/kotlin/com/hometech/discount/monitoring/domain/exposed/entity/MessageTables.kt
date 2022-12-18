package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import com.hometech.discount.monitoring.domain.exposed.extensions.enum
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object MessageTable : LongIdTable("messages") {
    val user = reference("user_id", UserTable)
    val message = text("message").nullable()
    val direction = enum<MessageDirection>("direction")
    val timestamp = datetime("timestamp")
}

class Message(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<Message>(MessageTable, "Messages")

    var user by User referencedOn MessageTable.user
    var message by MessageTable.message
    var direction by MessageTable.direction
    var timestamp by MessageTable.timestamp
}

enum class MessageDirection {
    INBOUND, OUTBOUND
}
