package com.example.core.formatting

import java.util.Calendar
import java.util.TimeZone

/**
 * Lightweight Gregorian -> Ethiopian date conversion.
 * Ethiopian New Year is September 11, or September 12 in the Gregorian year
 * immediately before a Gregorian leap year.
 */
object EthiopianCalendar {
    data class EthiopianDate(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String = "%04d/%02d/%02d".format(year, month, day)
    }

    fun fromTimestamp(timestamp: Long): EthiopianDate {
        val utc = TimeZone.getTimeZone("UTC")
        val source = Calendar.getInstance(utc).apply { timeInMillis = timestamp }
        val gYear = source.get(Calendar.YEAR)
        val gMonth = source.get(Calendar.MONTH) + 1
        val gDay = source.get(Calendar.DAY_OF_MONTH)

        val thisNewYearDay = newYearDayInSeptember(gYear)
        val afterNewYear = gMonth > 9 || (gMonth == 9 && gDay >= thisNewYearDay)
        val newYearGregorian = if (afterNewYear) gYear else gYear - 1
        val ethiopianYear = newYearGregorian - 7

        val start = Calendar.getInstance(utc).apply {
            clear()
            set(newYearGregorian, Calendar.SEPTEMBER, newYearDayInSeptember(newYearGregorian), 0, 0, 0)
        }
        val current = Calendar.getInstance(utc).apply {
            clear()
            set(gYear, gMonth - 1, gDay, 0, 0, 0)
        }
        val days = ((current.timeInMillis - start.timeInMillis) / 86_400_000L).toInt().coerceAtLeast(0)
        val month = if (days < 360) (days / 30) + 1 else 13
        val day = if (days < 360) (days % 30) + 1 else (days - 360) + 1
        return EthiopianDate(ethiopianYear, month, day)
    }

    private fun newYearDayInSeptember(gregorianYear: Int): Int =
        if (isGregorianLeapYear(gregorianYear + 1)) 12 else 11

    private fun isGregorianLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
