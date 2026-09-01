package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_transactions",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["partyId"]),
        Index(value = ["partyId", "currencyCode", "occurredAt"]),
        Index(value = ["occurredAt"]),
        Index(value = ["transactionType"])
    ]
)
data class LedgerTransactionEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val transactionType: String,
    val amountMinor: Long,
    val currencyCode: String,
    val currencyDecimalPlaces: Int,
    val occurredAt: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
