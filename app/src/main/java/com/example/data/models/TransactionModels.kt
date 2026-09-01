package com.example.data.models

import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity

data class TransactionWithParty(
    val transaction: LedgerTransactionEntity,
    val partyName: String,
    val partyPhone: String?,
    val partyType: String
)

data class TransactionReportSummary(
    val currencyCode: String,
    val decimalPlaces: Int,
    val totalCustomerReceivableBalance: Long,
    val totalSupplierPayableBalance: Long,
    val newCustomerDebtsInPeriod: Long,
    val customerPaymentsInPeriod: Long,
    val newSupplierDebtsInPeriod: Long,
    val supplierPaymentsInPeriod: Long,
    val totalCustomerCredits: Long,
    val totalSupplierAdvances: Long,
    val topDebtorCustomers: List<Pair<PartyEntity, Long>>,
    val topCreditorSuppliers: List<Pair<PartyEntity, Long>>,
    val totalTransactionCount: Int
)

data class StatementRow(
    val transaction: LedgerTransactionEntity,
    val debitAmount: Long?, // Debt added (+)
    val creditAmount: Long?, // Payment made (-)
    val runningBalance: Long // Cumulative balance up to this point
)

data class StatementData(
    val party: PartyEntity,
    val currencyCode: String,
    val decimalPlaces: Int,
    val fromTimestamp: Long,
    val toTimestamp: Long,
    val openingBalance: Long,
    val periodTotalDebts: Long,
    val periodTotalPayments: Long,
    val closingBalance: Long,
    val rows: List<StatementRow>
)
