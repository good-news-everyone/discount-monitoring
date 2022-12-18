package com.hometech.discount.monitoring.domain

class OutdatedItemException(url: String, cause: Throwable?) : RuntimeException("item with url $url is outdated", cause)
