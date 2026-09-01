package com.example.services

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import com.example.data.local.entities.TransactionAttachmentEntity
import org.json.JSONArray
import org.json.JSONObject

/** Versioned backup format for multi-ledger DeynBook 1.2. Reads legacy v1 backups too. */
data class V12BackupPackage(
    val schemaVersion: Int,
    val exportedAt: Long,
    val settings: SettingsEntity,
    val currencies: List<CurrencyEntity>,
    val ledgers: List<LedgerEntity>,
    val parties: List<PartyEntity>,
    val transactions: List<LedgerTransactionEntity>,
    val attachments: List<TransactionAttachmentEntity>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("schemaVersion", 2)
        root.put("appVersion", "1.2.0")
        root.put("exportedAt", exportedAt)
        root.put("settings", JSONObject().apply {
            put("businessName", settings.businessName)
            put("businessPhone", settings.businessPhone ?: "")
            put("businessAddress", settings.businessAddress ?: "")
            put("countryCode", settings.countryCode)
            put("languageCode", settings.languageCode)
            put("defaultCurrencyCode", settings.defaultCurrencyCode)
            put("enabledCurrenciesJson", settings.enabledCurrenciesJson)
            put("themeMode", settings.themeMode)
            put("onboardingCompleted", settings.onboardingCompleted)
            put("biometricLockEnabled", settings.biometricLockEnabled)
            put("calendarMode", settings.calendarMode)
            put("activeLedgerId", settings.activeLedgerId)
        })
        root.put("currencies", JSONArray().apply { currencies.forEach { c -> put(JSONObject().apply {
            put("code", c.code); put("name", c.name); put("symbol", c.symbol ?: ""); put("decimalPlaces", c.decimalPlaces); put("isCustom", c.isCustom); put("isEnabled", c.isEnabled)
        }) } })
        root.put("ledgers", JSONArray().apply { ledgers.forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("name", l.name); put("phone", l.phone ?: ""); put("address", l.address ?: ""); put("countryCode", l.countryCode); put("defaultCurrencyCode", l.defaultCurrencyCode); put("logoPath", l.logoPath ?: ""); put("footerNote", l.footerNote ?: ""); put("isArchived", l.isArchived); put("createdAt", l.createdAt); put("updatedAt", l.updatedAt)
        }) } })
        root.put("parties", JSONArray().apply { parties.forEach { p -> put(JSONObject().apply {
            put("id", p.id); put("ledgerId", p.ledgerId); put("name", p.name); put("normalizedName", p.normalizedName); put("phone", p.phone ?: ""); put("partyType", p.partyType); put("notes", p.notes ?: ""); put("isArchived", p.isArchived); put("createdAt", p.createdAt); put("updatedAt", p.updatedAt)
        }) } })
        root.put("transactions", JSONArray().apply { transactions.forEach { t -> put(JSONObject().apply {
            put("id", t.id); put("partyId", t.partyId); put("transactionType", t.transactionType); put("amountMinor", t.amountMinor); put("currencyCode", t.currencyCode); put("currencyDecimalPlaces", t.currencyDecimalPlaces); put("occurredAt", t.occurredAt); put("note", t.note ?: ""); if (t.dueAt != null) put("dueAt", t.dueAt); put("attachmentPath", t.attachmentPath ?: ""); put("createdAt", t.createdAt); put("updatedAt", t.updatedAt)
        }) } })
        root.put("attachments", JSONArray().apply { attachments.forEach { a -> put(JSONObject().apply {
            put("id", a.id); put("transactionId", a.transactionId); put("filePath", a.filePath); put("displayName", a.displayName); put("mimeType", a.mimeType); put("sizeBytes", a.sizeBytes); put("createdAt", a.createdAt)
        }) } })
        return root.toString(2)
    }
}

