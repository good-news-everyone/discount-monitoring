package com.hometech.discount.monitoring.parser

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

class Product(val name: String, @JsonProperty("offers") val offer: Offer)
class Offer(val price: BigDecimal, val priceCurrency: String)
