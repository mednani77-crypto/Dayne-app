package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.LedgerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerTransactionDao {
    @Query("SELECT * FROM ledger_transactions ORDER BY occurredAt DESC, createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions ORDER BY occurredAt DESC, createdAt DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE partyId = :partyId ORDER BY occurredAt DESC, createdAt DESC")
    fun getTransactionsByPartyIdFlow(partyId: String): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE partyId = :partyId ORDER BY occurredAt ASC, createdAt ASC")
    suspend fun getChronologicalTransactionsForParty(partyId: String): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE partyId = :partyId AND currencyCode = :currencyCode ORDER BY occurredAt ASC, createdAt ASC")
    suspend fun getChronologicalTransactionsForPartyAndCurrency(partyId: String, currencyCode: String): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE partyId = :partyId AND currencyCode = :currencyCode AND occurredAt BETWEEN :fromTime AND :toTime ORDER BY occurredAt ASC, createdAt ASC")
    suspend fun getTransactionsForPartyPeriod(partyId: String, currencyCode: String, fromTime: Long, toTime: Long): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE partyId = :partyId AND currencyCode = :currencyCode AND occurredAt < :beforeTime")
    suspend fun getTransactionsBeforeTime(partyId: String, currencyCode: String, beforeTime: Long): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): LedgerTransactionEntity?

    @Query("SELECT * FROM ledger_transactions")
    suspend fun getAllTransactions(): List<LedgerTransactionEntity>

    @Query("SELECT COUNT(*) FROM ledger_transactions WHERE partyId = :partyId")
    suspend fun getTransactionCountForParty(partyId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LedgerTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LedgerTransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: LedgerTransactionEntity)

    @Query("DELETE FROM ledger_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM ledger_transactions")
    suspend fun clearTransactions()
}
