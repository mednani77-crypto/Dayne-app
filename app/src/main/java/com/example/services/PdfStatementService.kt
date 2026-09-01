package com.example.services

import android.content.Context
import android.graphics.Bitmap
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
import com.example.data.models.StatementData
import java.io.File
import java.io.FileOutputStream

object PdfStatementService {

    fun generateStatementPdf(
        context: Context,
        businessName: String,
        statementData: StatementData,
        accountType: StatementAccountType,
        language: AppLanguage
    ): File {
        val strings = AppStrings.get(language)
        val isRtl = language.isRtl
        val accountLabel = when (accountType) {
            StatementAccountType.CUSTOMER -> strings.typeCustomer
            StatementAccountType.SUPPLIER -> strings.typeSupplier
        }

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val scale = 3f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var bitmap = Bitmap.createBitmap(
            (pageWidth * scale).toInt(),
            (pageHeight * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        var canvas = Canvas(bitmap).apply {
            scale(scale, scale)
            drawColor(Color.WHITE)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val boldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val regularTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var y: Float

        fun drawHeader(c: Canvas) {
            paint.color = Color.parseColor("#00695C")
            c.drawRoundRect(RectF(margin, margin, pageWidth - margin, margin + 65f), 8f, 8f, paint)

            paint.color = Color.WHITE
            paint.typeface = boldTypeface
            paint.textSize = 18f
            paint.textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            val titleX = if (isRtl) pageWidth - margin - 16f else margin + 16f
            val displayName = if (businessName.isNotBlank()) businessName else strings.appName
            c.drawText(displayName, titleX, margin + 28f, paint)

            paint.textSize = 12f
            paint.typeface = regularTypeface
            c.drawText("${strings.statementTitle} • $accountLabel • ${statementData.currencyCode}", titleX, margin + 50f, paint)

            paint.textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
            val dateX = if (isRtl) margin + 16f else pageWidth - margin - 16f
            c.drawText(DateFormatter.formatDate(System.currentTimeMillis()), dateX, margin + 38f, paint)
        }

        fun drawPartyInfoBox(c: Canvas, startY: Float): Float {
            val currY = startY + 14f

            paint.color = Color.parseColor("#F4FBF9")
            paint.style = Paint.Style.FILL
            c.drawRoundRect(RectF(margin, currY, pageWidth - margin, currY + 95f), 6f, 6f, paint)

            paint.color = Color.parseColor("#80CBC4")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            c.drawRoundRect(RectF(margin, currY, pageWidth - margin, currY + 95f), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#004D40")
            paint.typeface = boldTypeface
            paint.textSize = 13f
            paint.textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            val leftColX = if (isRtl) pageWidth - margin - 12f else margin + 12f
            c.drawText("${strings.statementFor} ${statementData.party.name}", leftColX, currY + 22f, paint)

            paint.typeface = regularTypeface
            paint.textSize = 10f
            paint.color = Color.parseColor("#37474F")
            statementData.party.phone?.let { phone ->
                c.drawText("${strings.partyPhoneLabel}: $phone", leftColX, currY + 40f, paint)
            }

            val periodText = "${DateFormatter.formatDate(statementData.fromTimestamp)} - ${DateFormatter.formatDate(statementData.toTimestamp)}"
            c.drawText("${strings.statementPeriod} $periodText", leftColX, currY + 58f, paint)

            paint.textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
            val rightColX = if (isRtl) margin + 12f else pageWidth - margin - 12f

            c.drawText(
                "${strings.statementInitialBalance} ${AmountFormatter.formatAmount(statementData.openingBalance, statementData.decimalPlaces, statementData.currencyCode)}",
                rightColX,
                currY + 22f,
                paint
            )
            c.drawText(
                "${strings.statementTotalDebts} ${AmountFormatter.formatAmount(statementData.periodTotalDebts, statementData.decimalPlaces, statementData.currencyCode)}",
                rightColX,
                currY + 40f,
                paint
            )
            c.drawText(
                "${strings.statementTotalPayments} ${AmountFormatter.formatAmount(statementData.periodTotalPayments, statementData.decimalPlaces, statementData.currencyCode)}",
                rightColX,
                currY + 58f,
                paint
            )

            paint.typeface = boldTypeface
            paint.textSize = 11f
            paint.color = if (statementData.closingBalance >= 0) Color.parseColor("#00796B") else Color.parseColor("#D32F2F")
            c.drawText(
                "${strings.statementClosingBalance} ${AmountFormatter.formatAmount(statementData.closingBalance, statementData.decimalPlaces, statementData.currencyCode)}",
                rightColX,
                currY + 80f,
                paint
            )

            return currY + 110f
        }

        fun drawTableHeader(c: Canvas, startY: Float): Float {
            val headerY = startY
            paint.color = Color.parseColor("#E0F2F1")
            c.drawRect(margin, headerY, pageWidth - margin, headerY + 24f, paint)

            paint.color = Color.parseColor("#004D40")
            paint.typeface = boldTypeface
            paint.textSize = 9.5f

            if (isRtl) {
                paint.textAlign = Paint.Align.RIGHT
                c.drawText(strings.dateLabel, pageWidth - margin - 8f, headerY + 16f, paint)
                c.drawText(strings.statementDescription, pageWidth - margin - 80f, headerY + 16f, paint)
                paint.textAlign = Paint.Align.LEFT
                c.drawText(strings.statementDebtCol, margin + 190f, headerY + 16f, paint)
                c.drawText(strings.statementPaymentCol, margin + 105f, headerY + 16f, paint)
                c.drawText(strings.statementRunningBalanceCol, margin + 8f, headerY + 16f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                c.drawText(strings.dateLabel, margin + 8f, headerY + 16f, paint)
                c.drawText(strings.statementDescription, margin + 80f, headerY + 16f, paint)
                paint.textAlign = Paint.Align.RIGHT
                c.drawText(strings.statementDebtCol, pageWidth - margin - 190f, headerY + 16f, paint)
                c.drawText(strings.statementPaymentCol, pageWidth - margin - 105f, headerY + 16f, paint)
                c.drawText(strings.statementRunningBalanceCol, pageWidth - margin - 8f, headerY + 16f, paint)
            }
            return headerY + 28f
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            paint.color = Color.parseColor("#78909C")
            paint.typeface = regularTypeface
            paint.textSize = 8.5f
            paint.textAlign = Paint.Align.CENTER
            c.drawText("${strings.statementFooterText} • $pageNum", pageWidth / 2f, pageHeight - margin + 15f, paint)
        }

        drawHeader(canvas)
        y = drawPartyInfoBox(canvas, margin + 70f)
        y = drawTableHeader(canvas, y)

        val rowHeight = 22f
        var rowIndex = 0

        for (row in statementData.rows) {
            if (y + rowHeight > pageHeight - margin - 30f) {
                drawFooter(canvas, pageNumber)
                page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()), null)
                document.finishPage(page)
                bitmap.recycle()

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                bitmap = Bitmap.createBitmap(
                    (pageWidth * scale).toInt(),
                    (pageHeight * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                canvas = Canvas(bitmap).apply {
                    scale(scale, scale)
                    drawColor(Color.WHITE)
                }
                drawHeader(canvas)
                y = drawTableHeader(canvas, margin + 75f)
            }

            if (rowIndex % 2 == 1) {
                paint.color = Color.parseColor("#F9FBFB")
                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, paint)
            }

            paint.color = Color.parseColor("#263238")
            paint.typeface = regularTypeface
            paint.textSize = 8.5f

            val dateStr = DateFormatter.formatDate(row.transaction.occurredAt)
            val noteStr = row.transaction.note ?: "-"
            val debitStr = row.debitAmount?.let { AmountFormatter.formatAmount(it, statementData.decimalPlaces) } ?: "-"
            val creditStr = row.creditAmount?.let { AmountFormatter.formatAmount(it, statementData.decimalPlaces) } ?: "-"
            val runningStr = AmountFormatter.formatAmount(row.runningBalance, statementData.decimalPlaces)
            val truncatedNote = if (noteStr.length > 25) noteStr.take(23) + ".." else noteStr

            if (isRtl) {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(dateStr, pageWidth - margin - 8f, y + 14f, paint)
                canvas.drawText(truncatedNote, pageWidth - margin - 80f, y + 14f, paint)

                paint.textAlign = Paint.Align.LEFT
                paint.color = if (row.debitAmount != null) Color.parseColor("#00796B") else Color.parseColor("#757575")
                canvas.drawText(debitStr, margin + 190f, y + 14f, paint)
                paint.color = if (row.creditAmount != null) Color.parseColor("#E65100") else Color.parseColor("#757575")
                canvas.drawText(creditStr, margin + 105f, y + 14f, paint)
                paint.color = Color.parseColor("#263238")
                paint.typeface = boldTypeface
                canvas.drawText(runningStr, margin + 8f, y + 14f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(dateStr, margin + 8f, y + 14f, paint)
                canvas.drawText(truncatedNote, margin + 80f, y + 14f, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = if (row.debitAmount != null) Color.parseColor("#00796B") else Color.parseColor("#757575")
                canvas.drawText(debitStr, pageWidth - margin - 190f, y + 14f, paint)
                paint.color = if (row.creditAmount != null) Color.parseColor("#E65100") else Color.parseColor("#757575")
                canvas.drawText(creditStr, pageWidth - margin - 105f, y + 14f, paint)
                paint.color = Color.parseColor("#263238")
                paint.typeface = boldTypeface
                canvas.drawText(runningStr, pageWidth - margin - 8f, y + 14f, paint)
            }

            y += rowHeight
            rowIndex++
        }

        drawFooter(canvas, pageNumber)
        page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()), null)
        document.finishPage(page)
        bitmap.recycle()

        val safeName = statementData.party.name.replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_")
        val suffix = if (accountType == StatementAccountType.CUSTOMER) "customer" else "supplier"
        val fileName = "DeynBook_${safeName}_${suffix}_${statementData.currencyCode}_${DateFormatter.formatForFileName(System.currentTimeMillis())}.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
            outputStream.flush()
        }
        document.close()

        return file
    }
}
