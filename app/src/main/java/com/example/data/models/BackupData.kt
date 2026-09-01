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
    val appVersion: String = "1.0.0",
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
            val sObj = JSONObject().apply {
                put("businessName", s.businessName)
                put("businessPhone", s.businessPhone ?: "")
                put("businessAddress", s.businessAddress ?: "")
                put("countryCode", s.countryCode)
                put("languageCode", s.languageCode)
                put("defaultCurrencyCode", s.defaultCurrencyCode)
                put("enabledCurrenciesJson", s.enabledCurrenciesJson)
                put("themeMode", s.themeMode)
                put("onboardingCompleted", s.onboardingCompleted)
            }
            root.put("settings", sObj)
        }

        val currenciesArray = JSONArray()
        currencies.forEach { c ->
            val cObj = JSONObject().apply {
                put("code", c.code)
                put("name", c.name)
                put("symbol", c.symbol ?: "")
                put("decimalPlaces", c.decimalPlaces)
                put("isCustom", c.isCustom)
                put("isEnabled", c.isEnabled)
            }
            currenciesArray.put(cObj)
        }
        root.put("currencies", currenciesArray)

        val partiesArray = JSONArray()
        parties.forEach { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("normalizedName", p.normalizedName)
                put("phone", p.phone ?: "")
                put("partyType", p.partyType)
                put("notes", p.notes ?: "")
                put("isArchived", p.isArchived)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            partiesArray.put(pObj)
        }
        root.put("parties", partiesArray)

        val txArray = JSONArray()
        transactions.forEach { t ->
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("partyId", t.partyId)
                put("transactionType", t.transactionType)
                put("amountMinor", t.amountMinor)
                put("currencyCode", t.currencyCode)
                put("currencyDecimalPlaces", t.currencyDecimalPlaces)
                put("occurredAt", t.occurredAt)
                put("note", t.note ?: "")
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
            }
            txArray.put(tObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): BackupData {
            val root = JSONObject(jsonStr)
            val schemaVersion = root.getInt("schemaVersion")
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val appVersion = root.optString("appVersion", "1.0.0")

            var settings: SettingsEntity? = null
            if (root.has("settings") && !root.isNull("settings")) {
                val sObj = root.getJSONObject("settings")
                settings = SettingsEntity(
                    id = 1,
                    businessName = sObj.optString("businessName", ""),
                    businessPhone = sObj.optString("businessPhone").takeIf { it.isNotBlank() },
                    businessAddress = sObj.optString("businessAddress").takeIf { it.isNotBlank() },
                    countryCode = sObj.optString("countryCode", "DJ"),
                    languageCode = sObj.optString("languageCode", "ar"),
                    defaultCurrencyCode = sObj.optString("defaultCurrencyCode", "DJF"),
                    enabledCurrenciesJson = sObj.optString("enabledCurrenciesJson", "[\"DJF\"]"),
                    themeMode = sObj.optString("themeMode", "SYSTEM"),
                    onboardingCompleted = sObj.optBoolean("onboardingCompleted", true)
                )
            }

            val currencies = mutableListOf<CurrencyEntity>()
            if (root.has("currencies")) {
                val arr = root.getJSONArray("currencies")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    currencies.add(
                        CurrencyEntity(
                            code = obj.getString("code"),
                            name = obj.getString("name"),
                            symbol = obj.optString("symbol").takeIf { it.isNotBlank() },
                            decimalPlaces = obj.optInt("decimalPlaces", 0),
                            isCustom = obj.optBoolean("isCustom", false),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
            }

            val parties = mutableListOf<PartyEntity>()
            if (root.has("parties")) {
                val arr = root.getJSONArray("parties")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parties.add(
                        PartyEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            normalizedName = obj.optString("normalizedName", obj.getString("name").trim().lowercase()),
                            phone = obj.optString("phone").takeIf { it.isNotBlank() },
                            partyType = obj.getString("partyType"),
                            notes = obj.optString("notes").takeIf { it.isNotBlank() },
                            isArchived = obj.optBoolean("isArchived", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val transactions = mutableListOf<LedgerTransactionEntity>()
            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    transactions.add(
                        LedgerTransactionEntity(
                            id = obj.getString("id"),
                            partyId = obj.getString("partyId"),
                            transactionType = obj.getString("transactionType"),
                            amountMinor = obj.getLong("amountMinor"),
                            currencyCode = obj.getString("currencyCode"),
                            currencyDecimalPlaces = obj.optInt("currencyDecimalPlaces", 0),
                            occurredAt = obj.optLong("occurredAt", System.currentTimeMillis()),
                            note = obj.optString("note").takeIf { it.isNotBlank() },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return BackupData(
                schemaVersion = schemaVersion,
                exportedAt = exportedAt,
                appVersion = appVersion,
                settings = settings,
                currencies = currencies,
                parties = parties,
                transactions = transactions
            )
        }
    }
}
