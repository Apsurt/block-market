package com.apsurt.blockmarket.client.util

import kotlin.math.roundToInt

/**
 * Extension function to format large numbers with k/m/b suffixes.
 * Examples: 999 -> 999 | 1500 -> 1.5k | 1000000 -> 1m
 */
fun Long.formatKMB(): String {
    if (this < 1000) return this.toString()

    val value: Double
    val suffix: String

    when {
        this < 1_000_000L -> { value = this / 1000.0; suffix = "k" }
        this < 1_000_000_000L -> { value = this / 1_000_000.0; suffix = "m" }
        else -> { value = this / 1_000_000_000.0; suffix = "b" }
    }

    // Round to 1 decimal place safely
    val rounded = (value * 10.0).roundToInt() / 10.0

    // Drop the ".0" if it's a whole number (e.g., 1.0k -> 1k)
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toLong()}$suffix"
    } else {
        "$rounded$suffix"
    }
}

// Overload for Integers (like share amounts)
fun Int.formatKMB(): String = this.toLong().formatKMB()