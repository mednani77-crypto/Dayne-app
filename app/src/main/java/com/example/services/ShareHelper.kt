package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
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

        context.startActivity(
            Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun shareTextSummary(
        context: Context,
        businessName: String,
        party: PartyEntity,
        balance: PartyCurrencyBalance,
        accountType: StatementAccountType,
        language: AppLanguage
    ) {
        val strings = AppStrings.get(language)
        val statusText = when (accountType) {
            StatementAccountType.CUSTOMER -> balance.getCustomerStatusText(strings)
            StatementAccountType.SUPPLIER -> balance.getSupplierStatusText(strings)
        }
        val accountLabel = when (accountType) {
            StatementAccountType.CUSTOMER -> strings.typeCustomer
            StatementAccountType.SUPPLIER -> strings.typeSupplier
        }

        val text = buildString {
            if (businessName.isNotBlank()) append("$businessName\n")
            append("${strings.statementTitle}: ${party.name}\n")
            append("$accountLabel\n")
            append("${strings.netBalance}: $statusText\n")
            append("${strings.statementGeneratedAt} ${DateFormatter.formatDate(System.currentTimeMillis())}\n")
            append(strings.statementFooterText)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(
            Intent.createChooser(intent, strings.shareTextSummary).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** Opens a pre-filled WhatsApp chat. The user still presses Send manually. */
    fun openWhatsAppReminder(context: Context, phone: String?, text: String) {
        val cleanPhone = phone.orEmpty().filter(Char::isDigit)
        if (cleanPhone.isNotBlank()) {
            val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(text)}")
            val direct = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            try {
                context.startActivity(direct)
                return
            } catch (_: Exception) {
            }
        }

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(share, "WhatsApp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.fromParts("tel", cleanPhone, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // A dialer is not guaranteed to exist on tablets or managed devices.
        }
    }
}
