package com.example.data.models

import com.example.data.local.entities.PartyEntity

data class PartyWithBalances(
    val party: PartyEntity,
    val balances: List<PartyCurrencyBalance>,
    val transactionCount: Int = 0,
    val lastTransactionTime: Long? = null
) {
    fun getBalanceForCurrency(currencyCode: String): PartyCurrencyBalance? {
        return balances.find { it.currencyCode == currencyCode }
    }
}

data class DashboardPartyAmount(
    val partyId: String,
    val name: String,
    val amountMinor: Long
)

data class DashboardSummary(
    val currencyCode: String,
    val decimalPlaces: Int,
    val totalReceivable: Long,
    val totalPayable: Long,
    val activeCustomerCount: Int,
    val activeSupplierCount: Int,
    /** Debt collection signals for DeynBook 1.2. */
    val overdueCount: Int = 0,
    val dueNext7DaysCount: Int = 0,
    val collectedThisMonth: Long = 0L,
    val paidToSuppliersThisMonth: Long = 0L,
    val topDebtors: List<DashboardPartyAmount> = emptyList()
)
