package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.formatting.DateFormatter
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LedgerEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.TransactionAttachmentEntity
import com.example.data.models.DashboardPartyAmount
import com.example.data.models.DashboardSummary
import com.example.data.models.PartyCurrencyBalance
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.data.models.TransactionReportSummary
import com.example.data.models.TransactionType
import com.example.data.models.TransactionWithParty
import com.example.services.CollectionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.UUID

/** Active-ledger domain layer for DeynBook 1.2. */
class DeynBookV12Repository(private val database: AppDatabase) {
    private val settingsDao = database.settingsDao()
    private val currencyDao = database.currencyDao()
    private val ledgerDao = database.ledgerDao()
    private val partyDao = database.partyDao()
    private val txDao = database.ledgerTransactionDao()
    private val attachmentDao = database.transactionAttachmentDao()

    fun getLedgersFlow(): Flow<List<LedgerEntity>> = ledgerDao.getAllLedgersFlow()

    fun getActiveLedgerFlow(): Flow<LedgerEntity?> = combine(
        settingsDao.getSettingsFlow(),
        ledgerDao.getAllLedgersFlow()
    ) { settings, ledgers ->
        val activeId = settings?.activeLedgerId ?: AppDatabase.DEFAULT_LEDGER_ID
        ledgers.firstOrNull { it.id == activeId }
            ?: ledgers.firstOrNull { !it.isArchived }
            ?: ledgers.firstOrNull()
    }

    suspend fun getActiveLedger(): LedgerEntity {
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        return ledgerDao.getById(settings.activeLedgerId)
            ?: ledgerDao.getAll().firstOrNull { !it.isArchived }
            ?: AppDatabase.defaultLedgerFromSettings(settings).also(ledgerDao::insert)
    }

    suspend fun switchLedger(ledgerId: String): Boolean = database.withTransaction {
        val ledger = ledgerDao.getById(ledgerId) ?: return@withTransaction false
        if (ledger.isArchived) return@withTransaction false
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        settingsDao.insertOrUpdate(
            settings.copy(
                activeLedgerId = ledger.id,
                businessName = ledger.name,
                businessPhone = ledger.phone,
                businessAddress = ledger.address,
                countryCode = ledger.countryCode,
                defaultCurrencyCode = ledger.defaultCurrencyCode,
                updatedAt = System.currentTimeMillis()
            )
        )
        ledgerDao.update(ledger.copy(updatedAt = System.currentTimeMillis()))
        true
    }

