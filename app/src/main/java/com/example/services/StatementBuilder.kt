package com.example.services

import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.StatementData
import com.example.data.models.StatementRow
import com.example.data.models.TransactionType

enum class StatementAccountType {
    CUSTOMER,
    SUPPLIER
}

/**
 * Pure statement calculator. It intentionally isolates customer and supplier ledgers,
 * which is essential when one party is configured as BOTH.
 */
object StatementBuilder {
    fun build(
        party: PartyEntity,
        transactions: List<LedgerTransactionEntity>,
        accountType: StatementAccountType,
        currencyCode: String,
        decimalPlaces: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): StatementData {
        require(fromTimestamp <= toTimestamp) { "Invalid statement period" }

        val relevant = transactions
            .asSequence()
            .filter { it.currencyCode == currencyCode }
            .filter { belongsToAccount(TransactionType.from(it.transactionType), accountType) }
            .sortedWith(compareBy<LedgerTransactionEntity> { it.occurredAt }.thenBy { it.createdAt })
            .toList()

        var openingBalance = 0L
        relevant.filter { it.occurredAt < fromTimestamp }.forEach { tx ->
            openingBalance = applyToBalance(openingBalance, tx)
        }

        var runningBalance = openingBalance
        var periodDebts = 0L
        var periodPayments = 0L
        val rows = mutableListOf<StatementRow>()

        relevant
            .filter { it.occurredAt in fromTimestamp..toTimestamp }
            .forEach { tx ->
                val type = TransactionType.from(tx.transactionType)
                if (type.isPositiveImpact) {
                    periodDebts = Math.addExact(periodDebts, tx.amountMinor)
                    runningBalance = Math.addExact(runningBalance, tx.amountMinor)
                    rows += StatementRow(
                        transaction = tx,
                        debitAmount = tx.amountMinor,
                        creditAmount = null,
                        runningBalance = runningBalance
                    )
                } else {
                    periodPayments = Math.addExact(periodPayments, tx.amountMinor)
                    runningBalance = Math.subtractExact(runningBalance, tx.amountMinor)
                    rows += StatementRow(
                        transaction = tx,
                        debitAmount = null,
                        creditAmount = tx.amountMinor,
                        runningBalance = runningBalance
                    )
                }
            }

        return StatementData(
            party = party,
            currencyCode = currencyCode,
            decimalPlaces = decimalPlaces,
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp,
            openingBalance = openingBalance,
            periodTotalDebts = periodDebts,
            periodTotalPayments = periodPayments,
            closingBalance = runningBalance,
            rows = rows
        )
    }

    private fun belongsToAccount(type: TransactionType, accountType: StatementAccountType): Boolean {
        return when (accountType) {
            StatementAccountType.CUSTOMER -> type.isReceivableImpact
            StatementAccountType.SUPPLIER -> !type.isReceivableImpact
        }
    }

    private fun applyToBalance(current: Long, tx: LedgerTransactionEntity): Long {
        return if (TransactionType.from(tx.transactionType).isPositiveImpact) {
            Math.addExact(current, tx.amountMinor)
        } else {
            Math.subtractExact(current, tx.amountMinor)
        }
    }
}
