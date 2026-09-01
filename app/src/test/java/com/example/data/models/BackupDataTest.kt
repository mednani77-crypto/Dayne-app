package com.example.data.models

import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupDataTest {
    private val currency = CurrencyEntity(
        code = "DJF",
        name = "Djiboutian Franc",
        decimalPlaces = 0,
        isEnabled = true,
        createdAt = 1L,
        updatedAt = 1L
    )

    private val party = PartyEntity(
        id = "party-1",
        name = "Ahmed",
        normalizedName = "ahmed",
        partyType = PartyType.CUSTOMER.value,
        createdAt = 1L,
        updatedAt = 1L
    )

    private val transaction = LedgerTransactionEntity(
        id = "tx-1",
        partyId = party.id,
        transactionType = TransactionType.CUSTOMER_DEBT.value,
        amountMinor = 15_000L,
        currencyCode = currency.code,
        currencyDecimalPlaces = 0,
        occurredAt = 10L,
        createdAt = 10L,
        updatedAt = 10L
    )

    private val settings = SettingsEntity(
        id = 1,
        businessName = "Shop",
        countryCode = "DJ",
        languageCode = "ar",
        defaultCurrencyCode = "DJF",
        enabledCurrenciesJson = "[\"DJF\"]",
        onboardingCompleted = true,
        createdAt = 1L,
        updatedAt = 1L
    )

    @Test
    fun backupRoundTripPreservesFinancialData() {
        val original = BackupData(
            schemaVersion = 1,
            exportedAt = 100L,
            appVersion = "1.0.0",
            settings = settings,
            currencies = listOf(currency),
            parties = listOf(party),
            transactions = listOf(transaction)
        )

        val restored = BackupData.fromJsonString(original.toJsonString())
        assertEquals(1, restored.parties.size)
        assertEquals(1, restored.transactions.size)
        assertEquals(15_000L, restored.transactions.single().amountMinor)
        assertEquals("DJF", restored.settings?.defaultCurrencyCode)
    }

    @Test
    fun backupWithMissingPartyReferenceIsRejected() {
        val invalid = BackupData(
            schemaVersion = 1,
            exportedAt = 100L,
            appVersion = "1.0.0",
            settings = settings,
            currencies = listOf(currency),
            parties = emptyList(),
            transactions = listOf(transaction)
        )

        try {
            BackupData.fromJsonString(invalid.toJsonString())
            fail("Expected invalid backup to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().isNotBlank())
        }
    }

    @Test
    fun unsupportedSchemaIsRejected() {
        val invalid = BackupData(
            schemaVersion = 2,
            exportedAt = 100L,
            appVersion = "2.0.0",
            settings = settings,
            currencies = listOf(currency),
            parties = listOf(party),
            transactions = emptyList()
        )

        try {
            BackupData.fromJsonString(invalid.toJsonString())
            fail("Expected unsupported schema to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Unsupported"))
        }
    }
}
