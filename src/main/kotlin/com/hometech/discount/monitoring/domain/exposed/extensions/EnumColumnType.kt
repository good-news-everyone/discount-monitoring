package com.hometech.discount.monitoring.domain.exposed.extensions

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

inline fun <reified T : Enum<T>> Table.enum(name: String): Column<T> {
    return enumerationByName(name, 50, T::class)
}