object V12BackupService {
    suspend fun export(database: AppDatabase): V12BackupPackage {
        val settingsDao = database.settingsDao()
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        val now = System.currentTimeMillis()
        settingsDao.insertOrUpdate(settings.copy(lastBackupAt = now, updatedAt = now))
        return V12BackupPackage(
            schemaVersion = 2,
            exportedAt = now,
            settings = settings.copy(lastBackupAt = now),
            currencies = database.currencyDao().getAllCurrencies(),
            ledgers = database.ledgerDao().getAll(),
            parties = database.partyDao().getAllParties(),
            transactions = database.ledgerTransactionDao().getAllTransactions(),
            attachments = database.transactionAttachmentDao().getAll()
        )
    }

    fun parse(json: String): V12BackupPackage {
        val root = JSONObject(json)
        val version = root.optInt("schemaVersion", 1)
        require(version in 1..2) { "Unsupported backup version" }
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val s = root.optJSONObject("settings") ?: JSONObject()
        val settings = SettingsEntity(
            id = 1,
            businessName = s.optString("businessName", ""),
            businessPhone = s.optString("businessPhone").takeIf(String::isNotBlank),
            businessAddress = s.optString("businessAddress").takeIf(String::isNotBlank),
            countryCode = s.optString("countryCode", "DJ"),
            languageCode = s.optString("languageCode", "ar"),
            defaultCurrencyCode = s.optString("defaultCurrencyCode", "DJF"),
            enabledCurrenciesJson = s.optString("enabledCurrenciesJson", "[\"DJF\",\"USD\"]"),
            themeMode = s.optString("themeMode", "SYSTEM"),
            onboardingCompleted = s.optBoolean("onboardingCompleted", true),
            lastBackupAt = exportedAt,
            biometricLockEnabled = s.optBoolean("biometricLockEnabled", false),
            calendarMode = s.optString("calendarMode", "GREGORIAN"),
            activeLedgerId = s.optString("activeLedgerId", AppDatabase.DEFAULT_LEDGER_ID)
        )

        val currencies = mutableListOf<CurrencyEntity>()
        root.optJSONArray("currencies")?.let { arr -> for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); currencies += CurrencyEntity(
            code = o.getString("code"), name = o.getString("name"), symbol = o.optString("symbol").takeIf(String::isNotBlank), decimalPlaces = o.optInt("decimalPlaces", 0), isCustom = o.optBoolean("isCustom", false), isEnabled = o.optBoolean("isEnabled", true)
        ) } }

        val ledgers = mutableListOf<LedgerEntity>()
        root.optJSONArray("ledgers")?.let { arr -> for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); ledgers += LedgerEntity(
            id = o.getString("id"), name = o.getString("name"), phone = o.optString("phone").takeIf(String::isNotBlank), address = o.optString("address").takeIf(String::isNotBlank), countryCode = o.optString("countryCode", "DJ"), defaultCurrencyCode = o.optString("defaultCurrencyCode", "DJF"), logoPath = o.optString("logoPath").takeIf(String::isNotBlank), footerNote = o.optString("footerNote").takeIf(String::isNotBlank), isArchived = o.optBoolean("isArchived", false), createdAt = o.optLong("createdAt", exportedAt), updatedAt = o.optLong("updatedAt", exportedAt)
        ) } }
        if (ledgers.isEmpty()) ledgers += AppDatabase.defaultLedgerFromSettings(settings)
        val ledgerIds = ledgers.mapTo(hashSetOf()) { it.id }
        val activeId = settings.activeLedgerId.takeIf { it in ledgerIds } ?: ledgers.first().id
        val fixedSettings = settings.copy(activeLedgerId = activeId)

        val parties = mutableListOf<PartyEntity>()
        root.optJSONArray("parties")?.let { arr -> for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); val name = o.getString("name"); parties += PartyEntity(
            id = o.getString("id"), ledgerId = o.optString("ledgerId", activeId).takeIf { it in ledgerIds } ?: activeId, name = name, normalizedName = o.optString("normalizedName", name.trim().lowercase()), phone = o.optString("phone").takeIf(String::isNotBlank), partyType = o.getString("partyType"), notes = o.optString("notes").takeIf(String::isNotBlank), isArchived = o.optBoolean("isArchived", false), createdAt = o.optLong("createdAt", exportedAt), updatedAt = o.optLong("updatedAt", exportedAt)
        ) } }
        val partyIds = parties.mapTo(hashSetOf()) { it.id }

        val transactions = mutableListOf<LedgerTransactionEntity>()
        root.optJSONArray("transactions")?.let { arr -> for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); val partyId = o.getString("partyId"); if (partyId in partyIds) transactions += LedgerTransactionEntity(
            id = o.getString("id"), partyId = partyId, transactionType = o.getString("transactionType"), amountMinor = o.getLong("amountMinor"), currencyCode = o.getString("currencyCode"), currencyDecimalPlaces = o.optInt("currencyDecimalPlaces", 0), occurredAt = o.getLong("occurredAt"), note = o.optString("note").takeIf(String::isNotBlank), dueAt = if (o.has("dueAt") && !o.isNull("dueAt")) o.optLong("dueAt") else null, attachmentPath = o.optString("attachmentPath").takeIf(String::isNotBlank), createdAt = o.optLong("createdAt", exportedAt), updatedAt = o.optLong("updatedAt", exportedAt)
        ) } }
        val txIds = transactions.mapTo(hashSetOf()) { it.id }

        val attachments = mutableListOf<TransactionAttachmentEntity>()
        root.optJSONArray("attachments")?.let { arr -> for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); val txId = o.getString("transactionId"); if (txId in txIds) attachments += TransactionAttachmentEntity(
            id = o.getString("id"), transactionId = txId, filePath = o.getString("filePath"), displayName = o.optString("displayName", "Attachment"), mimeType = o.optString("mimeType", "application/octet-stream"), sizeBytes = o.optLong("sizeBytes", 0L), createdAt = o.optLong("createdAt", exportedAt)
        ) } }
        if (attachments.isEmpty()) {
            transactions.filter { !it.attachmentPath.isNullOrBlank() }.forEach { tx -> attachments += TransactionAttachmentEntity(
                id = "legacy-${tx.id}", transactionId = tx.id, filePath = tx.attachmentPath!!, displayName = "Attachment", mimeType = "application/octet-stream", createdAt = tx.createdAt
            ) }
        }

        require(currencies.map { it.code }.distinct().size == currencies.size) { "Duplicate currencies" }
        require(parties.map { it.id }.distinct().size == parties.size) { "Duplicate accounts" }
        require(transactions.all { it.amountMinor > 0L }) { "Invalid amount" }
        return V12BackupPackage(version, exportedAt, fixedSettings, currencies.ifEmpty { AppDatabase.DEFAULT_CURRENCIES }, ledgers, parties, transactions, attachments)
    }

    suspend fun restore(database: AppDatabase, backup: V12BackupPackage) = database.withTransaction {
        database.transactionAttachmentDao().clear()
        database.ledgerTransactionDao().clearTransactions()
        database.partyDao().clearParties()
        database.ledgerDao().clear()
        database.currencyDao().clearCurrencies()
        database.settingsDao().clearSettings()
        database.currencyDao().insertCurrencies(backup.currencies.ifEmpty { AppDatabase.DEFAULT_CURRENCIES })
        database.ledgerDao().insertAll(backup.ledgers)
        database.partyDao().insertParties(backup.parties)
        database.ledgerTransactionDao().insertTransactions(backup.transactions)
        if (backup.attachments.isNotEmpty()) database.transactionAttachmentDao().insertAll(backup.attachments)
        database.settingsDao().insertOrUpdate(backup.settings.copy(id = 1, lastBackupAt = backup.exportedAt, updatedAt = System.currentTimeMillis()))
    }
}
