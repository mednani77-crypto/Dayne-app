package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.data.local.AppDatabase
import com.example.data.local.entities.TransactionAttachmentEntity
import com.example.data.repository.DeynBookV12Repository
import com.example.data.repository.ExcelImportCoordinator
import com.example.data.repository.ExcelImportResult
import com.example.services.AttachmentService
import com.example.services.BrandedPdfStatementService
import com.example.services.ExcelWorkbookService
import com.example.services.LocalizedCsvService
import com.example.services.ShareHelper
import com.example.services.StatementAccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.shareStatementPdfV12(
    partyId: String,
    currencyCode: String,
    accountType: StatementAccountType,
    language: AppLanguage
) {
    viewModelScope.launch {
        val db = AppDatabase.getInstance(getApplication())
        val v12 = DeynBookV12Repository(db)
        val party = repository.getPartyWithBalancesById(partyId)?.party ?: return@launch
        val data = buildStatementData(
            partyId,
            currencyCode,
            accountType,
            party.createdAt,
            System.currentTimeMillis()
        ) ?: return@launch
        val file = BrandedPdfStatementService.generate(
            getApplication(),
            v12.getActiveLedger(),
            data,
            accountType,
            language
        )
        ShareHelper.shareFile(getApplication(), file, "application/pdf", "DeynBook")
    }
}

fun MainViewModel.addAttachmentV12(
    transactionId: String,
    uri: Uri,
    onDone: (Boolean) -> Unit = {}
) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        val stored = withContext(Dispatchers.IO) { AttachmentService.copyWithMetadata(context, uri) }
        if (stored == null) {
            onDone(false)
            return@launch
        }
        try {
            DeynBookV12Repository(AppDatabase.getInstance(context)).addAttachment(
                transactionId,
                stored.path,
                stored.displayName,
                stored.mimeType,
                stored.sizeBytes
            )
            onDone(true)
        } catch (_: Exception) {
            AttachmentService.deletePath(stored.path)
            onDone(false)
        }
    }
}

fun MainViewModel.deleteAttachmentV12(attachment: TransactionAttachmentEntity) {
    viewModelScope.launch {
        DeynBookV12Repository(AppDatabase.getInstance(getApplication())).deleteAttachment(attachment.id)
        AttachmentService.deletePath(attachment.filePath)
    }
}

fun MainViewModel.exportExcelV12(
    uri: Uri,
    language: AppLanguage,
    fromTime: Long = Long.MIN_VALUE,
    toTime: Long = Long.MAX_VALUE
) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        val v12 = DeynBookV12Repository(AppDatabase.getInstance(context))
        val ledger = v12.getActiveLedger()
        val parties = v12.getScopedParties()
        val transactions = v12.getScopedTransactions().filter {
            it.occurredAt in minOf(fromTime, toTime)..maxOf(fromTime, toTime)
        }
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                ExcelWorkbookService.writeTransactionsWorkbook(
                    output,
                    ledger,
                    parties,
                    transactions,
                    language
                )
            }
        }
    }
}

fun MainViewModel.exportExcelImportTemplate(uri: Uri, language: AppLanguage) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "w")?.use {
                ExcelWorkbookService.writeImportTemplate(it, language)
            }
        }
    }
}

fun MainViewModel.importExcelV12(uri: Uri, onResult: (ExcelImportResult?) -> Unit) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        try {
            val parsed = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use {
                    ExcelWorkbookService.parseImportWorkbook(it)
                } ?: error("Cannot open Excel file")
            }
            val result = ExcelImportCoordinator(AppDatabase.getInstance(context)).importRows(parsed)
            onResult(result)
        } catch (_: Exception) {
            onResult(null)
        }
    }
}

