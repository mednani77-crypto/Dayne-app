package com.example.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyCurrencyBalance
import com.example.data.models.TransactionType
import java.io.File
import java.io.FileOutputStream

object ImageShareService {

    fun generateSummaryCardImage(
        context: Context,
        businessName: String,
        party: PartyEntity,
        balance: PartyCurrencyBalance,
        accountType: StatementAccountType,
        recentTransactions: List<LedgerTransactionEntity>,
        language: AppLanguage
    ): File {
        val strings = AppStrings.get(language)
        val isRtl = language.isRtl

        val width = 800
        val height = 950
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F4FBF9"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val regular = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        paint.color = Color.parseColor("#00695C")
        canvas.drawRoundRect(RectF(24f, 24f, width - 24f, 170f), 24f, 24f, paint)

        paint.color = Color.WHITE
        paint.typeface = bold
        paint.textSize = 28f
        paint.textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val hX = if (isRtl) width - 60f else 60f
        val displayStore = if (businessName.isNotBlank()) businessName else strings.appName
        canvas.drawText(displayStore, hX, 80f, paint)

        paint.typeface = regular
        paint.textSize = 18f
        paint.color = Color.parseColor("#B2DFDB")
        canvas.drawText("${strings.statementTitle} • ${balance.currencyCode}", hX, 120f, paint)

        paint.textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        val dX = if (isRtl) 60f else width - 60f
        paint.textSize = 16f
        canvas.drawText(DateFormatter.formatDate(System.currentTimeMillis()), dX, 90f, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(24f, 190f, width - 24f, 430f), 20f, 20f, paint)

        paint.color = Color.parseColor("#E0E0E0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(24f, 190f, width - 24f, 430f), 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#212121")
        paint.typeface = bold
        paint.textSize = 28f
        paint.textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val pX = if (isRtl) width - 60f else 60f
        canvas.drawText(party.name, pX, 245f, paint)

        paint.typeface = regular
        paint.textSize = 18f
        paint.color = Color.parseColor("#757575")
        val accountTypeLabel = when (accountType) {
            StatementAccountType.CUSTOMER -> strings.typeCustomer
            StatementAccountType.SUPPLIER -> strings.typeSupplier
        }
        val phoneStr = party.phone?.let { " • $it" } ?: ""
        canvas.drawText("$accountTypeLabel$phoneStr", pX, 280f, paint)

        val balAmount = when (accountType) {
            StatementAccountType.CUSTOMER -> balance.customerBalance
            StatementAccountType.SUPPLIER -> balance.supplierBalance
        }
        val statusText = when (accountType) {
            StatementAccountType.CUSTOMER -> balance.getCustomerStatusText(strings)
            StatementAccountType.SUPPLIER -> balance.getSupplierStatusText(strings)
        }

        paint.typeface = bold
        paint.textSize = 34f
        paint.color = when {
            balAmount > 0 -> Color.parseColor("#00796B")
            balAmount < 0 -> Color.parseColor("#D32F2F")
            else -> Color.parseColor("#388E3C")
        }
        canvas.drawText(statusText, pX, 360f, paint)

        paint.color = Color.parseColor("#37474F")
        paint.typeface = bold
        paint.textSize = 20f
        canvas.drawText("${strings.recentTransactions} (${recentTransactions.take(5).size})", pX, 475f, paint)

        var rowY = 515f
        val rowHeight = 65f
        for (tx in recentTransactions.take(5)) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(24f, rowY, width - 24f, rowY + rowHeight - 10f), 12f, 12f, paint)

            val type = TransactionType.from(tx.transactionType)
            val isDebt = type.isPositiveImpact
            val amountStr = (if (isDebt) "+" else "-") +
                AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode)
            val dateStr = DateFormatter.formatDate(tx.occurredAt)
            val noteStr = tx.note?.take(22) ?: when (type) {
                TransactionType.CUSTOMER_DEBT -> strings.quickActionCustomerDebt
                TransactionType.CUSTOMER_PAYMENT -> strings.quickActionCustomerPayment
                TransactionType.SUPPLIER_DEBT -> strings.quickActionSupplierDebt
                TransactionType.SUPPLIER_PAYMENT -> strings.quickActionSupplierPayment
                TransactionType.OPENING_RECEIVABLE -> strings.txOpeningReceivable
                TransactionType.OPENING_PAYABLE -> strings.txOpeningPayable
            }

            paint.typeface = regular
            paint.textSize = 17f
            paint.color = Color.parseColor("#424242")
            paint.textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            canvas.drawText("$dateStr  -  $noteStr", pX, rowY + 34f, paint)

            paint.typeface = bold
            paint.textSize = 18f
            paint.textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
            paint.color = if (isDebt) Color.parseColor("#00796B") else Color.parseColor("#E65100")
            val valueX = if (isRtl) 60f else width - 60f
            canvas.drawText(amountStr, valueX, rowY + 34f, paint)

            rowY += rowHeight
        }

        paint.color = Color.parseColor("#78909C")
        paint.typeface = regular
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(strings.statementFooterText, width / 2f, height - 35f, paint)

        val safeName = party.name.replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_")
        val suffix = if (accountType == StatementAccountType.CUSTOMER) "customer" else "supplier"
        val file = File(context.cacheDir, "DeynBook_Card_${safeName}_${suffix}_${balance.currencyCode}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }
        bitmap.recycle()
        return file
    }
}
