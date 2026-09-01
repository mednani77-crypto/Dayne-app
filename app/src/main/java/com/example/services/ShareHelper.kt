package com.example.services

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyCurrencyBalance
import java.io.File

object ShareHelper {

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareTextSummary(
        context: Context,
        businessName: String,
        party: PartyEntity,
        balance: PartyCurrencyBalance,
        language: AppLanguage
    ) {
        val strings = AppStrings.get(language)
        val isCustomer = party.partyType == "CUSTOMER" || party.partyType == "BOTH"
        val statusText = if (isCustomer) balance.getCustomerStatusText(strings) else balance.getSupplierStatusText(strings)

        val storeHeader = if (businessName.isNotBlank()) "$businessName\n" else ""
        val text = buildString {
            append(storeHeader)
            append("${strings.statementTitle}: ${party.name}\n")
            append("${strings.netBalance}: $statusText\n")
            append("${strings.statementGeneratedAt} ${DateFormatter.formatDate(System.currentTimeMillis())}\n")
            append("${strings.statementFooterText}")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(Intent.createChooser(intent, strings.shareTextSummary).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$cleanPhone")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Dialer not available on device
        }
    }
}
