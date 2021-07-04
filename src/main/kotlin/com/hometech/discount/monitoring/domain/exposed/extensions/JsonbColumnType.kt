package com.hometech.discount.monitoring.domain.exposed.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger { }
private val objectMapper = jacksonObjectMapper().apply {
    this.findAndRegisterModules()
    this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    this.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
}

fun <T : Any> Table.jsonb(
    name: String,
    klass: KClass<T>,
    nullable: Boolean = false
): Column<T> {
    val type = JsonColumnType(
        klass = klass,
        jsonMapper = objectMapper,
        nullable = nullable
    )
    return registerColumn(
        name = name,
        type = type
    )
}

class JsonColumnType<out T : Any>(
    private val klass: KClass<T>,
    private val jsonMapper: ObjectMapper,
    override var nullable: Boolean
) : IColumnType {

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val obj = PGobject().apply {
            this.type = "jsonb"
            this.value = value as String?
        }
        stmt[index] = obj
    }

    override fun valueFromDB(value: Any): Any {
        if (value::class == klass) {
            return value
        }
        return when (value) {
            is HashMap<*, *> -> value
            is Map<*, *> -> value
            else -> {
                value as PGobject
                try {
                    val json = value.value
                    jsonMapper.readValue(json, klass.java)
                } catch (e: Exception) {
                    logger.error(e) { "Can't parse JSON" }
                    throw e
                }
            }
        }
    }

    override fun notNullValueToDB(value: Any): Any = jsonMapper.writeValueAsString(value)
    override fun nonNullValueToString(value: Any): String = "'${jsonMapper.writeValueAsString(value)}'"
    override fun sqlType() = "jsonb"
}

class JsonKey(val key: String) : Expression<String>() {
    init {
        if (!key.matches("[a-zA-Z]+".toRegex())) throw IllegalArgumentException("Only simple json key allowed.")
    }

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { append(key) }
}

inline fun <reified T> Column<Map<*, *>>.json(jsonKey: JsonKey): Function<T> {
    val columnType = when (T::class) {
        Int::class -> IntegerColumnType()
        String::class -> VarCharColumnType()
        Boolean::class -> BooleanColumnType()
        else -> throw java.lang.RuntimeException("Column type ${T::class} not supported for json field.")
    }

    return json(jsonKey, columnType)
}

fun <T> Column<Map<*, *>>.json(jsonKey: JsonKey, columnType: IColumnType): Function<T> {
    return JsonVal(
        expr = this,
        jsonKey = jsonKey,
        columnType = columnType
    )
}

private class JsonVal<T>(
    val expr: Expression<*>,
    val jsonKey: JsonKey,
    override val columnType: IColumnType
) : Function<T>(columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        queryBuilder {
            append("CAST((${expr.toQueryBuilder(queryBuilder)} ->> '${jsonKey.key}') AS ${columnType.sqlType()})")
        }
}
