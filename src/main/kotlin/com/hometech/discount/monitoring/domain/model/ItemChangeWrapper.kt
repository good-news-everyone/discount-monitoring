package com.hometech.discount.monitoring.domain.model

import com.hometech.discount.monitoring.domain.entity.AdditionalInfoLog
import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.domain.entity.PriceChange
import com.hometech.discount.monitoring.domain.entity.PriceLog

data class ItemChangeWrapper(val item: Item, val itemChange: ChangeWrapper?) {

    fun isItemChanged(): Boolean {
        return itemChange?.priceLog?.priceChange != PriceChange.NONE ||
            itemChange.additionalInfoLog.infoNow != itemChange.additionalInfoLog.infoBefore
    }
}

data class ChangeWrapper(val priceLog: PriceLog, val additionalInfoLog: AdditionalInfoLog)
