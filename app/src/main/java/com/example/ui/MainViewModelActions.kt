package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.core.localization.AppLanguage
import com.example.data.models.PartyType
import com.example.data.models.TransactionReportSummary
import com.example.data.models.TransactionType
import com.example.services.LocalizedCsvService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun MainViewModel.configureOnboardingCurrencies(primaryCurrencyCode: String) {
    viewModelScope.launch {
        val enabledCodes = if (primaryCurrencyCode == "USD") setOf("USD") else setOf(primaryCurrencyCode, "USD")
        repository.getAllCurrencies().forEach { currency ->
            repository.setCurrencyEnabled(currency.code, currency.code in enabledCodes)
        }
    }
}

fun MainViewModel.createPartyWithCallback(
    name: String,
    phone: String?,
    partyType: PartyType,
    notes: String?,
    openingType: String,
    openingAmountMinor: Long,
    currencyCode: String,
    currencyDecimalPlaces: Int,
    openingOccurredAt: Long,
    onCreated: (String) -> Unit
) {
    viewModelScope.launch {
        if (name.trim().isBlank() || openingAmountMinor < 0L) return@launch
        val id = repository.createPartyWithOptionalOpeningBalance(
            name = name,
            phone = phone,
            partyType = partyType,
            notes = notes,
            openingBalanceType = openingType,
            openingAmountMinor = openingAmountMinor,
            currencyCode = currencyCode,
            currencyDecimalPlaces = currencyDecimalPlaces,
            occurredAt = openingOccurredAt
        )
        onCreated(id)
    }
}

fun MainViewModel.exportBackupToUri(uri: Uri) {
    viewModelScope.launch {
        val backup = repository.exportBackupData()
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.writer(Charsets.UTF_8).buffered().use { writer ->
                    writer.write(backup.toJsonString())
                }
            }
        }
    }
}

/**
 * Exports every transaction inside the requested date range, regardless of account/currency,
 * using the same language currently selected by the user. The resulting UTF-8 CSV opens
 * correctly in Microsoft Excel and other spreadsheet applications.
 */
fun MainViewModel.exportCsvToUri(
    uri: Uri,
    language: AppLanguage,
    fromTime: Long = Long.MIN_VALUE,
    toTime: Long = Long.MAX_VALUE
) {
    viewModelScope.launch {
        val partyRows = repository.getAllPartiesWithBalancesFlow().first()
        val parties = partyRows.map { it.party }
        val transactions = partyRows.flatMap { item ->
            repository.getTransactionsForPartyFlow(item.party.id).first()
        }
        val csv = LocalizedCsvService.generate(
            transactions = transactions,
            partiesById = parties.associateBy { it.id },
            language = language,
            fromTimestamp = fromTime,
            toTimestamp = toTime
        )

        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.writer(Charsets.UTF_8).buffered().use { writer ->
                    writer.write(csv)
                }
            }
        }
    }
}

suspend fun MainViewModel.loadReportSummary(
    currencyCode: String,
    fromTime: Long,
    toTime: Long,
    partyId: String?
): TransactionReportSummary {
    if (partyId == null) return repository.getReportSummary(currencyCode, fromTime, toTime)

    val partyWithBalances = repository.getPartyWithBalancesById(partyId)
        ?: return repository.getReportSummary(currencyCode, fromTime, toTime)
    val party = partyWithBalances.party
    val partyType = PartyType.from(party.partyType)
    val currency = repository.getCurrencyByCode(currencyCode)
    val balance = partyWithBalances.getBalanceForCurrency(currencyCode)
    val txs = repository.getTransactionsForPartyFlow(partyId)
        .first()
        .filter { it.currencyCode == currencyCode && it.occurredAt in fromTime..toTime }

    var customerDebts = 0L
    var customerPayments = 0L
    var supplierDebts = 0L
    var supplierPayments = 0L

    txs.forEach { tx ->
        when (TransactionType.from(tx.transactionType)) {
            TransactionType.CUSTOMER_DEBT,
            TransactionType.OPENING_RECEIVABLE -> customerDebts = Math.addExact(customerDebts, tx.amountMinor)
            TransactionType.CUSTOMER_PAYMENT -> customerPayments = Math.addExact(customerPayments, tx.amountMinor)
            TransactionType.SUPPLIER_DEBT,
            TransactionType.OPENING_PAYABLE -> supplierDebts = Math.addExact(supplierDebts, tx.amountMinor)
            TransactionType.SUPPLIER_PAYMENT -> supplierPayments = Math.addExact(supplierPayments, tx.amountMinor)
        }
    }

    val customerBalance = if (partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH) {
        balance?.customerBalance ?: 0L
    } else 0L
    val supplierBalance = if (partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH) {
        balance?.supplierBalance ?: 0L
    } else 0L

    return TransactionReportSummary(
        currencyCode = currencyCode,
        decimalPlaces = currency.decimalPlaces,
        totalCustomerReceivableBalance = customerBalance.coerceAtLeast(0L),
        totalSupplierPayableBalance = supplierBalance.coerceAtLeast(0L),
        newCustomerDebtsInPeriod = customerDebts,
        customerPaymentsInPeriod = customerPayments,
        newSupplierDebtsInPeriod = supplierDebts,
        supplierPaymentsInPeriod = supplierPayments,
        totalCustomerCredits = (-customerBalance).coerceAtLeast(0L),
        totalSupplierAdvances = (-supplierBalance).coerceAtLeast(0L),
        topDebtorCustomers = if (customerBalance > 0L) listOf(party to customerBalance) else emptyList(),
        topCreditorSuppliers = if (supplierBalance > 0L) listOf(party to supplierBalance) else emptyList(),
        totalTransactionCount = txs.size
    )
}
