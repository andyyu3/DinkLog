package com.dinklog.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.formatMatchDate(): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return formatter.format(Date(this))
}

fun Long.formatRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        days < 30L -> "${days / 7} weeks ago"
        days < 365L -> "${days / 30} months ago"
        else -> formatMatchDate()
    }
}
