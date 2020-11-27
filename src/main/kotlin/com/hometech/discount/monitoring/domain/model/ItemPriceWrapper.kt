package com.hometech.discount.monitoring.domain.model

import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.PriceLog

data class ItemPriceWrapper(val item: Item, val priceChange: PriceLog?)
