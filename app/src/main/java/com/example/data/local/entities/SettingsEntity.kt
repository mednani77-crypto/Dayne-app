package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    // Legacy global profile fields are kept for backward-compatible backups/onboarding.
    // DeynBook 1.2 uses the active LedgerEntity profile for statements and the home title.
    val businessName: String,
    val businessPhone: String? = null,
    val businessAddress: String? = null,
    val countryCode: String,
    val languageCode: String,
    val defaultCurrencyCode: String,
    val enabledCurrenciesJson: String,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val onboardingCompleted: Boolean = false,
    val lastBackupAt: Long? = null,
    val biometricLockEnabled: Boolean = false,
    val calendarMode: String = "GREGORIAN", // GREGORIAN, ETHIOPIAN
    val activeLedgerId: String = "default-ledger",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
