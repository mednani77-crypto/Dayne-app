package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.data.models.PartyType
import kotlinx.coroutines.launch

fun MainViewModel.createPartyWithCallback(
    name: String,
    phone: String?,
    partyType: PartyType,
    notes: String?,
    openingType: String,
    openingAmountMinor: Long,
    currencyCode: String,
    currencyDecimalPlaces: Int,
    onCreated: (String) -> Unit
) {
    viewModelScope.launch {
        if (name.trim().isBlank() || openingAmountMinor < 0L) return@launch
        val id = repository.createPartyWithOptionalOpeningBalance(
            name = name,
            phone = phone,
            partyType = partyType,
            notes = notes,
            openingBalanceType = openingType,
            openingAmountMinor = openingAmountMinor,
            currencyCode = currencyCode,
            currencyDecimalPlaces = currencyDecimalPlaces
        )
        onCreated(id)
    }
}
