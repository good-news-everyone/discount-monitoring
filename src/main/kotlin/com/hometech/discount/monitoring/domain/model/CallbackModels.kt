package com.hometech.discount.monitoring.domain.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SubscriptionForSize::class, name = SIZE_SUBSCRIPTION.toString()),
    JsonSubTypes.Type(value = UnsubscribeItem::class, name = UNSUBSCRIBE_ITEM.toString())
)
sealed class BaseButtonCallback(val type: Int)

data class SubscriptionForSize(
    val size: String,
    val id: Long,
) : BaseButtonCallback(SIZE_SUBSCRIPTION)

data class UnsubscribeItem(
    val id: Long
) : BaseButtonCallback(UNSUBSCRIBE_ITEM)

const val SIZE_SUBSCRIPTION = 1
const val UNSUBSCRIBE_ITEM = 2
