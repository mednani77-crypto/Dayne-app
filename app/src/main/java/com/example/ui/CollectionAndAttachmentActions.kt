package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.models.CollectionItem
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import com.example.data.repository.DeynBookV12Repository
import com.example.services.AttachmentService
import com.example.services.CollectionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun MainViewModel.loadCollectionItems(now: Long = System.currentTimeMillis()): List<CollectionItem> {
    val db = AppDatabase.getInstance(getApplication())
    val v12 = DeynBookV12Repository(db)
    return CollectionCalculator.calculate(
        parties = v12.getScopedParties(),
        transactions = v12.getScopedTransactions(),
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
        val stored = attachmentUri?.let { withContext(Dispatchers.IO) { AttachmentService.copyWithMetadata(context, it) } }
        val txId = repository.addTransaction(
            partyId = partyId,
            type = type,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            currencyDecimalPlaces = currencyDecimalPlaces,
            occurredAt = occurredAt,
            note = note
        )
        val db = AppDatabase.getInstance(context)
        db.ledgerTransactionDao().updateExtras(
            id = txId,
            dueAt = dueAt.takeIf { type == TransactionType.CUSTOMER_DEBT || type == TransactionType.SUPPLIER_DEBT },
            attachmentPath = stored?.path
        )
        stored?.let { file ->
            try {
                DeynBookV12Repository(db).addAttachment(txId, file.path, file.displayName, file.mimeType, file.sizeBytes)
            } catch (_: Exception) {
                AttachmentService.deletePath(file.path)
            }
        }
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
        val db = AppDatabase.getInstance(context)
        val v12 = DeynBookV12Repository(db)
        val existing = repository.getTransactionById(id) ?: return@launch
        val newStored = attachmentUri?.let { withContext(Dispatchers.IO) { AttachmentService.copyWithMetadata(context, it) } }

        repository.updateTransaction(id, partyId, type, amountMinor, currencyCode, currencyDecimalPlaces, occurredAt, note)
        val legacyPath = when {
            removeAttachment -> null
            newStored != null -> newStored.path
            else -> existing.attachmentPath
        }
        db.ledgerTransactionDao().updateExtras(
            id = id,
            dueAt = dueAt.takeIf { type == TransactionType.CUSTOMER_DEBT || type == TransactionType.SUPPLIER_DEBT },
            attachmentPath = legacyPath
        )

        if (removeAttachment && existing.attachmentPath != null) {
            v12.getAttachments(id).filter { it.filePath == existing.attachmentPath }.forEach { v12.deleteAttachment(it.id) }
            AttachmentService.deletePath(existing.attachmentPath)
        }
        newStored?.let { file ->
            try { v12.addAttachment(id, file.path, file.displayName, file.mimeType, file.sizeBytes) }
            catch (_: Exception) { AttachmentService.deletePath(file.path) }
        }
    }
}

private suspend fun MainViewModel.isCompatibleParty(partyId: String, type: TransactionType): Boolean {
    val party = repository.getPartyWithBalancesById(partyId)?.party ?: return false
    val partyType = PartyType.from(party.partyType)
    return if (type.isReceivableImpact) partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
    else partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH
}
