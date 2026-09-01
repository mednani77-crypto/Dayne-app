package com.example.services

import com.example.data.models.BackupData
import com.example.data.models.PartyType
import com.example.data.models.TransactionType

object BackupValidator {
    data class Result(val valid: Boolean, val reason: String? = null)

    fun validate(backup: BackupData): Result {
        if (backup.schemaVersion != 1) return Result(false, "Unsupported schema version")

        val currencyCodes = backup.currencies.map { it.code }
        if (currencyCodes.size != currencyCodes.toSet().size) return Result(false, "Duplicate currency code")
        if (backup.currencies.any { it.code.isBlank() || it.decimalPlaces !in 0..2 }) {
            return Result(false, "Invalid currency definition")
        }

        val partyIds = backup.parties.map { it.id }
        if (partyIds.size != partyIds.toSet().size) return Result(false, "Duplicate party id")
        if (backup.parties.any { it.id.isBlank() || it.name.isBlank() || !isPartyTypeValid(it.partyType) }) {
            return Result(false, "Invalid party")
        }

        val transactionIds = backup.transactions.map { it.id }
        if (transactionIds.size != transactionIds.toSet().size) return Result(false, "Duplicate transaction id")

        val partyIdSet = partyIds.toSet()
        val currencySet = currencyCodes.toSet()
        for (tx in backup.transactions) {
            if (tx.id.isBlank() || tx.amountMinor <= 0L) return Result(false, "Invalid transaction")
            if (tx.partyId !in partyIdSet) return Result(false, "Transaction references missing party")
            if (tx.currencyCode !in currencySet) return Result(false, "Transaction references missing currency")
            if (tx.currencyDecimalPlaces !in 0..2) return Result(false, "Invalid transaction precision")
            if (!isTransactionTypeValid(tx.transactionType)) return Result(false, "Invalid transaction type")
        }

        backup.settings?.let { settings ->
            if (settings.businessName.isBlank()) return Result(false, "Missing business name")
            if (settings.defaultCurrencyCode !in currencySet) return Result(false, "Missing default currency")
            if (settings.languageCode !in setOf("ar", "so", "am", "fr", "en")) return Result(false, "Invalid language")
        }

        return Result(true)
    }

    private fun isPartyTypeValid(value: String): Boolean =
        PartyType.entries.any { it.value == value }

    private fun isTransactionTypeValid(value: String): Boolean =
        TransactionType.entries.any { it.value == value }
}
