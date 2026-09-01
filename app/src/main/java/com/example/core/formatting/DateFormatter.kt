package com.example.core.formatting

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {
    fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    fun formatTime(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    fun formatForFileName(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    fun formatIsoUtc(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timestamp))
    }

    fun startOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
