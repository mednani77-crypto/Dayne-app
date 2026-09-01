package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.models.BackupData
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeynBookRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: DeynBookRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DeynBookRepository(database)
        AppDatabase.populateInitialData(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateAndDeleteTransactionImmediatelyRecalculateBalance() = runTest {
        val partyId = repository.createPartyWithOptionalOpeningBalance(
            name = "Ahmed",
            phone = null,
            partyType = PartyType.CUSTOMER,
            notes = null,
            openingBalanceType = "NONE"
        )
        val txId = repository.addTransaction(
            partyId = partyId,
            type = TransactionType.CUSTOMER_DEBT,
            amountMinor = 15_000,
            currencyCode = "DJF",
            currencyDecimalPlaces = 0
        )

        assertEquals(15_000L, repository.getPartyWithBalancesById(partyId)!!.getBalanceForCurrency("DJF")!!.customerBalance)

        repository.updateTransaction(
            id = txId,
            partyId = partyId,
            type = TransactionType.CUSTOMER_DEBT,
            amountMinor = 12_000,
            currencyCode = "DJF",
            currencyDecimalPlaces = 0,
            occurredAt = 100L,
            note = null
        )
        assertEquals(12_000L, repository.getPartyWithBalancesById(partyId)!!.getBalanceForCurrency("DJF")!!.customerBalance)

        repository.deleteTransaction(txId)
        assertEquals(null, repository.getPartyWithBalancesById(partyId)!!.getBalanceForCurrency("DJF"))
    }

    @Test
    fun backupRestoreReproducesCountsAndBalances() = runTest {
        val partyId = repository.createPartyWithOptionalOpeningBalance(
            name = "Supplier",
            phone = null,
            partyType = PartyType.SUPPLIER,
            notes = null,
            openingBalanceType = "NONE"
        )
        repository.addTransaction(
            partyId = partyId,
            type = TransactionType.SUPPLIER_DEBT,
            amountMinor = 20_000,
            currencyCode = "DJF",
            currencyDecimalPlaces = 0
        )
        repository.addTransaction(
            partyId = partyId,
            type = TransactionType.SUPPLIER_PAYMENT,
            amountMinor = 8_000,
            currencyCode = "DJF",
            currencyDecimalPlaces = 0
        )

        val backup: BackupData = repository.exportBackupData()
        assertNotNull(backup.settings)
        repository.resetAllData()
        repository.restoreBackupData(backup)

        val restored = repository.getPartyWithBalancesById(partyId)
        assertNotNull(restored)
        assertEquals(2, restored!!.transactionCount)
        assertEquals(12_000L, restored.getBalanceForCurrency("DJF")!!.supplierBalance)
    }
}
