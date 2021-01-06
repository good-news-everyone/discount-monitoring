package com.hometech.discount.monitoring.domain.model

data class AdditionalInfo(val sizes: List<SizeInfo>? = null)

data class SizeInfo(
    val name: String,
    val availability: Boolean
)
