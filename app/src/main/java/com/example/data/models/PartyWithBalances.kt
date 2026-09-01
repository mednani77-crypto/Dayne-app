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

data class DashboardSummary(
    val currencyCode: String,
    val decimalPlaces: Int,
    val totalReceivable: Long, // Sum of positive customer balances for this currency
    val totalPayable: Long,    // Sum of positive supplier balances for this currency
    val activeCustomerCount: Int,
    val activeSupplierCount: Int
)
