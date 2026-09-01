package com.example.data.models

import com.example.data.local.entities.PartyEntity

enum class CollectionStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_SOON,
    UPCOMING
}

data class CollectionItem(
    val party: PartyEntity,
    val debtTransactionId: String,
    val currencyCode: String,
    val decimalPlaces: Int,
    val originalAmountMinor: Long,
    val outstandingAmountMinor: Long,
    val dueAt: Long,
    val status: CollectionStatus,
    val dayDelta: Int,
    val note: String? = null
)
