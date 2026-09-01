package com.example.core.formatting

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow

object AmountFormatter {

    /**
     * Normalizes digits from Arabic-Indic (٠-٩) and Extended Arabic-Indic / Persian (۰-۹)
     * to standard ASCII (0-9) digits, and normalizes Arabic decimal comma / separator.
     */
    fun normalizeInputDigits(input: String): String {
        val sb = java.lang.StringBuilder()
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                // Arabic-Indic digits: ٠=0x0660, ٩=0x0669
                in '\u0660'..'\u0669' -> sb.append((ch - '\u0660' + '0'.code).toChar())
                // Extended Arabic-Indic digits (Persian/Urdu): ۰=0x06F0, ۹=0x06F9
                in '\u06F0'..'\u06F9' -> sb.append((ch - '\u06F0' + '0'.code).toChar())
                '.', ',', '٫', '،' -> sb.append('.')
                else -> {
                    // Ignore spaces or other non-numeric chars
                }
            }
        }
        return sb.toString()
    }

    /**
     * Converts a user input text amount to Integer Minor Units (Long).
     * Strictly avoids floating-point inaccuracies.
     * Examples:
     * - decimalPlaces=0, input "15000" -> 15000L
     * - decimalPlaces=2, input "12.50" -> 1250L
     * - decimalPlaces=2, input "12.5" -> 1250L
     * - decimalPlaces=2, input "12" -> 1200L
     * - decimalPlaces=2, input ".75" -> 75L
     */
    fun parseToMinorUnits(input: String, decimalPlaces: Int): Long? {
        val normalized = normalizeInputDigits(input)
        if (normalized.isBlank()) return null

        val parts = normalized.split('.')
        if (parts.size > 2) return null // multiple decimal points

        val integerPartStr = parts[0].ifEmpty { "0" }
        val whole = integerPartStr.toLongOrNull() ?: return null

        if (decimalPlaces == 0) {
            return whole
        }

        val fractionPartStr = if (parts.size == 2) parts[1] else ""
        // Take at most decimalPlaces characters, pad with zeros if less
        val adjustedFraction = if (fractionPartStr.length >= decimalPlaces) {
            fractionPartStr.substring(0, decimalPlaces)
        } else {
            fractionPartStr.padEnd(decimalPlaces, '0')
        }

        val fraction = adjustedFraction.toLongOrNull() ?: 0L
        val multiplier = 10.0.pow(decimalPlaces.toDouble()).toLong()

        return whole * multiplier + fraction
    }

    /**
     * Formats minor units (Long) back to a user-editable string for input fields.
     */
    fun formatToInputString(minorUnits: Long, decimalPlaces: Int): String {
        if (decimalPlaces == 0) return minorUnits.toString()
        val divisor = 10.0.pow(decimalPlaces.toDouble()).toLong()
        val whole = minorUnits / divisor
        val fraction = (minorUnits % divisor).toString().padStart(decimalPlaces, '0')
        return "$whole.$fraction"
    }

    /**
     * Formats minor units into formatted display string with thousand commas.
     * Example:
     * minorUnits = 15000, decimalPlaces = 0, currencyCode = "DJF" -> "15,000 DJF"
     * minorUnits = 1250, decimalPlaces = 2, currencyCode = "USD" -> "12.50 USD"
     */
    fun formatAmount(
        minorUnits: Long,
        decimalPlaces: Int,
        currencyCode: String? = null,
        includeSign: Boolean = false
    ): String {
        val isNegative = minorUnits < 0
        val absUnits = kotlin.math.abs(minorUnits)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val formattedNumber = if (decimalPlaces == 0) {
            val df = DecimalFormat("#,##0", symbols)
            df.format(absUnits)
        } else {
            val divisor = 10.0.pow(decimalPlaces.toDouble()).toLong()
            val whole = absUnits / divisor
            val fraction = (absUnits % divisor).toString().padStart(decimalPlaces, '0')
            val df = DecimalFormat("#,##0", symbols)
            "${df.format(whole)}.$fraction"
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
}
