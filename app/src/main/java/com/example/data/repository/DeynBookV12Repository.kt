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
        ledgerDao.getById(settings.activeLedgerId)?.let { return it }
        ledgerDao.getAll().firstOrNull { !it.isArchived }?.let { return it }
        val created = AppDatabase.defaultLedgerFromSettings(settings)
        ledgerDao.insert(created)
        return created
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
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            address = address?.trim()?.takeIf { it.isNotBlank() },
            countryCode = countryCode.ifBlank { "DJ" },
            defaultCurrencyCode = defaultCurrencyCode.ifBlank { "DJF" },
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
        val now = System.currentTimeMillis()
        val updated = ledger.copy(
            name = ledger.name.trim().ifBlank { "DeynBook" },
            phone = ledger.phone?.trim()?.takeIf { it.isNotBlank() },
            address = ledger.address?.trim()?.takeIf { it.isNotBlank() },
            footerNote = ledger.footerNote?.trim()?.takeIf { it.isNotBlank() },
            updatedAt = now
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
                    updatedAt = now
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
        if (ledgerDao.getAll().size <= 1) return@withTransaction false
        if (partyDao.countForLedger(ledgerId) > 0) return@withTransaction false
        val settings = settingsDao.getSettings() ?: AppDatabase.DEFAULT_SETTINGS
        if (settings.activeLedgerId == ledgerId) return@withTransaction false
        ledgerDao.delete(ledgerId)
        true
    }

    suspend fun syncDefaultLedgerFromSettings() {
        val settings = settingsDao.getSettings() ?: return
        val active = ledgerDao.getById(settings.activeLedgerId)
        if (active == null) {
            val created = AppDatabase.defaultLedgerFromSettings(settings)
            ledgerDao.insert(created)
            return
        }
        if (active.id == AppDatabase.DEFAULT_LEDGER_ID && active.name == "DeynBook" && settings.businessName.isNotBlank()) {
            ledgerDao.update(
                active.copy(
                    name = settings.businessName,
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
        val active = getActiveLedger()
        if (party.ledgerId != active.id) {
            partyDao.updateParty(
                party.copy(
                    ledgerId = active.id,
                    updatedAt = System.currentTimeMillis()
                )
            )
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
        val byParty = scopedTransactions.groupBy { it.partyId }

        scopedParties.map { party ->
            val partyTx = byParty[party.id].orEmpty()
            val balances = partyTx.map { it.currencyCode }.distinct().map { code ->
                PartyCurrencyBalance.calculateFromTransactions(
                    currencyCode = code,
                    decimalPlaces = currencyMap[code]?.decimalPlaces ?: 0,
                    transactions = partyTx
                )
            }
            PartyWithBalances(
                party = party,
                balances = balances,
                transactionCount = partyTx.size,
                lastTransactionTime = partyTx.maxOfOrNull { it.occurredAt }
            )
        }
    }

    fun getScopedRecentTransactionsFlow(limit: Int = 25): Flow<List<TransactionWithParty>> = combine(
        settingsDao.getSettingsFlow(),
        partyDao.getAllPartiesFlow(),
        txDao.getAllTransactionsFlow()
    ) { settings, parties, transactions ->
        val ledgerId = settings?.activeLedgerId ?: AppDatabase.DEFAULT_LEDGER_ID
        val partyMap = parties.filter { it.ledgerId == ledgerId }.associateBy { it.id }
        transactions.asSequence()
            .filter { it.partyId in partyMap }
            .sortedWith(
                compareByDescending<LedgerTransactionEntity> { it.occurredAt }
                    .thenByDescending { it.createdAt }
            )
            .take(limit)
            .map { tx ->
                val party = partyMap.getValue(tx.partyId)
                TransactionWithParty(
                    transaction = tx,
                    partyName = party.name,
                    partyPhone = party.phone,
                    partyType = party.partyType
                )
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
        val byParty = scopedTx.groupBy { it.partyId }

        var receivable = 0L
        var payable = 0L
        var customerCount = 0
        var supplierCount = 0
        val topDebtors = mutableListOf<DashboardPartyAmount>()

        scopedParties.forEach { party ->
            val balance = PartyCurrencyBalance.calculateFromTransactions(
                currencyCode,
                decimals,
                byParty[party.id].orEmpty()
            )
            val type = PartyType.from(party.partyType)
            if ((type == PartyType.CUSTOMER || type == PartyType.BOTH) && balance.customerBalance > 0L) {
                receivable = Math.addExact(receivable, balance.customerBalance)
                customerCount++
                topDebtors += DashboardPartyAmount(party.id, party.name, balance.customerBalance)
            }
            if ((type == PartyType.SUPPLIER || type == PartyType.BOTH) && balance.supplierBalance > 0L) {
                payable = Math.addExact(payable, balance.supplierBalance)
                supplierCount++
            }
        }

        val now = System.currentTimeMillis()
        val collections = CollectionCalculator.calculate(scopedParties, scopedTx, currencies, now)
            .filter { it.currencyCode == currencyCode }
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val next7End = DateFormatter.endOfDay(now + 7L * 86_400_000L)

        val collectedThisMonth = scopedTx.asSequence()
            .filter { it.currencyCode == currencyCode && it.occurredAt >= monthStart }
            .filter { TransactionType.from(it.transactionType) == TransactionType.CUSTOMER_PAYMENT }
            .fold(0L) { acc, tx -> Math.addExact(acc, tx.amountMinor) }
        val paidSuppliersThisMonth = scopedTx.asSequence()
            .filter { it.currencyCode == currencyCode && it.occurredAt >= monthStart }
            .filter { TransactionType.from(it.transactionType) == TransactionType.SUPPLIER_PAYMENT }
            .fold(0L) { acc, tx -> Math.addExact(acc, tx.amountMinor) }

        DashboardSummary(
            currencyCode = currencyCode,
            decimalPlaces = decimals,
            totalReceivable = receivable,
            totalPayable = payable,
            activeCustomerCount = customerCount,
            activeSupplierCount = supplierCount,
            overdueCount = collections.count { it.dueAt < DateFormatter.startOfDay(now) },
            dueNext7DaysCount = collections.count {
                it.dueAt in DateFormatter.startOfDay(now)..next7End
            },
            collectedThisMonth = collectedThisMonth,
            paidToSuppliersThisMonth = paidSuppliersThisMonth,
            topDebtors = topDebtors.sortedByDescending { it.amountMinor }.take(5)
        )
    }

    suspend fun getScopedReportSummary(
        currencyCode: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): TransactionReportSummary {
        val ledger = getActiveLedger()
        val parties = partyDao.getAllPartiesForLedger(ledger.id)
        val transactions = txDao.getAllTransactionsForLedger(ledger.id)
        val decimals = currencyDao.getCurrencyByCode(currencyCode)?.decimalPlaces ?: 0
        val byParty = transactions.groupBy { it.partyId }

        var receivable = 0L
        var payable = 0L
        var customerCredits = 0L
        var supplierAdvances = 0L
        val debtors = mutableListOf<Pair<PartyEntity, Long>>()
        val suppliers = mutableListOf<Pair<PartyEntity, Long>>()

        parties.forEach { party ->
            val balance = PartyCurrencyBalance.calculateFromTransactions(
                currencyCode,
                decimals,
                byParty[party.id].orEmpty()
            )
            val type = PartyType.from(party.partyType)
            if (type == PartyType.CUSTOMER || type == PartyType.BOTH) {
                when {
                    balance.customerBalance > 0L -> {
                        receivable = Math.addExact(receivable, balance.customerBalance)
                        debtors += party to balance.customerBalance
                    }
                    balance.customerBalance < 0L -> {
                        customerCredits = Math.addExact(customerCredits, -balance.customerBalance)
                    }
                }
            }
            if (type == PartyType.SUPPLIER || type == PartyType.BOTH) {
                when {
                    balance.supplierBalance > 0L -> {
                        payable = Math.addExact(payable, balance.supplierBalance)
                        suppliers += party to balance.supplierBalance
                    }
                    balance.supplierBalance < 0L -> {
                        supplierAdvances = Math.addExact(supplierAdvances, -balance.supplierBalance)
                    }
                }
            }
        }

        val periodTx = transactions.filter {
            it.currencyCode == currencyCode && it.occurredAt in fromTimestamp..toTimestamp
        }
        var customerDebts = 0L
        var customerPayments = 0L
        var supplierDebts = 0L
        var supplierPayments = 0L
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
            totalCustomerReceivableBalance = receivable,
            totalSupplierPayableBalance = payable,
            newCustomerDebtsInPeriod = customerDebts,
            customerPaymentsInPeriod = customerPayments,
            newSupplierDebtsInPeriod = supplierDebts,
            supplierPaymentsInPeriod = supplierPayments,
            totalCustomerCredits = customerCredits,
            totalSupplierAdvances = supplierAdvances,
            topDebtorCustomers = debtors.sortedByDescending { it.second }.take(5),
            topCreditorSuppliers = suppliers.sortedByDescending { it.second }.take(5),
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

    suspend fun deleteAttachment(id: String) {
        attachmentDao.deleteById(id)
    }
}