/** Settings all-time CSV: active ledger only, localized to the current app language. */
fun MainViewModel.exportCsvToUriV12(
    language: AppLanguage,
    launchDestination: (String, (Uri) -> Unit) -> Unit
) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        val v12 = DeynBookV12Repository(AppDatabase.getInstance(context))
        val ledger = v12.getActiveLedger()
        val parties = v12.getScopedParties()
        val txs = v12.getScopedTransactions()
        val csv = LocalizedCsvService.generate(
            transactions = txs,
            partiesById = parties.associateBy { it.id },
            language = language
        )
        val file = withContext(Dispatchers.IO) {
            File(
                context.cacheDir,
                "DeynBook_${ledger.name.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")}_all_${DateFormatter.formatForFileName(System.currentTimeMillis())}.csv"
            ).apply { writeText(csv, Charsets.UTF_8) }
        }
        ShareHelper.shareFile(context, file, "text/csv", "DeynBook CSV")
        launchDestination(file.name) { }
    }
}

/** Reports CSV with the selected date range, active ledger only. */
fun MainViewModel.requestScopedCsvExport(
    context: Context,
    language: AppLanguage,
    fromTime: Long,
    toTime: Long
) {
    viewModelScope.launch {
        val v12 = DeynBookV12Repository(AppDatabase.getInstance(context.applicationContext))
        val ledger = v12.getActiveLedger()
        val parties = v12.getScopedParties()
        val start = minOf(fromTime, toTime)
        val end = maxOf(fromTime, toTime)
        val txs = v12.getScopedTransactions().filter { it.occurredAt in start..end }
        val csv = LocalizedCsvService.generate(
            transactions = txs,
            partiesById = parties.associateBy { it.id },
            language = language,
            fromTimestamp = start,
            toTimestamp = end
        )
        val file = withContext(Dispatchers.IO) {
            File(
                context.cacheDir,
                "DeynBook_${ledger.name.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")}_${DateFormatter.formatForFileName(start)}_to_${DateFormatter.formatForFileName(end)}.csv"
            ).apply { writeText(csv, Charsets.UTF_8) }
        }
        ShareHelper.shareFile(context, file, "text/csv", "DeynBook CSV")
    }
}

fun MainViewModel.resetEverythingV12(onDone: () -> Unit = {}) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        AttachmentService.clearAll(context)
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.transactionAttachmentDao().clear()
            db.ledgerTransactionDao().clearTransactions()
            db.partyDao().clearParties()
            db.ledgerDao().clear()
            db.currencyDao().clearCurrencies()
            db.settingsDao().clearSettings()
            db.currencyDao().insertCurrencies(AppDatabase.DEFAULT_CURRENCIES)
            db.settingsDao().insertOrUpdate(AppDatabase.DEFAULT_SETTINGS)
            db.ledgerDao().insert(
                AppDatabase.defaultLedgerFromSettings(AppDatabase.DEFAULT_SETTINGS)
            )
        }
        setSelectedCurrencyCode("DJF")
        onDone()
    }
}

fun MainViewModel.saveLedgerLogoV12(
    ledgerId: String,
    uri: Uri?,
    onDone: (Boolean) -> Unit = {}
) {
    viewModelScope.launch {
        val context = getApplication<Application>()
        val db = AppDatabase.getInstance(context)
        val repo = DeynBookV12Repository(db)
        val ledger = repo.getActiveLedger().takeIf { it.id == ledgerId }
            ?: db.ledgerDao().getById(ledgerId)
            ?: return@launch
        if (uri == null) {
            AttachmentService.deletePath(ledger.logoPath)
            repo.setLedgerLogo(ledgerId, null)
            onDone(true)
            return@launch
        }
        val newPath = withContext(Dispatchers.IO) { AttachmentService.copyLogo(context, uri) }
        if (newPath == null) {
            onDone(false)
            return@launch
        }
        val old = ledger.logoPath
        repo.setLedgerLogo(ledgerId, newPath)
        if (old != null && old != newPath) AttachmentService.deletePath(old)
        onDone(true)
    }
}
