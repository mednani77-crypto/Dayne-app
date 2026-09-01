package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A completely separate debt book inside DeynBook. Parties belong to exactly one ledger,
 * and transactions inherit that isolation through their party.
 */
@Entity(
    tableName = "ledgers",
    indices = [Index(value = ["isArchived"]), Index(value = ["updatedAt"])]
)
data class LedgerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val countryCode: String = "DJ",
    val defaultCurrencyCode: String = "DJF",
    val logoPath: String? = null,
    val footerNote: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
