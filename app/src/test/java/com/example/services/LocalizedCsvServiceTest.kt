package com.example.services

import com.example.core.localization.AppLanguage
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedCsvServiceTest {

    private val customer = PartyEntity(
        id = "customer-1",
        name = "Ahmed",
        normalizedName = "ahmed",
        partyType = PartyType.CUSTOMER.value,
        createdAt = 1L,
        updatedAt = 1L
    )

    private val supplier = PartyEntity(
        id = "supplier-1",
        name = "Ali Supplier",
        normalizedName = "ali supplier",
        partyType = PartyType.SUPPLIER.value,
        createdAt = 1L,
        updatedAt = 1L
    )

    private fun tx(
        id: String,
        partyId: String,
        type: TransactionType,
        occurredAt: Long
    ) = LedgerTransactionEntity(
        id = id,
        partyId = partyId,
        transactionType = type.value,
        amountMinor = 1_500L,
        currencyCode = "DJF",
        currencyDecimalPlaces = 0,
        occurredAt = occurredAt,
        note = "note",
        createdAt = occurredAt,
        updatedAt = occurredAt
    )

    @Test
    fun arabicExportUsesArabicLabelsAndOnlyRequestedDateRange() {
        val csv = LocalizedCsvService.generate(
            transactions = listOf(
                tx("outside-before", customer.id, TransactionType.CUSTOMER_DEBT, 1_000L),
                tx("inside", supplier.id, TransactionType.SUPPLIER_PAYMENT, 2_000L),
                tx("outside-after", customer.id, TransactionType.CUSTOMER_PAYMENT, 3_000L)
            ),
            partiesById = mapOf(customer.id to customer, supplier.id to supplier),
            language = AppLanguage.ARABIC,
            fromTimestamp = 1_500L,
            toTimestamp = 2_500L
        )

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("معرّف العملية"))
        assertTrue(csv.contains("نوع العملية"))
        assertTrue(csv.contains("دفعت لمورد"))
        assertTrue(csv.contains("Ali Supplier"))
        assertTrue(csv.contains("inside"))
        assertFalse(csv.contains("outside-before"))
        assertFalse(csv.contains("outside-after"))
    }

    @Test
    fun frenchExportUsesFrenchHeaders() {
        val csv = LocalizedCsvService.generate(
            transactions = listOf(tx("tx-fr", customer.id, TransactionType.CUSTOMER_DEBT, 2_000L)),
            partiesById = mapOf(customer.id to customer),
            language = AppLanguage.FRENCH
        )

        assertTrue(csv.contains("ID de l’opération"))
        assertTrue(csv.contains("Type d’opération"))
        assertTrue(csv.contains("tx-fr"))
    }

    @Test
    fun reversedDateRangeIsNormalized() {
        val csv = LocalizedCsvService.generate(
            transactions = listOf(tx("inside-reversed", customer.id, TransactionType.CUSTOMER_DEBT, 2_000L)),
            partiesById = mapOf(customer.id to customer),
            language = AppLanguage.ENGLISH,
            fromTimestamp = 2_500L,
            toTimestamp = 1_500L
        )

        assertTrue(csv.contains("inside-reversed"))
    }

    @Test
    fun reportLanguageIsIndependentFromTranslatedUserContent() {
        val translatedParty = customer.copy(name = "أحمد")
        val translatedTx = tx("translated", customer.id, TransactionType.CUSTOMER_DEBT, 2_000L)
            .copy(note = "سكر وزيت")

        val csv = LocalizedCsvService.generate(
            transactions = listOf(translatedTx),
            partiesById = mapOf(translatedParty.id to translatedParty),
            language = AppLanguage.ARABIC
        )

        assertTrue(csv.contains("نوع العملية"))
        assertTrue(csv.contains("أحمد"))
        assertTrue(csv.contains("سكر وزيت"))
    }
}
