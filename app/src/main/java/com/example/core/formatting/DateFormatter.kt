package com.example.core.formatting

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {
    const val CALENDAR_GREGORIAN = "GREGORIAN"
    const val CALENDAR_ETHIOPIAN = "ETHIOPIAN"

    @Volatile
    private var calendarMode: String = CALENDAR_GREGORIAN

    fun setCalendarMode(mode: String) {
        calendarMode = if (mode == CALENDAR_ETHIOPIAN) CALENDAR_ETHIOPIAN else CALENDAR_GREGORIAN
    }

    fun getCalendarMode(): String = calendarMode

    fun formatDate(timestamp: Long): String =
        if (calendarMode == CALENDAR_ETHIOPIAN) {
            EthiopianCalendar.fromTimestamp(timestamp).toString()
        } else {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }

    fun formatDateTime(timestamp: Long): String =
        if (calendarMode == CALENDAR_ETHIOPIAN) {
            "${EthiopianCalendar.fromTimestamp(timestamp)} ${formatTime(timestamp)}"
        } else {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        }

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
