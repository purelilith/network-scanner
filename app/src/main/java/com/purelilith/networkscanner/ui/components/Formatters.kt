package com.purelilith.networkscanner.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val sameYearFormat by lazy { SimpleDateFormat("d MMMM, HH:mm", Locale("ru")) }
private val otherYearFormat by lazy { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")) }

fun formatDateTime(timestamp: Long): String {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val timestampYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
    val format = if (currentYear == timestampYear) sameYearFormat else otherYearFormat
    return format.format(Date(timestamp))
}

fun devicesCountText(count: Int): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    val word = when {
        lastTwoDigits in 11..14 -> "устройств"
        lastDigit == 1 -> "устройство"
        lastDigit in 2..4 -> "устройства"
        else -> "устройств"
    }
    return "$count $word"
}