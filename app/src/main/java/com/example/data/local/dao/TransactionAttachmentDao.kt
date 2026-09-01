package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.TransactionAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionAttachmentDao {
    @Query("SELECT * FROM transaction_attachments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    fun getForTransactionFlow(transactionId: String): Flow<List<TransactionAttachmentEntity>>

    @Query("SELECT * FROM transaction_attachments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    suspend fun getForTransaction(transactionId: String): List<TransactionAttachmentEntity>

    @Query("SELECT * FROM transaction_attachments ORDER BY createdAt ASC")
    suspend fun getAll(): List<TransactionAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: TransactionAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<TransactionAttachmentEntity>)

    @Query("DELETE FROM transaction_attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transaction_attachments WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: String)

    @Query("DELETE FROM transaction_attachments")
    suspend fun clear()
}
