package com.example.core.formatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountFormatterTest {
    @Test
    fun groupedDarleFrancIsNotParsedAsFifteen() {
        assertEquals(15_000L, AmountFormatter.parseToMinorUnits("15,000", 0))
    }

    @Test
    fun groupedTwoDecimalCurrencyKeepsWholeAmount() {
        assertEquals(1_500_000L, AmountFormatter.parseToMinorUnits("15,000", 2))
    }

    @Test
    fun commaCanBeDecimalWhenUnambiguous() {
        assertEquals(1_250L, AmountFormatter.parseToMinorUnits("12,50", 2))
    }

    @Test
    fun arabicIndicAndArabicSeparatorsAreSupported() {
        assertEquals(15_000L, AmountFormatter.parseToMinorUnits("١٥٬٠٠٠", 0))
        assertEquals(1_250L, AmountFormatter.parseToMinorUnits("١٢٫٥٠", 2))
        assertEquals(123_456L, AmountFormatter.parseToMinorUnits("۱۲۳۴۵۶", 0))
    }

    @Test
    fun excessivePrecisionIsRejectedInsteadOfTruncated() {
        assertNull(AmountFormatter.parseToMinorUnits("12.345", 2))
    }

    @Test
    fun malformedAndOverflowingAmountsAreRejected() {
        assertNull(AmountFormatter.parseToMinorUnits("12.3.4", 2))
        assertNull(AmountFormatter.parseToMinorUnits("999999999999999999999999", 2))
    }
}
