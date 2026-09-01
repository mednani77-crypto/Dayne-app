package com.example.data.models

import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.1.0",
    val settings: SettingsEntity?,
    val currencies: List<CurrencyEntity>,
    val parties: List<PartyEntity>,
    val transactions: List<LedgerTransactionEntity>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("schemaVersion", schemaVersion)
        root.put("exportedAt", exportedAt)
        root.put("appVersion", appVersion)

        settings?.let { s ->
            root.put("settings", JSONObject().apply {
                put("businessName", s.businessName)
                put("businessPhone", s.businessPhone ?: "")
                put("businessAddress", s.businessAddress ?: "")
                put("countryCode", s.countryCode)
                put("languageCode", s.languageCode)
                put("defaultCurrencyCode", s.defaultCurrencyCode)
                put("enabledCurrenciesJson", s.enabledCurrenciesJson)
                put("themeMode", s.themeMode)
                put("onboardingCompleted", s.onboardingCompleted)
                put("biometricLockEnabled", s.biometricLockEnabled)
                put("calendarMode", s.calendarMode)
            })
        }

        root.put("currencies", JSONArray().apply {
            currencies.forEach { c ->
                put(JSONObject().apply {
                    put("code", c.code)
                    put("name", c.name)
                    put("symbol", c.symbol ?: "")
                    put("decimalPlaces", c.decimalPlaces)
                    put("isCustom", c.isCustom)
                    put("isEnabled", c.isEnabled)
                })
            }
        })

        root.put("parties", JSONArray().apply {
            parties.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("normalizedName", p.normalizedName)
                    put("phone", p.phone ?: "")
                    put("partyType", p.partyType)
                    put("notes", p.notes ?: "")
                    put("isArchived", p.isArchived)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("partyId", t.partyId)
                    put("transactionType", t.transactionType)
                    put("amountMinor", t.amountMinor)
                    put("currencyCode", t.currencyCode)
                    put("currencyDecimalPlaces", t.currencyDecimalPlaces)
                    put("occurredAt", t.occurredAt)
                    put("note", t.note ?: "")
                    if (t.dueAt != null) put("dueAt", t.dueAt) else put("dueAt", JSONObject.NULL)
                    put("attachmentPath", t.attachmentPath ?: "")
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                })
            }
        })

        return root.toString(2)
    }

    fun requireValid() {
        require(schemaVersion == 1) { "Unsupported backup version" }
        require(exportedAt > 0L) { "Invalid export time" }

        val currencyCodes = currencies.map { it.code }
        require(currencyCodes.size == currencyCodes.toSet().size) { "Duplicate currency code" }
        require(currencies.all { it.code.isNotBlank() && it.name.isNotBlank() && it.decimalPlaces in 0..2 }) {
            "Invalid currency definition"
        }

        val partyIds = parties.map { it.id }
        require(partyIds.size == partyIds.toSet().size) { "Duplicate party id" }
        require(parties.all {
            it.id.isNotBlank() &&
                it.name.isNotBlank() &&
                PartyType.entries.any { type -> type.value == it.partyType }
        }) { "Invalid party" }

        val transactionIds = transactions.map { it.id }
        require(transactionIds.size == transactionIds.toSet().size) { "Duplicate transaction id" }
        val partySet = partyIds.toSet()
        val currencySet = currencyCodes.toSet()
        require(transactions.all { tx ->
            tx.id.isNotBlank() &&
                tx.partyId in partySet &&
                tx.currencyCode in currencySet &&
                tx.amountMinor > 0L &&
                tx.currencyDecimalPlaces in 0..2 &&
                TransactionType.entries.any { type -> type.value == tx.transactionType }
        }) { "Invalid transaction or broken reference" }

        settings?.let { s ->
            require(s.defaultCurrencyCode in currencySet) { "Default currency is missing" }
            require(s.languageCode in setOf("ar", "so", "am", "fr", "en")) { "Invalid language" }
            require(s.calendarMode in setOf("GREGORIAN", "ETHIOPIAN")) { "Invalid calendar mode" }
        }
    }

    companion object {
        fun fromJsonString(jsonStr: String): BackupData {
            val root = JSONObject(jsonStr)
            val schemaVersion = root.getInt("schemaVersion")
            val exportedAt = root.getLong("exportedAt")
            val appVersion = root.optString("appVersion", "1.0.0")

            val settings = if (root.has("settings") && !root.isNull("settings")) {
                val obj = root.getJSONObject("settings")
                SettingsEntity(
                    id = 1,
                    businessName = obj.optString("businessName", ""),
                    businessPhone = obj.optString("businessPhone").takeIf { it.isNotBlank() },
                    businessAddress = obj.optString("businessAddress").takeIf { it.isNotBlank() },
                    countryCode = obj.optString("countryCode", "DJ"),
                    languageCode = obj.optString("languageCode", "ar"),
                    defaultCurrencyCode = obj.optString("defaultCurrencyCode", "DJF"),
                    enabledCurrenciesJson = obj.optString("enabledCurrenciesJson", "[\"DJF\"]"),
                    themeMode = obj.optString("themeMode", "SYSTEM"),
                    onboardingCompleted = obj.optBoolean("onboardingCompleted", true),
                    biometricLockEnabled = obj.optBoolean("biometricLockEnabled", false),
                    calendarMode = obj.optString("calendarMode", "GREGORIAN")
                )
            } else null

            val currencies = mutableListOf<CurrencyEntity>()
            val currencyArray = root.getJSONArray("currencies")
            for (i in 0 until currencyArray.length()) {
                val obj = currencyArray.getJSONObject(i)
                currencies += CurrencyEntity(
                    code = obj.getString("code"),
                    name = obj.getString("name"),
                    symbol = obj.optString("symbol").takeIf { it.isNotBlank() },
                    decimalPlaces = obj.getInt("decimalPlaces"),
                    isCustom = obj.optBoolean("isCustom", false),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            }

            val parties = mutableListOf<PartyEntity>()
            val partyArray = root.getJSONArray("parties")
            for (i in 0 until partyArray.length()) {
                val obj = partyArray.getJSONObject(i)
                val partyName = obj.getString("name")
                parties += PartyEntity(
                    id = obj.getString("id"),
                    name = partyName,
                    normalizedName = obj.optString("normalizedName", partyName.trim().lowercase()),
                    phone = obj.optString("phone").takeIf { it.isNotBlank() },
                    partyType = obj.getString("partyType"),
                    notes = obj.optString("notes").takeIf { it.isNotBlank() },
                    isArchived = obj.optBoolean("isArchived", false),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            }

            val transactions = mutableListOf<LedgerTransactionEntity>()
            val transactionArray = root.getJSONArray("transactions")
            for (i in 0 until transactionArray.length()) {
                val obj = transactionArray.getJSONObject(i)
                transactions += LedgerTransactionEntity(
                    id = obj.getString("id"),
                    partyId = obj.getString("partyId"),
                    transactionType = obj.getString("transactionType"),
                    amountMinor = obj.getLong("amountMinor"),
                    currencyCode = obj.getString("currencyCode"),
                    currencyDecimalPlaces = obj.getInt("currencyDecimalPlaces"),
                    occurredAt = obj.getLong("occurredAt"),
                    note = obj.optString("note").takeIf { it.isNotBlank() },
                    dueAt = if (obj.has("dueAt") && !obj.isNull("dueAt")) obj.getLong("dueAt") else null,
                    attachmentPath = obj.optString("attachmentPath").takeIf { it.isNotBlank() },
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            }

            return BackupData(
                schemaVersion = schemaVersion,
                exportedAt = exportedAt,
                appVersion = appVersion,
                settings = settings,
                currencies = currencies,
                parties = parties,
                transactions = transactions
            ).also { it.requireValid() }
        }
    }
}
