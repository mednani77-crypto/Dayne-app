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
import com.example.services.ImageShareService
import com.example.services.LocalizedCsvService
import com.example.services.ReportExportOptions
import com.example.services.ReportExportResult
import com.example.services.ReportTranslationException
import com.example.services.ReportTranslationService
import com.example.services.ShareHelper
import com.example.services.StatementAccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.shareStatementPdfV12(
    partyId: String,
    currencyCode: String,
    accountType: StatementAccountType,
    options: ReportExportOptions,
    onResult: (ReportExportResult) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val context = getApplication<Application>()
            val db = AppDatabase.getInstance(context)
            val v12 = DeynBookV12Repository(db)
            val party = repository.getPartyWithBalancesById(partyId)?.party
                ?: return@launch onResult(ReportExportResult.EXPORT_FAILED)
            val data = buildStatementData(
                partyId,
                currencyCode,
                accountType,
                party.createdAt,
                System.currentTimeMillis()
            ) ?: return@launch onResult(ReportExportResult.EXPORT_FAILED)
            val (ledger, translatedData) = ReportTranslationService(context).translateStatement(
                v12.getActiveLedger(), data, options
            )
            val file = withContext(Dispatchers.IO) {
                BrandedPdfStatementService.generate(
                    context,
                    ledger,
                    translatedData,
                    accountType,
                    options.targetLanguage
                )
            }
            ShareHelper.shareFile(context, file, "application/pdf", "DeynBook")
            onResult(ReportExportResult.SUCCESS)
        } catch (error: ReportTranslationException) {
            onResult(error.result)
        } catch (_: Exception) {
            onResult(ReportExportResult.EXPORT_FAILED)
        }
    }
}

fun MainViewModel.shareImageCardTranslatedV12(
    partyId: String,
    currencyCode: String,
    accountType: StatementAccountType,
    options: ReportExportOptions,
    onResult: (ReportExportResult) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val context = getApplication<Application>()
            val v12 = DeynBookV12Repository(AppDatabase.getInstance(context))
            val partyWithBalances = repository.getPartyWithBalancesById(partyId)
                ?: return@launch onResult(ReportExportResult.EXPORT_FAILED)
            val decimals = repository.getCurrencyByCode(currencyCode).decimalPlaces
            val balance = partyWithBalances.getBalanceForCurrency(currencyCode)
                ?: com.example.data.models.PartyCurrencyBalance(currencyCode, decimals)
            val recent = repository.getTransactionsForPartyFlow(partyId).first()
                .filter { it.currencyCode == currencyCode }
                .filter {
                    val receivable = com.example.data.models.TransactionType.from(it.transactionType).isReceivableImpact
                    if (accountType == StatementAccountType.CUSTOMER) receivable else !receivable
                }
                .sortedByDescending { it.occurredAt }
                .take(5)
            val translated = ReportTranslationService(context).translateExportData(
                v12.getActiveLedger(), listOf(partyWithBalances.party), recent, options
            )
            val file = withContext(Dispatchers.IO) {
                ImageShareService.generateSummaryCardImage(
                    context = context,
                    businessName = translated.first.name,
                    party = translated.second.first(),
                    balance = balance,
                    accountType = accountType,
                    recentTransactions = translated.third,
                    language = options.targetLanguage
                )
            }
            ShareHelper.shareFile(context, file, "image/png", "DeynBook")
            onResult(ReportExportResult.SUCCESS)
        } catch (error: ReportTranslationException) {
            onResult(error.result)
        } catch (_: Exception) {
            onResult(ReportExportResult.EXPORT_FAILED)
        }
    }
}

fun MainViewModel.shareTextSummaryTranslatedV12(
    partyId: String,
    currencyCode: String,
    accountType: StatementAccountType,
    options: ReportExportOptions,
    onResult: (ReportExportResult) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val context = getApplication<Application>()
            val v12 = DeynBookV12Repository(AppDatabase.getInstance(context))
            val partyWithBalances = repository.getPartyWithBalancesById(partyId)
                ?: return@launch onResult(ReportExportResult.EXPORT_FAILED)
            val decimals = repository.getCurrencyByCode(currencyCode).decimalPlaces
            val balance = partyWithBalances.getBalanceForCurrency(currencyCode)
                ?: com.example.data.models.PartyCurrencyBalance(currencyCode, decimals)
            val translated = ReportTranslationService(context).translateExportData(
                v12.getActiveLedger(), listOf(partyWithBalances.party), emptyList(), options
            )
            ShareHelper.shareTextSummary(
                context = context,
                businessName = translated.first.name,
                party = translated.second.first(),
                balance = balance,
                accountType = accountType,
                language = options.targetLanguage
            )
            onResult(ReportExportResult.SUCCESS)
        } catch (error: ReportTranslationException) {
            onResult(error.result)
        } catch (_: Exception) {
            onResult(ReportExportResult.EXPORT_FAILED)
        }
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
    options: ReportExportOptions,
    fromTime: Long = Long.MIN_VALUE,
    toTime: Long = Long.MAX_VALUE,
    onResult: (ReportExportResult) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val context = getApplication<Application>()
            val v12 = DeynBookV12Repository(AppDatabase.getInstance(context))
            val ledger = v12.getActiveLedger()
            val parties = v12.getScopedParties()
            val transactions = v12.getScopedTransactions().filter {
                it.occurredAt in minOf(fromTime, toTime)..maxOf(fromTime, toTime)
            }
            val translated = ReportTranslationService(context).translateExportData(
                ledger, parties, transactions, options
            )
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                    ExcelWorkbookService.writeTransactionsWorkbook(
                        output,
                        translated.first,
                        translated.second,
                        translated.third,
                        options.targetLanguage
                    )
                } ?: error("Cannot open output")
            }
            onResult(ReportExportResult.SUCCESS)
        } catch (error: ReportTranslationException) {
            onResult(error.result)
        } catch (_: Exception) {
            onResult(ReportExportResult.EXPORT_FAILED)
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

/** Reports CSV with the selected date range, active ledger only. */
fun MainViewModel.requestScopedCsvExport(
    context: Context,
    options: ReportExportOptions,
    fromTime: Long,
    toTime: Long,
    onResult: (ReportExportResult) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val v12 = DeynBookV12Repository(AppDatabase.getInstance(context.applicationContext))
            val originalLedger = v12.getActiveLedger()
            val originalParties = v12.getScopedParties()
            val start = minOf(fromTime, toTime)
            val end = maxOf(fromTime, toTime)
            val originalTxs = v12.getScopedTransactions().filter { it.occurredAt in start..end }
            val (ledger, parties, txs) = ReportTranslationService(context).translateExportData(
                originalLedger, originalParties, originalTxs, options
            )
            val csv = LocalizedCsvService.generate(
                transactions = txs,
                partiesById = parties.associateBy { it.id },
                language = options.targetLanguage,
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
            onResult(ReportExportResult.SUCCESS)
        } catch (error: ReportTranslationException) {
            onResult(error.result)
        } catch (_: Exception) {
            onResult(ReportExportResult.EXPORT_FAILED)
        }
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