    suspend fun createLedger(
        name: String,
        countryCode: String,
        defaultCurrencyCode: String,
        phone: String? = null,
        address: String? = null
    ): LedgerEntity = database.withTransaction {
        val now = System.currentTimeMillis()
        val ledger = LedgerEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "DeynBook" },
            phone = phone?.trim()?.takeIf(String::isNotBlank),
            address = address?.trim()?.takeIf(String::isNotBlank),
            countryCode = countryCode,
            defaultCurrencyCode = defaultCurrencyCode,
            createdAt = now,
            updatedAt = now
        )
        ledgerDao.insert(ledger)
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        settingsDao.insertOrUpdate(
            settings.copy(
                activeLedgerId = ledger.id,
                businessName = ledger.name,
                businessPhone = ledger.phone,
                businessAddress = ledger.address,
                countryCode = ledger.countryCode,
                defaultCurrencyCode = ledger.defaultCurrencyCode,
                updatedAt = now
            )
        )
        ledger
    }

    suspend fun updateLedger(ledger: LedgerEntity) = database.withTransaction {
        val updated = ledger.copy(
            name = ledger.name.trim().ifBlank { "DeynBook" },
            phone = ledger.phone?.trim()?.takeIf(String::isNotBlank),
            address = ledger.address?.trim()?.takeIf(String::isNotBlank),
            footerNote = ledger.footerNote?.trim()?.takeIf(String::isNotBlank),
            updatedAt = System.currentTimeMillis()
        )
        ledgerDao.update(updated)
        val settings = settingsDao.getSettings()
        if (settings?.activeLedgerId == updated.id) {
            settingsDao.insertOrUpdate(
                settings.copy(
                    businessName = updated.name,
                    businessPhone = updated.phone,
                    businessAddress = updated.address,
                    countryCode = updated.countryCode,
                    defaultCurrencyCode = updated.defaultCurrencyCode,
                    updatedAt = updated.updatedAt
                )
            )
        }
    }

    suspend fun setLedgerLogo(ledgerId: String, logoPath: String?) {
        val ledger = ledgerDao.getById(ledgerId) ?: return
        updateLedger(ledger.copy(logoPath = logoPath))
    }

    suspend fun archiveLedger(ledgerId: String, archived: Boolean): Boolean = database.withTransaction {
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        if (archived && settings.activeLedgerId == ledgerId) return@withTransaction false
        ledgerDao.setArchived(ledgerId, archived)
        true
    }

    suspend fun deleteLedgerIfEmpty(ledgerId: String): Boolean = database.withTransaction {
        val all = ledgerDao.getAll()
        if (all.size <= 1) return@withTransaction false
        if (partyDao.countForLedger(ledgerId) > 0) return@withTransaction false
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        if (settings.activeLedgerId == ledgerId) return@withTransaction false
        ledgerDao.delete(ledgerId)
        true
    }

    suspend fun syncDefaultLedgerFromSettings() {
        val settings = settingsDao.getSettings() ?: return
        val active = ledgerDao.getById(settings.activeLedgerId)
        if (active == null || (active.id == AppDatabase.DEFAULT_LEDGER_ID && active.name == "DeynBook")) {
            val old = ledgerDao.getById(AppDatabase.DEFAULT_LEDGER_ID)
            ledgerDao.insert(
                (old ?: AppDatabase.defaultLedgerFromSettings(settings)).copy(
                    name = settings.businessName.ifBlank { old?.name ?: "DeynBook" },
                    phone = settings.businessPhone,
                    address = settings.businessAddress,
                    countryCode = settings.countryCode,
                    defaultCurrencyCode = settings.defaultCurrencyCode,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun assignPartyToActiveLedger(partyId: String) {
        val party = partyDao.getPartyById(partyId) ?: return
        val ledger = getActiveLedger()
        if (party.ledgerId != ledger.id) {
            partyDao.updateParty(party.copy(ledgerId = ledger.id, updatedAt = System.currentTimeMillis()))
        }
    }

    fun getScopedPartiesWithBalancesFlow(): Flow<List<PartyWithBalances>> = combine(
        settingsDao.getSettingsFlow(),
        partyDao.getAllPartiesFlow(),
        txDao.getAllTransactionsFlow(),
        currencyDao.getAllCurrenciesFlow()
    ) { settings, parties, transactions, currencies ->
        val ledgerId = settings?.activeLedgerId ?: AppDatabase.DEFAULT_LEDGER_ID
        val scopedParties = parties.filter { it.ledgerId == ledgerId }
        val scopedPartyIds = scopedParties.mapTo(hashSetOf()) { it.id }
        val scopedTransactions = transactions.filter { it.partyId in scopedPartyIds }
        val currencyMap = currencies.associateBy { it.code }
        val txByParty = scopedTransactions.groupBy { it.partyId }
        scopedParties.map { party ->
            val partyTxs = txByParty[party.id].orEmpty()
            val balances = partyTxs.map { it.currencyCode }.distinct().map { code ->
                PartyCurrencyBalance.calculateFromTransactions(
                    code,
                    currencyMap[code]?.decimalPlaces ?: 0,
                    partyTxs
                )
            }
            PartyWithBalances(
                party = party,
                balances = balances,
                transactionCount = partyTxs.size,
                lastTransactionTime = partyTxs.maxOfOrNull { it.occurredAt }
            )
        }
    }

    fun getScopedRecentTransactionsFlow(limit: Int = 25): Flow<List<TransactionWithParty>> = combine(
        settingsDao.getSettingsFlow(),
        partyDao.getAllPartiesFlow(),
        txDao.getAllTransactionsFlow()
    ) { settings, parties, transactions ->
        val ledgerId = settings?.activeLedgerId ?: AppDatabase.DEFAULT_LEDGER_ID
        val scopedParties = parties.filter { it.ledgerId == ledgerId }
        val partyMap = scopedParties.associateBy { it.id }
        transactions.asSequence()
            .filter { it.partyId in partyMap }
            .sortedWith(
                compareByDescending<LedgerTransactionEntity> { it.occurredAt }
                    .thenByDescending { it.createdAt }
            )
            .take(limit)
            .map { tx ->
                val party = partyMap.getValue(tx.partyId)
                TransactionWithParty(tx, party.name, party.phone, party.partyType)
            }
            .toList()
    }

    fun getSmartDashboardFlow(currencyCode: String): Flow<DashboardSummary> = combine(
        settingsDao.getSettingsFlow(),
        partyDao.getAllPartiesFlow(),
        txDao.getAllTransactionsFlow(),
        currencyDao.getAllCurrenciesFlow()
    ) { settings, parties, transactions, currencies ->
        val ledgerId = settings?.activeLedgerId ?: AppDatabase.DEFAULT_LEDGER_ID
        val scopedParties = parties.filter { it.ledgerId == ledgerId && !it.isArchived }
        val partyIds = scopedParties.mapTo(hashSetOf()) { it.id }
        val scopedTx = transactions.filter { it.partyId in partyIds }
        val decimals = currencies.firstOrNull { it.code == currencyCode }?.decimalPlaces ?: 0
        val txByParty = scopedTx.groupBy { it.partyId }

        var receivable = 0L
        var payable = 0L
        var customerCount = 0
        var supplierCount = 0
        val top = mutableListOf<DashboardPartyAmount>()
        scopedParties.forEach { party ->
            val balance = PartyCurrencyBalance.calculateFromTransactions(
                currencyCode,
                decimals,
                txByParty[party.id].orEmpty()
            )
            val partyType = PartyType.from(party.partyType)
            if ((partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH) && balance.customerBalance > 0L) {
                receivable = Math.addExact(receivable, balance.customerBalance)
                customerCount++
                top += DashboardPartyAmount(party.id, party.name, balance.customerBalance)
            }
            if ((partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH) && balance.supplierBalance > 0L) {
                payable = Math.addExact(payable, balance.supplierBalance)
                supplierCount++
            }
        }

        val now = System.currentTimeMillis()
        val collectionItems = CollectionCalculator.calculate(scopedParties, scopedTx, currencies, now)
            .filter { it.currencyCode == currencyCode }
        val startMonth = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endWeek = DateFormatter.endOfDay(now + 7L * 24L * 60L * 60L * 1000L)
        val collected = scopedTx.asSequence()
            .filter { it.currencyCode == currencyCode && it.occurredAt >= startMonth }
            .filter { TransactionType.from(it.transactionType) == TransactionType.CUSTOMER_PAYMENT }
            .fold(0L) { acc, tx -> Math.addExact(acc, tx.amountMinor) }
        val paidSuppliers = scopedTx.asSequence()
            .filter { it.currencyCode == currencyCode && it.occurredAt >= startMonth }
            .filter { TransactionType.from(it.transactionType) == TransactionType.SUPPLIER_PAYMENT }
            .fold(0L) { acc, tx -> Math.addExact(acc, tx.amountMinor) }

        DashboardSummary(
            currencyCode = currencyCode,
            decimalPlaces = decimals,
            totalReceivable = receivable,
            totalPayable = payable,
            activeCustomerCount = customerCount,
            activeSupplierCount = supplierCount,
            overdueCount = collectionItems.count { it.dueAt < DateFormatter.startOfDay(now) },
            dueNext7DaysCount = collectionItems.count {
                it.dueAt in DateFormatter.startOfDay(now)..endWeek
            },
            collectedThisMonth = collected,
            paidToSuppliersThisMonth = paidSuppliers,
            topDebtors = top.sortedByDescending { it.amountMinor }.take(5)
        )
    }

    suspend fun getScopedReportSummary(
        currencyCode: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): TransactionReportSummary {
        val ledger = getActiveLedger()
        val parties = partyDao.getAllPartiesForLedger(ledger.id)
        val txs = txDao.getAllTransactionsForLedger(ledger.id)
        val decimals = currencyDao.getCurrencyByCode(currencyCode)?.decimalPlaces ?: 0
        val txByParty = txs.groupBy { it.partyId }

        var totalReceivable = 0L
        var totalPayable = 0L
        var customerCredits = 0L
        var supplierAdvances = 0L
        val debtorCustomers = mutableListOf<Pair<PartyEntity, Long>>()
        val creditorSuppliers = mutableListOf<Pair<PartyEntity, Long>>()

        parties.forEach { party ->
            val bal = PartyCurrencyBalance.calculateFromTransactions(
                currencyCode,
                decimals,
                txByParty[party.id].orEmpty()
            )
            val type = PartyType.from(party.partyType)
            if (type == PartyType.CUSTOMER || type == PartyType.BOTH) {
                if (bal.customerBalance > 0L) {
                    totalReceivable = Math.addExact(totalReceivable, bal.customerBalance)
                    debtorCustomers += party to bal.customerBalance
                } else if (bal.customerBalance < 0L) {
                    customerCredits = Math.addExact(customerCredits, -bal.customerBalance)
                }
            }
            if (type == PartyType.SUPPLIER || type == PartyType.BOTH) {
                if (bal.supplierBalance > 0L) {
                    totalPayable = Math.addExact(totalPayable, bal.supplierBalance)
                    creditorSuppliers += party to bal.supplierBalance
                } else if (bal.supplierBalance < 0L) {
                    supplierAdvances = Math.addExact(supplierAdvances, -bal.supplierBalance)
                }
            }
        }

        var customerDebts = 0L
        var customerPayments = 0L
        var supplierDebts = 0L
        var supplierPayments = 0L
        val periodTx = txs.filter {
            it.currencyCode == currencyCode && it.occurredAt in fromTimestamp..toTimestamp
        }
        periodTx.forEach { tx ->
            when (TransactionType.from(tx.transactionType)) {
                TransactionType.CUSTOMER_DEBT,
                TransactionType.OPENING_RECEIVABLE -> customerDebts = Math.addExact(customerDebts, tx.amountMinor)
                TransactionType.CUSTOMER_PAYMENT -> customerPayments = Math.addExact(customerPayments, tx.amountMinor)
                TransactionType.SUPPLIER_DEBT,
                TransactionType.OPENING_PAYABLE -> supplierDebts = Math.addExact(supplierDebts, tx.amountMinor)
                TransactionType.SUPPLIER_PAYMENT -> supplierPayments = Math.addExact(supplierPayments, tx.amountMinor)
            }
        }

        return TransactionReportSummary(
            currencyCode = currencyCode,
            decimalPlaces = decimals,
            totalCustomerReceivableBalance = totalReceivable,
            totalSupplierPayableBalance = totalPayable,
            newCustomerDebtsInPeriod = customerDebts,
            customerPaymentsInPeriod = customerPayments,
            newSupplierDebtsInPeriod = supplierDebts,
            supplierPaymentsInPeriod = supplierPayments,
            totalCustomerCredits = customerCredits,
            totalSupplierAdvances = supplierAdvances,
            topDebtorCustomers = debtorCustomers.sortedByDescending { it.second }.take(5),
            topCreditorSuppliers = creditorSuppliers.sortedByDescending { it.second }.take(5),
            totalTransactionCount = periodTx.size
        )
    }

    suspend fun getScopedParties(): List<PartyEntity> =
        partyDao.getAllPartiesForLedger(getActiveLedger().id)

    suspend fun getScopedTransactions(): List<LedgerTransactionEntity> =
        txDao.getAllTransactionsForLedger(getActiveLedger().id)

    fun getAttachmentsFlow(transactionId: String): Flow<List<TransactionAttachmentEntity>> =
        attachmentDao.getForTransactionFlow(transactionId)

    suspend fun getAttachments(transactionId: String): List<TransactionAttachmentEntity> =
        attachmentDao.getForTransaction(transactionId)

    suspend fun addAttachment(
        transactionId: String,
        filePath: String,
        displayName: String,
        mimeType: String,
        sizeBytes: Long
    ): TransactionAttachmentEntity {
        val attachment = TransactionAttachmentEntity(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId,
            filePath = filePath,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
        attachmentDao.insert(attachment)
        return attachment
    }

    suspend fun deleteAttachment(id: String) = attachmentDao.deleteById(id)
}
