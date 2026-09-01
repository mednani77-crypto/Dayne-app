package com.example.core.formatting

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object AmountFormatter {

    /**
     * Normalizes localized digits while preserving separators long enough for parsing.
     *
     * Supported digits:
     * - ASCII: 0-9
     * - Arabic-Indic: ٠-٩
     * - Extended Arabic-Indic: ۰-۹
     *
     * Supported decimal separators:
     * - .
     * - Arabic decimal separator: ٫
     * - comma when it is unambiguously being used as a decimal separator
     *
     * Grouping separators such as 15,000 / 15٬000 are removed safely.
     */
    fun normalizeInputDigits(input: String, decimalPlaces: Int = 0): String {
        val digitNormalized = buildString {
            for (ch in input.trim()) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in '\u0660'..'\u0669' -> append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                    in '\u06F0'..'\u06F9' -> append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
                    '.', ',', '٫', '،', '٬' -> append(ch)
                    ' ', '\u00A0', '\u202F' -> Unit // grouping spaces
                    else -> Unit
                }
            }
        }

        if (digitNormalized.isBlank()) return ""

        // Arabic decimal separator is always a decimal mark.
        if (digitNormalized.contains('٫')) {
            return digitNormalized
                .replace(",", "")
                .replace("،", "")
                .replace("٬", "")
                .replace('٫', '.')
        }

        // A dot is always treated as the decimal separator. Commas are grouping marks.
        if (digitNormalized.contains('.')) {
            return digitNormalized
                .replace(",", "")
                .replace("،", "")
                .replace("٬", "")
        }

        val withoutArabicGrouping = digitNormalized.replace("٬", "")
        val commaNormalized = withoutArabicGrouping.replace('،', ',')
        val commaCount = commaNormalized.count { it == ',' }

        if (commaCount == 1 && decimalPlaces > 0) {
            val commaIndex = commaNormalized.indexOf(',')
            val before = commaNormalized.substring(0, commaIndex)
            val after = commaNormalized.substring(commaIndex + 1)

            // 12,5 and 12,50 are decimal amounts for a 2-decimal currency.
            // 15,000 is a grouped integer, because the suffix exceeds decimalPlaces.
            if (before.all(Char::isDigit) && after.all(Char::isDigit) && after.isNotEmpty() && after.length <= decimalPlaces) {
                return "$before.$after"
            }
        }

        // Multiple commas, or a suffix larger than the currency precision, are grouping marks.
        return commaNormalized.replace(",", "")
    }

    /**
     * Converts a user-entered amount into integer minor units without floating-point math.
     * Returns null for malformed input, excessive decimal precision, negative values, or overflow.
     */
    fun parseToMinorUnits(input: String, decimalPlaces: Int): Long? {
        if (decimalPlaces !in 0..9) return null

        val normalized = normalizeInputDigits(input, decimalPlaces)
        if (normalized.isBlank()) return null

        val parts = normalized.split('.')
        if (parts.size > 2) return null

        val integerPartStr = parts[0].ifEmpty { "0" }
        if (!integerPartStr.all(Char::isDigit)) return null
        val whole = integerPartStr.toLongOrNull() ?: return null

        if (decimalPlaces == 0) {
            if (parts.size == 2 && parts[1].any { it != '0' }) return null
            return whole
        }

        val fractionPartStr = if (parts.size == 2) parts[1] else ""
        if (!fractionPartStr.all(Char::isDigit)) return null
        if (fractionPartStr.length > decimalPlaces) return null

        val adjustedFraction = fractionPartStr.padEnd(decimalPlaces, '0')
        val fraction = adjustedFraction.ifEmpty { "0" }.toLongOrNull() ?: return null
        val multiplier = powerOfTen(decimalPlaces) ?: return null

        return try {
            Math.addExact(Math.multiplyExact(whole, multiplier), fraction)
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Formats minor units back to a plain editable amount string. */
    fun formatToInputString(minorUnits: Long, decimalPlaces: Int): String {
        if (decimalPlaces == 0) return minorUnits.toString()
        val divisor = powerOfTen(decimalPlaces) ?: return minorUnits.toString()
        val whole = minorUnits / divisor
        val fraction = kotlin.math.abs(minorUnits % divisor).toString().padStart(decimalPlaces, '0')
        return "$whole.$fraction"
    }

    /** Formats minor units for display using a stable, locale-independent numeric representation. */
    fun formatAmount(
        minorUnits: Long,
        decimalPlaces: Int,
        currencyCode: String? = null,
        includeSign: Boolean = false
    ): String {
        val isNegative = minorUnits < 0
        val absUnits = if (minorUnits == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minorUnits)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val formattedNumber = if (decimalPlaces == 0) {
            DecimalFormat("#,##0", symbols).format(absUnits)
        } else {
            val divisor = powerOfTen(decimalPlaces) ?: 1L
            val whole = absUnits / divisor
            val fraction = (absUnits % divisor).toString().padStart(decimalPlaces, '0')
            "${DecimalFormat("#,##0", symbols).format(whole)}.$fraction"
        }

        val signPrefix = when {
            isNegative -> "-"
            includeSign && minorUnits > 0 -> "+"
            else -> ""
        }

        return if (currencyCode != null) {
            "$signPrefix$formattedNumber $currencyCode"
        } else {
            "$signPrefix$formattedNumber"
        }
    }

    private fun powerOfTen(exponent: Int): Long? {
        if (exponent !in 0..18) return null
        var value = 1L
        repeat(exponent) {
            value = try {
                Math.multiplyExact(value, 10L)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return value
    }
}
