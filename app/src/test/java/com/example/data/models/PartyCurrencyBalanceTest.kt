package com.example.data.models

import com.example.data.local.entities.LedgerTransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PartyCurrencyBalanceTest {
    private fun tx(type: TransactionType, amount: Long, currency: String = "DJF") =
        LedgerTransactionEntity(
            id = "${type.value}-$amount-$currency",
            partyId = "party-1",
            transactionType = type.value,
            amountMinor = amount,
            currencyCode = currency,
            currencyDecimalPlaces = if (currency == "USD") 2 else 0,
            occurredAt = 1L
        )

    @Test
    fun customerDebtAndPaymentProduceCorrectBalance() {
        val balance = PartyCurrencyBalance.calculateFromTransactions(
            "DJF",
            0,
            listOf(
                tx(TransactionType.CUSTOMER_DEBT, 15_000),
                tx(TransactionType.CUSTOMER_PAYMENT, 5_000)
            )
        )
        assertEquals(15_000L, balance.customerTotalDebt)
        assertEquals(5_000L, balance.customerTotalPaid)
        assertEquals(10_000L, balance.customerBalance)
    }

    @Test
    fun supplierDebtAndPaymentProduceCorrectBalance() {
        val balance = PartyCurrencyBalance.calculateFromTransactions(
            "DJF",
            0,
            listOf(
                tx(TransactionType.SUPPLIER_DEBT, 20_000),
                tx(TransactionType.SUPPLIER_PAYMENT, 8_000)
            )
        )
        assertEquals(12_000L, balance.supplierBalance)
    }

    @Test
    fun overpaymentsAndAdvancesRemainNegativeInsteadOfBeingClamped() {
        val customer = PartyCurrencyBalance.calculateFromTransactions(
            "DJF",
            0,
            listOf(
                tx(TransactionType.CUSTOMER_DEBT, 10_000),
                tx(TransactionType.CUSTOMER_PAYMENT, 12_000)
            )
        )
        val supplier = PartyCurrencyBalance.calculateFromTransactions(
            "DJF",
            0,
            listOf(
                tx(TransactionType.SUPPLIER_DEBT, 5_000),
                tx(TransactionType.SUPPLIER_PAYMENT, 8_000)
            )
        )
        assertEquals(-2_000L, customer.customerBalance)
        assertEquals(-3_000L, supplier.supplierBalance)
    }

    @Test
    fun differentCurrenciesNeverMix() {
        val transactions = listOf(
            tx(TransactionType.CUSTOMER_DEBT, 10_000, "DJF"),
            tx(TransactionType.CUSTOMER_DEBT, 2_000, "USD")
        )
        val djf = PartyCurrencyBalance.calculateFromTransactions("DJF", 0, transactions)
        val usd = PartyCurrencyBalance.calculateFromTransactions("USD", 2, transactions)
        assertEquals(10_000L, djf.customerBalance)
        assertEquals(2_000L, usd.customerBalance)
    }

    @Test
    fun customerAndSupplierLedgersRemainIndependentForBothParty() {
        val balance = PartyCurrencyBalance.calculateFromTransactions(
            "DJF",
            0,
            listOf(
                tx(TransactionType.CUSTOMER_DEBT, 10_000),
                tx(TransactionType.SUPPLIER_DEBT, 7_000)
            )
        )
        assertEquals(10_000L, balance.customerBalance)
        assertEquals(7_000L, balance.supplierBalance)
    }
}
