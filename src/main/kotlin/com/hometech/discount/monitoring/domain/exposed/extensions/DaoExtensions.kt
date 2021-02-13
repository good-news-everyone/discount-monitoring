package com.hometech.discount.monitoring.domain.exposed.extensions

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SizedIterable

abstract class NamedEntityClass<out E : LongEntity>(
    table: IdTable<Long>,
    val name: String,
    entityType: Class<E>? = null,
) : LongEntityClass<E>(table, entityType)

fun <E : LongEntity> NamedEntityClass<E>.findOrException(ids: Iterable<Long>): SizedIterable<E> {
    val foundEntities = find { table.id.inList(ids) }
    if (foundEntities.count() != ids.count().toLong()) {
        val notFoundIds = ids - foundEntities.map { it.id.value }
        throw RuntimeException("Для элемента '$name' не найдены ID '$notFoundIds'")
    }
    return foundEntities
}

fun <E : LongEntity> NamedEntityClass<E>.findOrException(id: Long): E {
    return this.find { table.id.eq(id) }.firstOrNull() ?: throw RuntimeException("Для элемента '$name' не найден ID '$id'")
}
