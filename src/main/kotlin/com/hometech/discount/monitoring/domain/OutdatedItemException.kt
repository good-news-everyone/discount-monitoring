package com.hometech.discount.monitoring.domain

class OutdatedItemException(url: String) : RuntimeException("item with url $url is outdated")
