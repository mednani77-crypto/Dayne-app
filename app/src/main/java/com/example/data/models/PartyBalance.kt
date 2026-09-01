package com.example.data.models

import com.example.core.formatting.AmountFormatter
import com.example.core.localization.Strings
import com.example.data.local.entities.LedgerTransactionEntity

enum class PartyType(val value: String) {
    CUSTOMER("CUSTOMER"),
    SUPPLIER("SUPPLIER"),
    BOTH("BOTH");

    companion object {
        fun from(value: String): PartyType = entries.find { it.value.equals(value, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class TransactionType(val value: String, val isReceivableImpact: Boolean, val isPositiveImpact: Boolean) {
    CUSTOMER_DEBT("CUSTOMER_DEBT", isReceivableImpact = true, isPositiveImpact = true),
    CUSTOMER_PAYMENT("CUSTOMER_PAYMENT", isReceivableImpact = true, isPositiveImpact = false),
    SUPPLIER_DEBT("SUPPLIER_DEBT", isReceivableImpact = false, isPositiveImpact = true),
    SUPPLIER_PAYMENT("SUPPLIER_PAYMENT", isReceivableImpact = false, isPositiveImpact = false),
    OPENING_RECEIVABLE("OPENING_RECEIVABLE", isReceivableImpact = true, isPositiveImpact = true),
    OPENING_PAYABLE("OPENING_PAYABLE", isReceivableImpact = false, isPositiveImpact = true);

    companion object {
        fun from(value: String): TransactionType = entries.find { it.value.equals(value, ignoreCase = true) } ?: CUSTOMER_DEBT
    }
}

/**
 * Balance details for a single currency of a party.
 * Customer Balance = (CUSTOMER_DEBT + OPENING_RECEIVABLE) - CUSTOMER_PAYMENT
 * Supplier Balance = (SUPPLIER_DEBT + OPENING_PAYABLE) - SUPPLIER_PAYMENT
 */
data class PartyCurrencyBalance(
    val currencyCode: String,
    val decimalPlaces: Int,
    val customerTotalDebt: Long = 0L,
    val customerTotalPaid: Long = 0L,
    val customerBalance: Long = 0L, // > 0: owes us, = 0: settled, < 0: customer credit (overpaid)

    val supplierTotalDebt: Long = 0L,
    val supplierTotalPaid: Long = 0L,
    val supplierBalance: Long = 0L  // > 0: we owe him, = 0: settled, < 0: advance payment made to supplier
) {
    fun getCustomerStatusText(strings: Strings): String {
        return when {
            customerBalance > 0 -> "${strings.statusCustomerOwes} ${AmountFormatter.formatAmount(customerBalance, decimalPlaces, currencyCode)}"
            customerBalance == 0L -> strings.statusSettled
            else -> "${strings.statusCustomerCredit} ${AmountFormatter.formatAmount(-customerBalance, decimalPlaces, currencyCode)}"
        }
    }

    fun getSupplierStatusText(strings: Strings): String {
        return when {
            supplierBalance > 0 -> "${strings.statusSupplierOwesUs} ${AmountFormatter.formatAmount(supplierBalance, decimalPlaces, currencyCode)}"
            supplierBalance == 0L -> strings.statusSettled
            else -> "${strings.statusSupplierAdvance} ${AmountFormatter.formatAmount(-supplierBalance, decimalPlaces, currencyCode)}"
        }
    }

    companion object {
        fun calculateFromTransactions(
            currencyCode: String,
            decimalPlaces: Int,
            transactions: List<LedgerTransactionEntity>
        ): PartyCurrencyBalance {
            var customerDebt = 0L
            var customerPaid = 0L
            var supplierDebt = 0L
            var supplierPaid = 0L

            for (tx in transactions) {
                if (tx.currencyCode != currencyCode) continue
                when (TransactionType.from(tx.transactionType)) {
                    TransactionType.CUSTOMER_DEBT, TransactionType.OPENING_RECEIVABLE -> customerDebt += tx.amountMinor
                    TransactionType.CUSTOMER_PAYMENT -> customerPaid += tx.amountMinor
                    TransactionType.SUPPLIER_DEBT, TransactionType.OPENING_PAYABLE -> supplierDebt += tx.amountMinor
                    TransactionType.SUPPLIER_PAYMENT -> supplierPaid += tx.amountMinor
                }
            }

            return PartyCurrencyBalance(
                currencyCode = currencyCode,
                decimalPlaces = decimalPlaces,
                customerTotalDebt = customerDebt,
                customerTotalPaid = customerPaid,
                customerBalance = customerDebt - customerPaid,
                supplierTotalDebt = supplierDebt,
                supplierTotalPaid = supplierPaid,
                supplierBalance = supplierDebt - supplierPaid
            )
        }
    }
}
