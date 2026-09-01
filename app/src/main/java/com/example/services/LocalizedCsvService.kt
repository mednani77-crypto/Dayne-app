package com.example.services

import com.example.core.formatting.AmountFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Produces an Excel-compatible UTF-8 CSV export using the language selected in DeynBook.
 * Only the date range filters the export: all accounts, currencies and transaction types
 * inside the requested range are included.
 */
object LocalizedCsvService {

    fun generate(
        transactions: List<LedgerTransactionEntity>,
        partiesById: Map<String, PartyEntity>,
        language: AppLanguage,
        fromTimestamp: Long = Long.MIN_VALUE,
        toTimestamp: Long = Long.MAX_VALUE
    ): String {
        val strings = AppStrings.get(language)
        val labels = labelsFor(language)
        val rangeStart = minOf(fromTimestamp, toTimestamp)
        val rangeEnd = maxOf(fromTimestamp, toTimestamp)

        val rows = transactions
            .asSequence()
            .filter { it.occurredAt in rangeStart..rangeEnd }
            .sortedWith(compareBy<LedgerTransactionEntity> { it.occurredAt }.thenBy { it.id })
            .toList()

        return buildString {
            // UTF-8 BOM improves Arabic/Amharic/Somali/French display in Microsoft Excel.
            append('\uFEFF')
            appendCsvRow(
                listOf(
                    labels.transactionId,
                    strings.dateLabel,
                    strings.partyNameLabel,
                    strings.partyTypeLabel,
                    labels.transactionType,
                    strings.amountLabel,
                    strings.currencyLabel,
                    strings.noteLabel,
                    labels.createdAt,
                    labels.updatedAt
                )
            )

            rows.forEach { tx ->
                val party = partiesById[tx.partyId]
                appendCsvRow(
                    listOf(
                        tx.id,
                        formatDateTime(tx.occurredAt, language),
                        party?.name ?: labels.unknownParty,
                        localizedPartyType(party?.partyType, language),
                        localizedTransactionType(tx.transactionType, language),
                        AmountFormatter.formatToInputString(tx.amountMinor, tx.currencyDecimalPlaces),
                        tx.currencyCode,
                        tx.note.orEmpty(),
                        formatDateTime(tx.createdAt, language),
                        formatDateTime(tx.updatedAt, language)
                    )
                )
            }
        }
    }

    private fun StringBuilder.appendCsvRow(values: List<String>) {
        append(values.joinToString(",") { escapeCsv(it) })
        append('\n')
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun localizedPartyType(value: String?, language: AppLanguage): String {
        val strings = AppStrings.get(language)
        return when (value?.let(PartyType::from)) {
            PartyType.CUSTOMER -> strings.typeCustomer
            PartyType.SUPPLIER -> strings.typeSupplier
            PartyType.BOTH -> strings.typeBoth
            null -> labelsFor(language).unknownParty
        }
    }

    private fun localizedTransactionType(value: String, language: AppLanguage): String {
        val strings = AppStrings.get(language)
        return when (TransactionType.from(value)) {
            TransactionType.CUSTOMER_DEBT -> strings.txCustomerDebtTitle
            TransactionType.CUSTOMER_PAYMENT -> strings.txCustomerPaymentTitle
            TransactionType.SUPPLIER_DEBT -> strings.txSupplierDebtTitle
            TransactionType.SUPPLIER_PAYMENT -> strings.txSupplierPaymentTitle
            TransactionType.OPENING_RECEIVABLE -> strings.txOpeningReceivable
            TransactionType.OPENING_PAYABLE -> strings.txOpeningPayable
        }
    }

    private fun formatDateTime(timestamp: Long, language: AppLanguage): String {
        val locale = when (language) {
            AppLanguage.ARABIC -> Locale.forLanguageTag("ar")
            AppLanguage.SOMALI -> Locale.forLanguageTag("so")
            AppLanguage.AMHARIC -> Locale.forLanguageTag("am")
            AppLanguage.FRENCH -> Locale.FRENCH
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        return SimpleDateFormat("dd/MM/yyyy HH:mm", locale).format(Date(timestamp))
    }

    private fun labelsFor(language: AppLanguage): CsvLabels = when (language) {
        AppLanguage.ARABIC -> CsvLabels(
            transactionId = "معرّف العملية",
            transactionType = "نوع العملية",
            createdAt = "تاريخ الإنشاء",
            updatedAt = "آخر تعديل",
            unknownParty = "حساب غير معروف"
        )
        AppLanguage.SOMALI -> CsvLabels(
            transactionId = "Aqoonsiga dhaqdhaqaaqa",
            transactionType = "Nooca dhaqdhaqaaqa",
            createdAt = "La sameeyay",
            updatedAt = "La cusbooneysiiyay",
            unknownParty = "Xisaab aan la garanayn"
        )
        AppLanguage.AMHARIC -> CsvLabels(
            transactionId = "የግብይት መለያ",
            transactionType = "የግብይት ዓይነት",
            createdAt = "የተፈጠረበት ጊዜ",
            updatedAt = "የተሻሻለበት ጊዜ",
            unknownParty = "ያልታወቀ መለያ"
        )
        AppLanguage.FRENCH -> CsvLabels(
            transactionId = "ID de l’opération",
            transactionType = "Type d’opération",
            createdAt = "Créé le",
            updatedAt = "Modifié le",
            unknownParty = "Compte inconnu"
        )
        AppLanguage.ENGLISH -> CsvLabels(
            transactionId = "Transaction ID",
            transactionType = "Transaction Type",
            createdAt = "Created At",
            updatedAt = "Updated At",
            unknownParty = "Unknown account"
        )
    }

    private data class CsvLabels(
        val transactionId: String,
        val transactionType: String,
        val createdAt: String,
        val updatedAt: String,
        val unknownParty: String
    )
}
