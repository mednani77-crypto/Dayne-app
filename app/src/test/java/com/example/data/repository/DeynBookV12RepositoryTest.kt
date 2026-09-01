package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LedgerEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeynBookV12RepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: DeynBookV12Repository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.populateInitialData(database)
        repository = DeynBookV12Repository(database)
    }

    @After
    fun close() = database.close()

    @Test
    fun ledgersKeepAccountsAndDashboardCompletelySeparate() = runTest {
        val first = repository.getActiveLedger()
        val second = repository.createLedger("Wholesale", "DJ", "DJF")

        val secondParty = PartyEntity(
            id = "second-party",
            ledgerId = second.id,
            name = "Second Customer",
            normalizedName = "second customer",
            partyType = PartyType.CUSTOMER.value
        )
        database.partyDao().insertParty(secondParty)
        database.ledgerTransactionDao().insertTransaction(
            LedgerTransactionEntity(
                id = "second-debt",
                partyId = secondParty.id,
                transactionType = TransactionType.CUSTOMER_DEBT.value,
                amountMinor = 25_000L,
                currencyCode = "DJF",
                currencyDecimalPlaces = 0,
                occurredAt = 100L
            )
        )

        assertEquals(second.id, repository.getActiveLedger().id)
        assertEquals(listOf("Second Customer"), repository.getScopedParties().map { it.name })
        assertEquals(25_000L, repository.getSmartDashboardFlow("DJF").first().totalReceivable)

        assertTrue(repository.switchLedger(first.id))
        assertTrue(repository.getScopedParties().isEmpty())
        assertEquals(0L, repository.getSmartDashboardFlow("DJF").first().totalReceivable)
    }

    @Test
    fun multipleAttachmentsCanBelongToOneTransaction() = runTest {
        val ledger = repository.getActiveLedger()
        val party = PartyEntity(
            id = "p1",
            ledgerId = ledger.id,
            name = "Ahmed",
            normalizedName = "ahmed",
            partyType = PartyType.CUSTOMER.value
        )
        database.partyDao().insertParty(party)
        database.ledgerTransactionDao().insertTransaction(
            LedgerTransactionEntity(
                id = "t1",
                partyId = party.id,
                transactionType = TransactionType.CUSTOMER_DEBT.value,
                amountMinor = 10_000L,
                currencyCode = "DJF",
                currencyDecimalPlaces = 0,
                occurredAt = 1L
            )
        )
        repository.addAttachment("t1", "/tmp/a.jpg", "a.jpg", "image/jpeg", 10L)
        repository.addAttachment("t1", "/tmp/b.pdf", "b.pdf", "application/pdf", 20L)
        val items = repository.getAttachments("t1")
        assertEquals(2, items.size)
        assertEquals(setOf("a.jpg", "b.pdf"), items.map { it.displayName }.toSet())
    }
}
