package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.data.models.PartyType
import com.example.data.models.TransactionReportSummary
import com.example.data.models.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun MainViewModel.createPartyWithCallback(
    name: String,
    phone: String?,
    partyType: PartyType,
    notes: String?,
    openingType: String,
    openingAmountMinor: Long,
    currencyCode: String,
    currencyDecimalPlaces: Int,
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
            currencyDecimalPlaces = currencyDecimalPlaces
        )
        onCreated(id)
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
