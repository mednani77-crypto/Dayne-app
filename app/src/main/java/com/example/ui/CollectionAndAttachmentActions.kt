package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.models.CollectionItem
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import com.example.services.AttachmentService
import com.example.services.CollectionCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

suspend fun MainViewModel.loadCollectionItems(now: Long = System.currentTimeMillis()): List<CollectionItem> {
    val partyRows = repository.getAllPartiesWithBalancesFlow().first()
    val parties = partyRows.map { it.party }
    val transactions = partyRows.flatMap { row ->
        repository.getTransactionsForPartyFlow(row.party.id).first()
    }
    return CollectionCalculator.calculate(
        parties = parties,
        transactions = transactions,
        currencies = repository.getAllCurrencies(),
        now = now
    )
}

fun MainViewModel.addTransactionWithExtras(
    partyId: String,
    type: TransactionType,
    amountMinor: Long,
    currencyCode: String,
    currencyDecimalPlaces: Int,
    occurredAt: Long,
    note: String?,
    dueAt: Long?,
    attachmentUri: Uri?
) {
    viewModelScope.launch {
        if (amountMinor <= 0L || !isCompatibleParty(partyId, type)) return@launch
        val context = getApplication<Application>()
        val attachmentPath = attachmentUri?.let { AttachmentService.copyToPrivateStorage(context, it) }
        val txId = repository.addTransaction(
            partyId = partyId,
            type = type,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            currencyDecimalPlaces = currencyDecimalPlaces,
            occurredAt = occurredAt,
            note = note
        )
        AppDatabase.getInstance(context).ledgerTransactionDao().updateExtras(
            id = txId,
            dueAt = dueAt.takeIf { type == TransactionType.CUSTOMER_DEBT || type == TransactionType.SUPPLIER_DEBT },
            attachmentPath = attachmentPath
        )
    }
}

fun MainViewModel.updateTransactionWithExtras(
    id: String,
    partyId: String,
    type: TransactionType,
    amountMinor: Long,
    currencyCode: String,
    currencyDecimalPlaces: Int,
    occurredAt: Long,
    note: String?,
    dueAt: Long?,
    attachmentUri: Uri?,
    removeAttachment: Boolean
) {
    viewModelScope.launch {
        if (amountMinor <= 0L || !isCompatibleParty(partyId, type)) return@launch
        val context = getApplication<Application>()
        val existing = repository.getTransactionById(id) ?: return@launch
        val newPath = when {
            removeAttachment -> null
            attachmentUri != null -> AttachmentService.copyToPrivateStorage(context, attachmentUri) ?: existing.attachmentPath
            else -> existing.attachmentPath
        }

        repository.updateTransaction(
            id = id,
            partyId = partyId,
            type = type,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            currencyDecimalPlaces = currencyDecimalPlaces,
            occurredAt = occurredAt,
            note = note
        )
        AppDatabase.getInstance(context).ledgerTransactionDao().updateExtras(
            id = id,
            dueAt = dueAt.takeIf { type == TransactionType.CUSTOMER_DEBT || type == TransactionType.SUPPLIER_DEBT },
            attachmentPath = newPath
        )

        if ((removeAttachment || (attachmentUri != null && newPath != existing.attachmentPath)) && existing.attachmentPath != null) {
            AttachmentService.deletePath(existing.attachmentPath)
        }
    }
}

private suspend fun MainViewModel.isCompatibleParty(partyId: String, type: TransactionType): Boolean {
    val party = repository.getPartyWithBalancesById(partyId)?.party ?: return false
    val partyType = PartyType.from(party.partyType)
    return if (type.isReceivableImpact) {
        partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
    } else {
        partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH
    }
}
