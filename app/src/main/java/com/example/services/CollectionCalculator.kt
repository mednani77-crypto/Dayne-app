package com.example.services

import com.example.core.formatting.DateFormatter
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.CollectionItem
import com.example.data.models.CollectionStatus
import com.example.data.models.PartyType
import com.example.data.models.TransactionType

object CollectionCalculator {
    fun calculate(
        parties: List<PartyEntity>,
        transactions: List<LedgerTransactionEntity>,
        currencies: List<CurrencyEntity>,
        now: Long = System.currentTimeMillis()
    ): List<CollectionItem> {
        val activeCustomers = parties
            .filter { !it.isArchived }
            .filter {
                val type = PartyType.from(it.partyType)
                type == PartyType.CUSTOMER || type == PartyType.BOTH
            }
            .associateBy { it.id }
        val decimals = currencies.associate { it.code to it.decimalPlaces }
        val todayStart = DateFormatter.startOfDay(now)

        return transactions
            .filter { it.partyId in activeCustomers }
            .groupBy { it.partyId to it.currencyCode }
            .flatMap { (key, group) ->
                val party = activeCustomers.getValue(key.first)
                var unappliedPayments = group
                    .filter { TransactionType.from(it.transactionType) == TransactionType.CUSTOMER_PAYMENT }
                    .fold(0L) { acc, tx -> Math.addExact(acc, tx.amountMinor) }

                group
                    .filter {
                        val type = TransactionType.from(it.transactionType)
                        type == TransactionType.CUSTOMER_DEBT || type == TransactionType.OPENING_RECEIVABLE
                    }
                    .sortedWith(compareBy<LedgerTransactionEntity> { it.occurredAt }.thenBy { it.id })
                    .mapNotNull { debt ->
                        val applied = minOf(unappliedPayments, debt.amountMinor)
                        unappliedPayments -= applied
                        val outstanding = debt.amountMinor - applied
                        val dueAt = debt.dueAt
                        if (outstanding <= 0L || dueAt == null) return@mapNotNull null

                        val dueStart = DateFormatter.startOfDay(dueAt)
                        val dayDelta = ((dueStart - todayStart) / 86_400_000L).toInt()
                        val status = when {
                            dayDelta < 0 -> CollectionStatus.OVERDUE
                            dayDelta == 0 -> CollectionStatus.DUE_TODAY
                            dayDelta <= 7 -> CollectionStatus.DUE_SOON
                            else -> CollectionStatus.UPCOMING
                        }
                        CollectionItem(
                            party = party,
                            debtTransactionId = debt.id,
                            currencyCode = debt.currencyCode,
                            decimalPlaces = decimals[debt.currencyCode] ?: debt.currencyDecimalPlaces,
                            originalAmountMinor = debt.amountMinor,
                            outstandingAmountMinor = outstanding,
                            dueAt = dueAt,
                            status = status,
                            dayDelta = dayDelta,
                            note = debt.note
                        )
                    }
            }
            .sortedWith(
                compareBy<CollectionItem> {
                    when (it.status) {
                        CollectionStatus.OVERDUE -> 0
                        CollectionStatus.DUE_TODAY -> 1
                        CollectionStatus.DUE_SOON -> 2
                        CollectionStatus.UPCOMING -> 3
                    }
                }.thenBy { it.dueAt }.thenBy { it.party.name.lowercase() }
            )
    }
}
