package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_attachments",
    foreignKeys = [
        ForeignKey(
            entity = LedgerTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["createdAt"])]
)
data class TransactionAttachmentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val filePath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
