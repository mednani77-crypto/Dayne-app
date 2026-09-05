package com.example.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.data.local.entities.LedgerEntity
import com.example.data.models.StatementData
import java.io.File
import java.io.FileOutputStream

/** Professional statement template using the active ledger identity. */
object BrandedPdfStatementService {
    fun generate(
        context: Context,
        ledger: LedgerEntity,
        data: StatementData,
        accountType: StatementAccountType,
        language: AppLanguage
    ): File {
        val strings = AppStrings.get(language)
        val rtl = language.isRtl
        val document = PdfDocument()
        val width = 595
        val height = 842
        val margin = 32f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val regular = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var pageNo = 0
        var page: PdfDocument.Page
        var bitmap: Bitmap
        var canvas: Canvas
        val scale = 3f

        fun startPage(): Pair<PdfDocument.Page, Pair<Bitmap, Canvas>> {
            pageNo++
            val p = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
            val b = Bitmap.createBitmap((width * scale).toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(b).apply { scale(scale, scale); drawColor(Color.WHITE) }
            return p to (b to c)
        }

        fun drawHeader(c: Canvas): Float {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#075985")
            c.drawRoundRect(RectF(margin, margin, width - margin, margin + 82f), 10f, 10f, paint)
            val logo = ledger.logoPath?.let { try { BitmapFactory.decodeFile(it) } catch (_: Exception) { null } }
            val logoSize = 50f
            val logoLeft = if (rtl) width - margin - logoSize - 14f else margin + 14f
            if (logo != null) {
                c.drawBitmap(logo, null, RectF(logoLeft, margin + 16f, logoLeft + logoSize, margin + 66f), null)
                logo.recycle()
            }
            val textX = if (rtl) (if (ledger.logoPath != null) width - margin - logoSize - 24f else width - margin - 14f) else (if (ledger.logoPath != null) margin + logoSize + 24f else margin + 14f)
            paint.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
            paint.color = Color.WHITE
            paint.typeface = bold
            paint.textSize = 18f
            c.drawText(ledger.name, textX, margin + 30f, paint)
            paint.typeface = regular
            paint.textSize = 9.5f
            val identity = listOfNotNull(ledger.phone, ledger.address).joinToString(" • ")
            if (identity.isNotBlank()) c.drawText(identity.take(70), textX, margin + 48f, paint)
            val accountLabel = if (accountType == StatementAccountType.CUSTOMER) strings.typeCustomer else strings.typeSupplier
            c.drawText("${strings.statementTitle} • $accountLabel • ${data.currencyCode}", textX, margin + 65f, paint)
            return margin + 98f
        }

        fun drawInfo(c: Canvas, top: Float): Float {
            paint.color = Color.parseColor("#F0F9FF")
            c.drawRoundRect(RectF(margin, top, width - margin, top + 82f), 8f, 8f, paint)
            paint.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
            val x = if (rtl) width - margin - 12f else margin + 12f
            paint.color = Color.parseColor("#0C4A6E")
            paint.typeface = bold
            paint.textSize = 12f
            c.drawText("${strings.statementFor} ${data.party.name}", x, top + 20f, paint)
            paint.typeface = regular; paint.textSize = 9.5f; paint.color = Color.DKGRAY
            data.party.phone?.let { c.drawText("${strings.partyPhoneLabel}: $it", x, top + 36f, paint) }
            c.drawText("${strings.statementPeriod} ${DateFormatter.formatDate(data.fromTimestamp)} - ${DateFormatter.formatDate(data.toTimestamp)}", x, top + 52f, paint)
            c.drawText("${strings.statementInitialBalance} ${AmountFormatter.formatAmount(data.openingBalance, data.decimalPlaces, data.currencyCode)}", x, top + 68f, paint)
            paint.textAlign = if (rtl) Paint.Align.LEFT else Paint.Align.RIGHT
            val right = if (rtl) margin + 12f else width - margin - 12f
            paint.typeface = bold
            paint.color = if (data.closingBalance >= 0L) Color.parseColor("#166534") else Color.parseColor("#B91C1C")
            c.drawText("${strings.statementClosingBalance} ${AmountFormatter.formatAmount(data.closingBalance, data.decimalPlaces, data.currencyCode)}", right, top + 68f, paint)
            return top + 96f
        }

        fun tableHeader(c: Canvas, y: Float): Float {
            paint.color = Color.parseColor("#E0F2FE"); c.drawRect(margin, y, width - margin, y + 25f, paint)
            paint.color = Color.parseColor("#0C4A6E"); paint.typeface = bold; paint.textSize = 9f
            if (rtl) {
                paint.textAlign = Paint.Align.RIGHT; c.drawText(strings.dateLabel, width - margin - 7f, y + 16f, paint); c.drawText(strings.statementDescription, width - margin - 78f, y + 16f, paint)
                paint.textAlign = Paint.Align.LEFT; c.drawText(strings.statementDebtCol, margin + 190f, y + 16f, paint); c.drawText(strings.statementPaymentCol, margin + 105f, y + 16f, paint); c.drawText(strings.statementRunningBalanceCol, margin + 7f, y + 16f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT; c.drawText(strings.dateLabel, margin + 7f, y + 16f, paint); c.drawText(strings.statementDescription, margin + 78f, y + 16f, paint)
                paint.textAlign = Paint.Align.RIGHT; c.drawText(strings.statementDebtCol, width - margin - 190f, y + 16f, paint); c.drawText(strings.statementPaymentCol, width - margin - 105f, y + 16f, paint); c.drawText(strings.statementRunningBalanceCol, width - margin - 7f, y + 16f, paint)
            }
            return y + 29f
        }

        fun footer(c: Canvas) {
            paint.textAlign = Paint.Align.CENTER; paint.typeface = regular; paint.textSize = 8f; paint.color = Color.GRAY
            val note = ledger.footerNote?.takeIf { it.isNotBlank() } ?: strings.statementFooterText
            while (paint.textSize > 5.5f && paint.measureText("$note • $pageNo") > width - margin * 2) {
                paint.textSize -= 0.5f
            }
            c.drawText("$note • $pageNo", width / 2f, height - 18f, paint)
        }

        fun wrapText(value: String, maxWidth: Float): List<String> {
            if (value.isBlank()) return listOf("-")
            val lines = mutableListOf<String>()
            var current = ""
            value.split(Regex("\\s+")).forEach { word ->
                if (paint.measureText(word) > maxWidth) {
                    if (current.isNotBlank()) { lines += current; current = "" }
                    var fragment = ""
                    word.forEach { char ->
                        val candidate = fragment + char
                        if (fragment.isNotBlank() && paint.measureText(candidate) > maxWidth) {
                            lines += fragment
                            fragment = char.toString()
                        } else fragment = candidate
                    }
                    current = fragment
                } else {
                    val candidate = if (current.isBlank()) word else "$current $word"
                    if (current.isNotBlank() && paint.measureText(candidate) > maxWidth) {
                        lines += current
                        current = word
                    } else current = candidate
                }
            }
            if (current.isNotBlank()) lines += current
            return lines.ifEmpty { listOf("-") }
        }

        var started = startPage(); page = started.first; bitmap = started.second.first; canvas = started.second.second
        var y = tableHeader(canvas, drawInfo(canvas, drawHeader(canvas)))
        data.rows.forEachIndexed { index, row ->
            paint.typeface = regular; paint.textSize = 8.4f
            val noteLines = wrapText(row.transaction.note ?: "-", 120f)
            val rowHeight = maxOf(23f, 11f + noteLines.size * 10.5f)
            if (y + rowHeight > height - 45f) {
                footer(canvas); page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null); document.finishPage(page); bitmap.recycle()
                started = startPage(); page = started.first; bitmap = started.second.first; canvas = started.second.second
                y = tableHeader(canvas, drawHeader(canvas))
            }
            if (index % 2 == 1) { paint.color = Color.parseColor("#F8FAFC"); canvas.drawRect(margin, y, width - margin, y + rowHeight, paint) }
            paint.typeface = regular; paint.textSize = 8.4f; paint.color = Color.DKGRAY
            val date = DateFormatter.formatDate(row.transaction.occurredAt)
            val debt = row.debitAmount?.let { AmountFormatter.formatAmount(it, data.decimalPlaces) } ?: "-"
            val payment = row.creditAmount?.let { AmountFormatter.formatAmount(it, data.decimalPlaces) } ?: "-"
            val balance = AmountFormatter.formatAmount(row.runningBalance, data.decimalPlaces)
            if (rtl) {
                paint.textAlign = Paint.Align.RIGHT; canvas.drawText(date, width - margin - 7f, y + 15f, paint)
                noteLines.forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, width - margin - 78f, y + 15f + lineIndex * 10.5f, paint)
                }
                paint.textAlign = Paint.Align.LEFT; canvas.drawText(debt, margin + 190f, y + 15f, paint); canvas.drawText(payment, margin + 105f, y + 15f, paint); paint.typeface = bold; canvas.drawText(balance, margin + 7f, y + 15f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT; canvas.drawText(date, margin + 7f, y + 15f, paint)
                noteLines.forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, margin + 78f, y + 15f + lineIndex * 10.5f, paint)
                }
                paint.textAlign = Paint.Align.RIGHT; canvas.drawText(debt, width - margin - 190f, y + 15f, paint); canvas.drawText(payment, width - margin - 105f, y + 15f, paint); paint.typeface = bold; canvas.drawText(balance, width - margin - 7f, y + 15f, paint)
            }
            y += rowHeight
        }
        footer(canvas); page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null); document.finishPage(page); bitmap.recycle()
        val safe = data.party.name.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
        val file = File(context.cacheDir, "DeynBook_${safe}_${data.currencyCode}_${DateFormatter.formatForFileName(System.currentTimeMillis())}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }; document.close(); return file
    }
}
