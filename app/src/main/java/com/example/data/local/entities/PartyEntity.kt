package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parties",
    indices = [
        Index(value = ["normalizedName"]),
        Index(value = ["partyType"]),
        Index(value = ["isArchived"])
    ]
)
data class PartyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val phone: String? = null,
    val partyType: String, // "CUSTOMER", "SUPPLIER", "BOTH"
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
