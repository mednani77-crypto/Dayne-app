package com.example.services

import com.example.core.formatting.DateFormatter
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.CollectionStatus
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionCalculatorTest {
    private val customer = PartyEntity(
        id = "customer-1",
        name = "Ahmed",
        normalizedName = "ahmed",
        partyType = PartyType.CUSTOMER.value,
        createdAt = 1L,
        updatedAt = 1L
    )
    private val djf = CurrencyEntity(
        code = "DJF",
        name = "Djiboutian Franc",
        decimalPlaces = 0,
        isEnabled = true
    )

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        occurredAt: Long,
        dueAt: Long? = null
    ) = LedgerTransactionEntity(
        id = id,
        partyId = customer.id,
        transactionType = type.value,
        amountMinor = amount,
        currencyCode = "DJF",
        currencyDecimalPlaces = 0,
        occurredAt = occurredAt,
        dueAt = dueAt,
        createdAt = occurredAt,
        updatedAt = occurredAt
    )

    @Test
    fun partialPaymentReducesOutstandingAndKeepsOverdueDebt() {
        val now = DateFormatter.startOfDay(1_788_200_000_000L)
        val dueYesterday = now - 86_400_000L
        val result = CollectionCalculator.calculate(
            parties = listOf(customer),
            transactions = listOf(
                tx("debt", TransactionType.CUSTOMER_DEBT, 10_000L, now - 5 * 86_400_000L, dueYesterday),
                tx("payment", TransactionType.CUSTOMER_PAYMENT, 3_000L, now - 2 * 86_400_000L)
            ),
            currencies = listOf(djf),
            now = now
        )

        assertEquals(1, result.size)
        assertEquals(7_000L, result.single().outstandingAmountMinor)
        assertEquals(CollectionStatus.OVERDUE, result.single().status)
        assertEquals(-1, result.single().dayDelta)
    }

    @Test
    fun paymentsAreAppliedOldestDebtFirst() {
        val now = DateFormatter.startOfDay(1_788_200_000_000L)
        val result = CollectionCalculator.calculate(
            parties = listOf(customer),
            transactions = listOf(
                tx("old", TransactionType.CUSTOMER_DEBT, 5_000L, now - 10 * 86_400_000L, now - 2 * 86_400_000L),
                tx("new", TransactionType.CUSTOMER_DEBT, 7_000L, now - 5 * 86_400_000L, now + 2 * 86_400_000L),
                tx("payment", TransactionType.CUSTOMER_PAYMENT, 6_000L, now - 1 * 86_400_000L)
            ),
            currencies = listOf(djf),
            now = now
        )

        assertEquals(1, result.size)
        assertEquals("new", result.single().debtTransactionId)
        assertEquals(6_000L, result.single().outstandingAmountMinor)
        assertEquals(CollectionStatus.DUE_SOON, result.single().status)
    }

    @Test
    fun debtsWithoutDueDateAreNotCollectionTasks() {
        val now = DateFormatter.startOfDay(1_788_200_000_000L)
        val result = CollectionCalculator.calculate(
            parties = listOf(customer),
            transactions = listOf(tx("debt", TransactionType.CUSTOMER_DEBT, 5_000L, now - 1000L, null)),
            currencies = listOf(djf),
            now = now
        )
        assertTrue(result.isEmpty())
    }
}
