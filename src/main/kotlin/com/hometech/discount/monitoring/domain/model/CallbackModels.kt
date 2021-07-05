package com.hometech.discount.monitoring.domain.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SubscriptionForSize::class, name = "SIZE_SUBSCRIPTION"),
    JsonSubTypes.Type(value = UnsubscribeItem::class, name = "UNSUBSCRIBE_ITEM")
)
sealed class BaseButtonCallback(val type: CallbackType)

data class SubscriptionForSize(
    val sizeName: String,
    val itemId: Long,
) : BaseButtonCallback(CallbackType.SIZE_SUBSCRIPTION)

data class UnsubscribeItem(
    val subscriptionId: Long
) : BaseButtonCallback(CallbackType.UNSUBSCRIBE_ITEM)

enum class CallbackType { SIZE_SUBSCRIPTION, UNSUBSCRIBE_ITEM }
