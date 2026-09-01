package com.example.services

import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class StatementBuilderTest {
    private val party = PartyEntity(
        id = "both-party",
        name = "Ahmed",
        normalizedName = "ahmed",
        partyType = PartyType.BOTH.value,
        createdAt = 1L,
        updatedAt = 1L
    )

    private fun tx(id: String, type: TransactionType, amount: Long, time: Long) =
        LedgerTransactionEntity(
            id = id,
            partyId = party.id,
            transactionType = type.value,
            amountMinor = amount,
            currencyCode = "DJF",
            currencyDecimalPlaces = 0,
            occurredAt = time,
            createdAt = time,
            updatedAt = time
        )

    private val transactions = listOf(
        tx("c-open", TransactionType.CUSTOMER_DEBT, 10_000, 10),
        tx("s-open", TransactionType.SUPPLIER_DEBT, 7_000, 11),
        tx("c-pay", TransactionType.CUSTOMER_PAYMENT, 2_000, 20),
        tx("s-pay", TransactionType.SUPPLIER_PAYMENT, 3_000, 21)
    )

    @Test
    fun customerStatementContainsOnlyCustomerLedger() {
        val result = StatementBuilder.build(
            party = party,
            transactions = transactions,
            accountType = StatementAccountType.CUSTOMER,
            currencyCode = "DJF",
            decimalPlaces = 0,
            fromTimestamp = 0,
            toTimestamp = 100
        )
        assertEquals(2, result.rows.size)
        assertEquals(10_000L, result.periodTotalDebts)
        assertEquals(2_000L, result.periodTotalPayments)
        assertEquals(8_000L, result.closingBalance)
    }

    @Test
    fun supplierStatementContainsOnlySupplierLedger() {
        val result = StatementBuilder.build(
            party = party,
            transactions = transactions,
            accountType = StatementAccountType.SUPPLIER,
            currencyCode = "DJF",
            decimalPlaces = 0,
            fromTimestamp = 0,
            toTimestamp = 100
        )
        assertEquals(2, result.rows.size)
        assertEquals(7_000L, result.periodTotalDebts)
        assertEquals(3_000L, result.periodTotalPayments)
        assertEquals(4_000L, result.closingBalance)
    }

    @Test
    fun openingBalanceIsCalculatedFromOnlyMatchingAccount() {
        val customer = StatementBuilder.build(
            party = party,
            transactions = transactions,
            accountType = StatementAccountType.CUSTOMER,
            currencyCode = "DJF",
            decimalPlaces = 0,
            fromTimestamp = 15,
            toTimestamp = 100
        )
        val supplier = StatementBuilder.build(
            party = party,
            transactions = transactions,
            accountType = StatementAccountType.SUPPLIER,
            currencyCode = "DJF",
            decimalPlaces = 0,
            fromTimestamp = 15,
            toTimestamp = 100
        )
        assertEquals(10_000L, customer.openingBalance)
        assertEquals(7_000L, supplier.openingBalance)
    }
}
