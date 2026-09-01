package com.example.core.formatting

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class EthiopianCalendarTest {
    private fun utcTimestamp(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.timeInMillis

    @Test
    fun ethiopianNewYearAfterLeapBoundaryIsCorrect() {
        val date = EthiopianCalendar.fromTimestamp(utcTimestamp(2023, 9, 12))
        assertEquals(2016, date.year)
        assertEquals(1, date.month)
        assertEquals(1, date.day)
    }

    @Test
    fun ethiopianNewYearNormalBoundaryIsCorrect() {
        val date = EthiopianCalendar.fromTimestamp(utcTimestamp(2024, 9, 11))
        assertEquals(2017, date.year)
        assertEquals(1, date.month)
        assertEquals(1, date.day)
    }

    @Test
    fun thirteenthMonthIsCalculated() {
        val date = EthiopianCalendar.fromTimestamp(utcTimestamp(2024, 9, 10))
        assertEquals(2016, date.year)
        assertEquals(13, date.month)
        assertEquals(5, date.day)
    }
}
