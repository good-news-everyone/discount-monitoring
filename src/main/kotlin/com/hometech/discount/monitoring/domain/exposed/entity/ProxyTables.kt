package com.hometech.discount.monitoring.domain.exposed.entity

import com.hometech.discount.monitoring.domain.exposed.extensions.NamedEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object ProxyTable : LongIdTable("proxies") {
    val ip = text("ip_address")
    val port = integer("port")
    val timestamp = datetime("timestamp")
}

class Proxy(id: EntityID<Long>) : LongEntity(id) {
    companion object : NamedEntityClass<Proxy>(ProxyTable, "Proxies")

    var ip by ProxyTable.ip
    var port by ProxyTable.port
    var timestamp by ProxyTable.timestamp
}
