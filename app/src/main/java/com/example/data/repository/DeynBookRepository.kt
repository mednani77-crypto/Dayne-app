package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import com.example.data.models.BackupData
import com.example.data.models.DashboardSummary
import com.example.data.models.PartyCurrencyBalance
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.data.models.StatementData
import com.example.data.models.StatementRow
import com.example.data.models.TransactionReportSummary
import com.example.data.models.TransactionType
import com.example.data.models.TransactionWithParty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class DeynBookRepository(private val database: AppDatabase) {

    private val settingsDao = database.settingsDao()
    private val currencyDao = database.currencyDao()
    private val partyDao = database.partyDao()
    private val txDao = database.ledgerTransactionDao()

    // -------------------------------------------------------------
    // SETTINGS & CURRENCIES
    // -------------------------------------------------------------

    fun getSettingsFlow(): Flow<SettingsEntity?> = settingsDao.getSettingsFlow()

    suspend fun getSettings(): SettingsEntity {
        return settingsDao.getSettings() ?: run {
            AppDatabase.DEFAULT_SETTINGS.also { settingsDao.insertOrUpdate(it) }
        }
    }

    suspend fun updateSettings(settings: SettingsEntity) {
        settingsDao.insertOrUpdate(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    fun getEnabledCurrenciesFlow(): Flow<List<CurrencyEntity>> = currencyDao.getEnabledCurrenciesFlow()
    fun getAllCurrenciesFlow(): Flow<List<CurrencyEntity>> = currencyDao.getAllCurrenciesFlow()

    suspend fun getAllCurrencies(): List<CurrencyEntity> = currencyDao.getAllCurrencies()

    suspend fun getCurrencyByCode(code: String): CurrencyEntity {
        return currencyDao.getCurrencyByCode(code) ?: CurrencyEntity(
            code = code,
            name = code,
            decimalPlaces = 0,
            isEnabled = true
        )
    }

    suspend fun saveCurrency(currency: CurrencyEntity) {
        currencyDao.insertCurrency(currency.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setCurrencyEnabled(code: String, isEnabled: Boolean) {
        currencyDao.setCurrencyEnabled(code, isEnabled)
    }

    // -------------------------------------------------------------
    // PARTIES & BALANCES
    // -------------------------------------------------------------

    fun getAllPartiesWithBalancesFlow(): Flow<List<PartyWithBalances>> {
        return combine(
            partyDao.getAllPartiesFlow(),
            txDao.getAllTransactionsFlow(),
            currencyDao.getAllCurrenciesFlow()
        ) { parties, transactions, currencies ->
            val currencyMap = currencies.associateBy { it.code }
            val txByParty = transactions.groupBy { it.partyId }

            parties.map { party ->
                val partyTxs = txByParty[party.id] ?: emptyList()
                val usedCurrencyCodes = partyTxs.map { it.currencyCode }.distinct()

                val balances = usedCurrencyCodes.map { code ->
                    val decimals = currencyMap[code]?.decimalPlaces ?: 0
                    PartyCurrencyBalance.calculateFromTransactions(code, decimals, partyTxs)
                }

                val lastTxTime = partyTxs.maxOfOrNull { it.occurredAt }

                PartyWithBalances(
                    party = party,
                    balances = balances,
                    transactionCount = partyTxs.size,
                    lastTransactionTime = lastTxTime
                )
            }
        }
    }

    suspend fun getPartyWithBalancesById(partyId: String): PartyWithBalances? {
        val party = partyDao.getPartyById(partyId) ?: return null
        val transactions = txDao.getChronologicalTransactionsForParty(partyId)
        val currencies = currencyDao.getAllCurrencies().associateBy { it.code }

        val usedCurrencyCodes = transactions.map { it.currencyCode }.distinct()
        val balances = usedCurrencyCodes.map { code ->
            val decimals = currencies[code]?.decimalPlaces ?: 0
            PartyCurrencyBalance.calculateFromTransactions(code, decimals, transactions)
        }

        return PartyWithBalances(
            party = party,
            balances = balances,
            transactionCount = transactions.size,
            lastTransactionTime = transactions.maxOfOrNull { it.occurredAt }
        )
    }

    suspend fun searchSimilarParties(name: String): List<PartyEntity> {
        val normalized = name.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        return partyDao.searchParties(normalized)
    }

    suspend fun createPartyWithOptionalOpeningBalance(
        name: String,
        phone: String?,
        partyType: PartyType,
        notes: String?,
        openingBalanceType: String, // "NONE", "HE_OWES_ME", "I_OWE_HIM"
        openingAmountMinor: Long = 0L,
        currencyCode: String = "DJF",
        currencyDecimalPlaces: Int = 0,
        occurredAt: Long = System.currentTimeMillis()
    ): String {
        return database.withTransaction {
            val partyId = UUID.randomUUID().toString()
            val cleanName = name.trim()
            val party = PartyEntity(
                id = partyId,
                name = cleanName,
                normalizedName = cleanName.lowercase(),
                phone = phone?.trim()?.takeIf { it.isNotBlank() },
                partyType = partyType.value,
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            partyDao.insertParty(party)

            if (openingAmountMinor > 0L && openingBalanceType != "NONE") {
                val txType = when (openingBalanceType) {
                    "HE_OWES_ME" -> TransactionType.OPENING_RECEIVABLE.value
                    "I_OWE_HIM" -> TransactionType.OPENING_PAYABLE.value
                    else -> null
                }

                if (txType != null) {
                    val tx = LedgerTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        partyId = partyId,
                        transactionType = txType,
                        amountMinor = openingAmountMinor,
                        currencyCode = currencyCode,
                        currencyDecimalPlaces = currencyDecimalPlaces,
                        occurredAt = occurredAt,
                        note = "الرصيد الافتتاحي / Opening Balance",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    txDao.insertTransaction(tx)
                }
            }

            partyId
        }
    }

    suspend fun updateParty(party: PartyEntity) {
        partyDao.updateParty(party.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setPartyArchived(partyId: String, isArchived: Boolean) {
        partyDao.setPartyArchived(partyId, isArchived)
    }

    suspend fun deletePartyIfNoTransactions(partyId: String): Boolean {
        return database.withTransaction {
            val count = txDao.getTransactionCountForParty(partyId)
            if (count == 0) {
                partyDao.deleteParty(partyId)
                true
            } else {
                false
            }
        }
    }

    // -------------------------------------------------------------
    // TRANSACTIONS
    // -------------------------------------------------------------

    fun getRecentTransactionsWithPartyFlow(limit: Int = 20): Flow<List<TransactionWithParty>> {
        return combine(
            txDao.getRecentTransactionsFlow(limit),
            partyDao.getAllPartiesFlow()
        ) { txs, parties ->
            val partyMap = parties.associateBy { it.id }
            txs.map { tx ->
                val party = partyMap[tx.partyId]
                TransactionWithParty(
                    transaction = tx,
                    partyName = party?.name ?: "Unknown",
                    partyPhone = party?.phone,
                    partyType = party?.partyType ?: "CUSTOMER"
                )
            }
        }
    }

    fun getTransactionsForPartyFlow(partyId: String): Flow<List<LedgerTransactionEntity>> {
        return txDao.getTransactionsByPartyIdFlow(partyId)
    }

    suspend fun addTransaction(
        partyId: String,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int,
        occurredAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): String {
        return database.withTransaction {
            val txId = UUID.randomUUID().toString()
            val tx = LedgerTransactionEntity(
                id = txId,
                partyId = partyId,
                transactionType = type.value,
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                currencyDecimalPlaces = currencyDecimalPlaces,
                occurredAt = occurredAt,
                note = note?.trim()?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            txDao.insertTransaction(tx)
            txId
        }
    }

    suspend fun updateTransaction(
        id: String,
        partyId: String,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int,
        occurredAt: Long,
        note: String?
    ) {
        database.withTransaction {
            val existing = txDao.getTransactionById(id)
            if (existing != null) {
                val updated = existing.copy(
                    partyId = partyId,
                    transactionType = type.value,
                    amountMinor = amountMinor,
                    currencyCode = currencyCode,
                    currencyDecimalPlaces = currencyDecimalPlaces,
                    occurredAt = occurredAt,
                    note = note?.trim()?.takeIf { it.isNotBlank() },
                    updatedAt = System.currentTimeMillis()
                )
                txDao.updateTransaction(updated)
            }
        }
    }

    suspend fun deleteTransaction(id: String) {
        database.withTransaction {
            txDao.deleteTransactionById(id)
        }
    }

    suspend fun getTransactionById(id: String): LedgerTransactionEntity? {
        return txDao.getTransactionById(id)
    }

    // -------------------------------------------------------------
    // DASHBOARD & REPORTS (Multi-currency isolation)
    // -------------------------------------------------------------

    fun getDashboardSummaryFlow(currencyCode: String): Flow<DashboardSummary> {
        return combine(
            partyDao.getActivePartiesFlow(),
            txDao.getAllTransactionsFlow(),
            currencyDao.getAllCurrenciesFlow()
        ) { parties, transactions, currencies ->
            val decimals = currencies.find { it.code == currencyCode }?.decimalPlaces ?: 0
            val txByParty = transactions.groupBy { it.partyId }

            var totalReceivable = 0L // Sum of positive customer balances
            var totalPayable = 0L    // Sum of positive supplier balances
            var activeCustomerCount = 0
            var activeSupplierCount = 0

            parties.forEach { party ->
                val partyTxs = txByParty[party.id] ?: emptyList()
                val balance = PartyCurrencyBalance.calculateFromTransactions(currencyCode, decimals, partyTxs)

                if (party.partyType == PartyType.CUSTOMER.value || party.partyType == PartyType.BOTH.value) {
                    if (balance.customerBalance > 0L) {
                        totalReceivable += balance.customerBalance
                        activeCustomerCount++
                    }
                }

                if (party.partyType == PartyType.SUPPLIER.value || party.partyType == PartyType.BOTH.value) {
                    if (balance.supplierBalance > 0L) {
                        totalPayable += balance.supplierBalance
                        activeSupplierCount++
                    }
                }
            }

            DashboardSummary(
                currencyCode = currencyCode,
                decimalPlaces = decimals,
                totalReceivable = totalReceivable,
                totalPayable = totalPayable,
                activeCustomerCount = activeCustomerCount,
                activeSupplierCount = activeSupplierCount
            )
        }
    }

    suspend fun getReportSummary(
        currencyCode: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): TransactionReportSummary {
        val allParties = partyDao.getAllParties()
        val allTxs = txDao.getAllTransactions()
        val currencies = currencyDao.getAllCurrencies().associateBy { it.code }
        val decimals = currencies[currencyCode]?.decimalPlaces ?: 0

        val txByParty = allTxs.groupBy { it.partyId }

        var totalCustomerReceivable = 0L
        var totalSupplierPayable = 0L
        var totalCustomerCredits = 0L
        var totalSupplierAdvances = 0L

        val customerDebtors = mutableListOf<Pair<PartyEntity, Long>>()
        val supplierCreditors = mutableListOf<Pair<PartyEntity, Long>>()

        allParties.forEach { party ->
            val partyTxs = txByParty[party.id] ?: emptyList()
            val balance = PartyCurrencyBalance.calculateFromTransactions(currencyCode, decimals, partyTxs)

            if (party.partyType == PartyType.CUSTOMER.value || party.partyType == PartyType.BOTH.value) {
                if (balance.customerBalance > 0L) {
                    totalCustomerReceivable += balance.customerBalance
                    customerDebtors.add(party to balance.customerBalance)
                } else if (balance.customerBalance < 0L) {
                    totalCustomerCredits += (-balance.customerBalance)
                }
            }

            if (party.partyType == PartyType.SUPPLIER.value || party.partyType == PartyType.BOTH.value) {
                if (balance.supplierBalance > 0L) {
                    totalSupplierPayable += balance.supplierBalance
                    supplierCreditors.add(party to balance.supplierBalance)
                } else if (balance.supplierBalance < 0L) {
                    totalSupplierAdvances += (-balance.supplierBalance)
                }
            }
        }

        // Transactions in period for this currency
        var newCustomerDebts = 0L
        var customerPayments = 0L
        var newSupplierDebts = 0L
        var supplierPayments = 0L
        var periodTxCount = 0

        allTxs.filter { it.currencyCode == currencyCode && it.occurredAt in fromTimestamp..toTimestamp }
            .forEach { tx ->
                periodTxCount++
                when (TransactionType.from(tx.transactionType)) {
                    TransactionType.CUSTOMER_DEBT, TransactionType.OPENING_RECEIVABLE -> newCustomerDebts += tx.amountMinor
                    TransactionType.CUSTOMER_PAYMENT -> customerPayments += tx.amountMinor
                    TransactionType.SUPPLIER_DEBT, TransactionType.OPENING_PAYABLE -> newSupplierDebts += tx.amountMinor
                    TransactionType.SUPPLIER_PAYMENT -> supplierPayments += tx.amountMinor
                }
            }

        return TransactionReportSummary(
            currencyCode = currencyCode,
            decimalPlaces = decimals,
            totalCustomerReceivableBalance = totalCustomerReceivable,
            totalSupplierPayableBalance = totalSupplierPayable,
            newCustomerDebtsInPeriod = newCustomerDebts,
            customerPaymentsInPeriod = customerPayments,
            newSupplierDebtsInPeriod = newSupplierDebts,
            supplierPaymentsInPeriod = supplierPayments,
            totalCustomerCredits = totalCustomerCredits,
            totalSupplierAdvances = totalSupplierAdvances,
            topDebtorCustomers = customerDebtors.sortedByDescending { it.second }.take(5),
            topCreditorSuppliers = supplierCreditors.sortedByDescending { it.second }.take(5),
            totalTransactionCount = periodTxCount
        )
    }

    // -------------------------------------------------------------
    // STATEMENT GENERATION
    // -------------------------------------------------------------

    suspend fun getStatementData(
        partyId: String,
        currencyCode: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): StatementData? {
        val party = partyDao.getPartyById(partyId) ?: return null
        val currency = getCurrencyByCode(currencyCode)
        val decimals = currency.decimalPlaces

        // Transactions before period to calculate opening balance
        val priorTxs = txDao.getTransactionsBeforeTime(partyId, currencyCode, fromTimestamp)
        val priorBalance = PartyCurrencyBalance.calculateFromTransactions(currencyCode, decimals, priorTxs)
        val isCustomer = party.partyType == PartyType.CUSTOMER.value || party.partyType == PartyType.BOTH.value
        val initialBalance = if (isCustomer) priorBalance.customerBalance else priorBalance.supplierBalance

        // Transactions in period
        val periodTxs = txDao.getTransactionsForPartyPeriod(partyId, currencyCode, fromTimestamp, toTimestamp)

        var running = initialBalance
        var periodDebts = 0L
        var periodPayments = 0L
        val rows = mutableListOf<StatementRow>()

        for (tx in periodTxs) {
            val type = TransactionType.from(tx.transactionType)
            val isDebit = type.isPositiveImpact
            if (isDebit) {
                periodDebts += tx.amountMinor
                running += tx.amountMinor
                rows.add(StatementRow(tx, debitAmount = tx.amountMinor, creditAmount = null, runningBalance = running))
            } else {
                periodPayments += tx.amountMinor
                running -= tx.amountMinor
                rows.add(StatementRow(tx, debitAmount = null, creditAmount = tx.amountMinor, runningBalance = running))
            }
        }

        return StatementData(
            party = party,
            currencyCode = currencyCode,
            decimalPlaces = decimals,
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp,
            openingBalance = initialBalance,
            periodTotalDebts = periodDebts,
            periodTotalPayments = periodPayments,
            closingBalance = running,
            rows = rows
        )
    }

    // -------------------------------------------------------------
    // BACKUP & RESTORE & CSV EXPORT
    // -------------------------------------------------------------

    suspend fun exportBackupData(): BackupData {
        val settings = settingsDao.getSettings()
        val currencies = currencyDao.getAllCurrencies()
        val parties = partyDao.getAllParties()
        val transactions = txDao.getAllTransactions()

        settingsDao.insertOrUpdate(
            (settings ?: AppDatabase.DEFAULT_SETTINGS).copy(
                lastBackupAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        return BackupData(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0.0",
            settings = settings,
            currencies = currencies,
            parties = parties,
            transactions = transactions
        )
    }

    suspend fun restoreBackupData(backup: BackupData): Boolean {
        if (backup.schemaVersion != 1) return false

        return database.withTransaction {
            // Full replace inside safe transaction
            txDao.clearTransactions()
            partyDao.clearParties()
            currencyDao.clearCurrencies()

            if (backup.currencies.isNotEmpty()) {
                currencyDao.insertCurrencies(backup.currencies)
            } else {
                currencyDao.insertCurrencies(AppDatabase.DEFAULT_CURRENCIES)
            }

            if (backup.parties.isNotEmpty()) {
                partyDao.insertParties(backup.parties)
            }

            if (backup.transactions.isNotEmpty()) {
                txDao.insertTransactions(backup.transactions)
            }

            backup.settings?.let { s ->
                settingsDao.insertOrUpdate(
                    s.copy(
                        id = 1,
                        lastBackupAt = backup.exportedAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            true
        }
    }

    suspend fun exportAllTransactionsToCsv(): String {
        val txs = txDao.getAllTransactions()
        val parties = partyDao.getAllParties().associateBy { it.id }

        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append('\uFEFF')
        // Header
        sb.append("Transaction ID,Date,Party Name,Party Type,Transaction Type,Amount,Currency,Note,Created At,Updated At\n")

        for (tx in txs) {
            val party = parties[tx.partyId]
            val partyName = escapeCsv(party?.name ?: "Unknown")
            val partyType = party?.partyType ?: "CUSTOMER"
            val txType = tx.transactionType
            val amountFormatted = com.example.core.formatting.AmountFormatter.formatToInputString(tx.amountMinor, tx.currencyDecimalPlaces)
            val dateStr = com.example.core.formatting.DateFormatter.formatDateTime(tx.occurredAt)
            val note = escapeCsv(tx.note ?: "")
            val createdAtStr = com.example.core.formatting.DateFormatter.formatIsoUtc(tx.createdAt)
            val updatedAtStr = com.example.core.formatting.DateFormatter.formatIsoUtc(tx.updatedAt)

            sb.append("${tx.id},$dateStr,$partyName,$partyType,$txType,$amountFormatted,${tx.currencyCode},$note,$createdAtStr,$updatedAtStr\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    suspend fun resetAllData() {
        database.withTransaction {
            txDao.clearTransactions()
            partyDao.clearParties()
            currencyDao.clearCurrencies()
            currencyDao.insertCurrencies(AppDatabase.DEFAULT_CURRENCIES)
            settingsDao.clearSettings()
            settingsDao.insertOrUpdate(AppDatabase.DEFAULT_SETTINGS)
        }
    }
}
