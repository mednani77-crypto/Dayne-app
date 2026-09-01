package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.formatting.AmountFormatter
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.services.ExcelWorkbookService
import java.util.Locale
import java.util.UUID

data class ExcelImportResult(
    val accountsCreated: Int,
    val transactionsCreated: Int,
    val warnings: List<String>
)

class ExcelImportCoordinator(private val database: AppDatabase) {
    suspend fun importRows(parsed: ExcelWorkbookService.ParsedImport): ExcelImportResult = database.withTransaction {
        val settings = database.settingsDao().getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        val ledger = database.ledgerDao().getById(settings.activeLedgerId)
            ?: AppDatabase.defaultLedgerFromSettings(settings)
        val partyDao = database.partyDao()
        val txDao = database.ledgerTransactionDao()
        val currencies = database.currencyDao().getAllCurrencies().associateBy { it.code.uppercase(Locale.US) }
        val existing = partyDao.getAllPartiesForLedger(ledger.id).toMutableList()
        val warnings = parsed.warnings.toMutableList()
        var accountCount = 0
        var txCount = 0

        parsed.rows.forEachIndexed { index, row ->
            val normalized = row.name.trim().lowercase(Locale.ROOT)
            val phone = row.phone?.trim()?.takeIf(String::isNotBlank)
            var party = existing.firstOrNull {
                it.normalizedName == normalized && (phone == null || it.phone == phone)
            }
            if (party == null) {
                val now = System.currentTimeMillis()
                party = PartyEntity(
                    id = UUID.randomUUID().toString(),
                    ledgerId = ledger.id,
                    name = row.name.trim(),
                    normalizedName = normalized,
                    phone = phone,
                    partyType = row.partyType.value,
                    notes = row.note,
                    createdAt = now,
                    updatedAt = now
                )
                partyDao.insertParty(party)
                existing += party
                accountCount++
            }

            val amountText = row.amountText
            if (!amountText.isNullOrBlank()) {
                val currencyCode = row.currencyCode?.uppercase(Locale.US)?.takeIf(currencies::containsKey)
                    ?: ledger.defaultCurrencyCode
                val currency = currencies[currencyCode]
                if (currency == null) {
                    warnings += "Row ${index + 2}: unknown currency $currencyCode"
                    return@forEachIndexed
                }
                val amountMinor = AmountFormatter.parseToMinorUnits(amountText, currency.decimalPlaces)
                if (amountMinor == null || amountMinor <= 0L) {
                    warnings += "Row ${index + 2}: invalid amount '$amountText'"
                    return@forEachIndexed
                }
                val txType = ExcelWorkbookService.inferTransactionType(row.direction, row.partyType, hasAmount = true)
                if (txType == null) {
                    warnings += "Row ${index + 2}: direction is required for an account that is both customer and supplier"
                    return@forEachIndexed
                }
                val now = System.currentTimeMillis()
                txDao.insertTransaction(
                    LedgerTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        partyId = party.id,
                        transactionType = txType.value,
                        amountMinor = amountMinor,
                        currencyCode = currencyCode,
                        currencyDecimalPlaces = currency.decimalPlaces,
                        occurredAt = now,
                        note = row.note,
                        dueAt = ExcelWorkbookService.parseDueDate(row.dueDateText),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                txCount++
            }
        }

        ExcelImportResult(accountCount, txCount, warnings)
    }
}
