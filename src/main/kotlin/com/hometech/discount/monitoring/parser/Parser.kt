package com.hometech.discount.monitoring.parser

import com.hometech.discount.monitoring.domain.model.ItemInfo

interface Parser {
    fun getType(): ParserType
    fun getItemInfo(url: String): ItemInfo
}
