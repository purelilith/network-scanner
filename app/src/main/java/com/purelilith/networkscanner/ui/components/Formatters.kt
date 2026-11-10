package com.purelilith.networkscanner.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatDateTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val pattern = if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
        "d MMMM, HH:mm"
    } else {
        "d MMMM yyyy, HH:mm"
    }
    return SimpleDateFormat(pattern, Locale("ru")).format(Date(timestamp))
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