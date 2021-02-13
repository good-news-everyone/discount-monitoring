package com.hometech.discount.monitoring.helper

import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.streams.asSequence

fun randomString(size: Int = 20): String {
    val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return java.util.Random().ints(size.toLong(), 0, source.length)
        .asSequence()
        .map(source::get)
        .joinToString("")
}

fun randomInt(from: Int = Int.MIN_VALUE, to: Int = Int.MAX_VALUE): Int {
    return Random.nextInt(from, to)
}

fun randomLong(from: Long = Long.MIN_VALUE, to: Long = Long.MAX_VALUE): Long {
    return Random.nextLong(from, to)
}

fun randomBigDecimal(from: Int = Int.MIN_VALUE, to: Int = Int.MAX_VALUE): BigDecimal {
    return Random.nextInt(from, to).toBigDecimal()
}

fun BigDecimal.shouldBeEqualsIgnoreScale(other: BigDecimal) {
    this.setScale(10) shouldBe (other.setScale(10))
}
