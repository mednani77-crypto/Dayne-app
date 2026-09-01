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
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import java.io.File
import java.io.FileOutputStream

object ImageShareService {

    fun generateSummaryCardImage(
        context: Context,
        businessName: String,
        party: PartyEntity,
        balance: PartyCurrencyBalance,
        recentTransactions: List<LedgerTransactionEntity>,
        language: AppLanguage
    ): File {
        val strings = AppStrings.get(language)
        val isRtl = language.isRtl

        val width = 800
        val height = 950
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Canvas
        canvas.drawColor(Color.parseColor("#F4FBF9"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val regular = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Header Background
        paint.color = Color.parseColor("#00695C")
        canvas.drawRoundRect(RectF(24f, 24f, width - 24f, 170f), 24f, 24f, paint)

        // Header Store Name
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

        // Date on Header right
        paint.textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        val dX = if (isRtl) 60f else width - 60f
        paint.textSize = 16f
        canvas.drawText(DateFormatter.formatDate(System.currentTimeMillis()), dX, 90f, paint)

        // Person Card Box
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(24f, 190f, width - 24f, 430f), 20f, 20f, paint)

        // Border
        paint.color = Color.parseColor("#E0E0E0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(24f, 190f, width - 24f, 430f), 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        // Person Name & Type
        paint.color = Color.parseColor("#212121")
        paint.typeface = bold
        paint.textSize = 28f
        paint.textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val pX = if (isRtl) width - 60f else 60f
        canvas.drawText(party.name, pX, 245f, paint)

        paint.typeface = regular
        paint.textSize = 18f
        paint.color = Color.parseColor("#757575")
        val partyTypeLabel = when (PartyType.from(party.partyType)) {
            PartyType.CUSTOMER -> strings.typeCustomer
            PartyType.SUPPLIER -> strings.typeSupplier
            PartyType.BOTH -> strings.typeBoth
        }
        val phoneStr = party.phone?.let { " • $it" } ?: ""
        canvas.drawText("$partyTypeLabel$phoneStr", pX, 280f, paint)

        // Big Balance Display
        val isCustomer = party.partyType == PartyType.CUSTOMER.value || party.partyType == PartyType.BOTH.value
        val balAmount = if (isCustomer) balance.customerBalance else balance.supplierBalance
        val statusText = if (isCustomer) balance.getCustomerStatusText(strings) else balance.getSupplierStatusText(strings)

        paint.typeface = bold
        paint.textSize = 34f
        paint.color = when {
            balAmount > 0 -> Color.parseColor("#00796B") // Standard Debt
            balAmount < 0 -> Color.parseColor("#D32F2F") // Credit/Advance
            else -> Color.parseColor("#388E3C")         // Settled
        }
        canvas.drawText(statusText, pX, 360f, paint)

        // Recent 5 Transactions Box Header
        paint.color = Color.parseColor("#37474F")
        paint.typeface = bold
        paint.textSize = 20f
        canvas.drawText("${strings.recentTransactions} (5)", pX, 475f, paint)

        // Recent transactions rows
        val take5 = recentTransactions.take(5)
        var rowY = 515f
        val rHeight = 65f

        for (tx in take5) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(24f, rowY, width - 24f, rowY + rHeight - 10f), 12f, 12f, paint)

            val type = TransactionType.from(tx.transactionType)
            val isDebit = type.isPositiveImpact
            val signStr = if (isDebit) "+" else "-"
            val amountStr = "$signStr${AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode)}"
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
            paint.color = if (isDebit) Color.parseColor("#00796B") else Color.parseColor("#E65100")
            val valX = if (isRtl) 60f else width - 60f
            canvas.drawText(amountStr, valX, rowY + 34f, paint)

            rowY += rHeight
        }

        // Footer Branding
        paint.color = Color.parseColor("#78909C")
        paint.typeface = regular
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(strings.statementFooterText, width / 2f, height - 35f, paint)

        // Save Bitmap to Cache
        val safeName = party.name.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
        val file = File(context.cacheDir, "DeynBook_Card_${safeName}_${balance.currencyCode}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        return file
    }
}
