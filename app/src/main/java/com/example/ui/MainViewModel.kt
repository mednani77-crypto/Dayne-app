package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.localization.AppLanguage
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import com.example.data.models.BackupData
import com.example.data.models.DashboardSummary
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.data.models.StatementData
import com.example.data.models.TransactionReportSummary
import com.example.data.models.TransactionType
import com.example.data.models.TransactionWithParty
import com.example.data.repository.DeynBookRepository
import com.example.services.ImageShareService
import com.example.services.PdfStatementService
import com.example.services.ShareHelper
import com.example.services.StatementAccountType
import com.example.services.StatementBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = DeynBookRepository(database)

    val settings: StateFlow<SettingsEntity?> = repository.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allCurrencies: StateFlow<List<CurrencyEntity>> = repository.getAllCurrenciesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val enabledCurrencies: StateFlow<List<CurrencyEntity>> = repository.getEnabledCurrenciesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val partiesWithBalances: StateFlow<List<PartyWithBalances>> = repository.getAllPartiesWithBalancesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentTransactions: StateFlow<List<TransactionWithParty>> = repository.getRecentTransactionsWithPartyFlow(25)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedCurrencyCode = MutableStateFlow("DJF")
    val selectedCurrencyCode: StateFlow<String> = _selectedCurrencyCode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dashboardSummary: StateFlow<DashboardSummary> = _selectedCurrencyCode
        .flatMapLatest { code -> repository.getDashboardSummaryFlow(code) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            DashboardSummary("DJF", 0, 0L, 0L, 0, 0)
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _pendingRestoreBackup = MutableStateFlow<BackupData?>(null)
    val pendingRestoreBackup: StateFlow<BackupData?> = _pendingRestoreBackup.asStateFlow()

    fun setSelectedCurrencyCode(code: String) {
        _selectedCurrencyCode.value = code
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun completeOnboarding(
        language: AppLanguage,
        countryCode: String,
        currencyCode: String,
        businessName: String,
        businessPhone: String?
    ) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(
                current.copy(
                    businessName = businessName,
                    businessPhone = businessPhone,
                    countryCode = countryCode,
                    languageCode = language.code,
                    defaultCurrencyCode = currencyCode,
                    onboardingCompleted = true
                )
            )
            setSelectedCurrencyCode(currencyCode)
        }
    }

    fun updateSettings(newSettings: SettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            if (newSettings.defaultCurrencyCode != _selectedCurrencyCode.value) {
                setSelectedCurrencyCode(newSettings.defaultCurrencyCode)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(languageCode = language.code))
        }
    }

    fun toggleCurrency(code: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettings()
            if (!isEnabled && current.defaultCurrencyCode == code) return@launch
            repository.setCurrencyEnabled(code, isEnabled)
        }
    }

    fun addCustomCurrency(code: String, name: String, decimals: Int) {
        viewModelScope.launch {
            val cleanCode = code.trim().uppercase()
            if (cleanCode.isBlank() || name.trim().isBlank() || decimals !in 0..2) return@launch
            val entity = CurrencyEntity(
                code = cleanCode,
                name = name.trim(),
                decimalPlaces = decimals,
                isCustom = true,
                isEnabled = true
            )
            repository.saveCurrency(entity)
        }
    }

    suspend fun searchSimilarParties(name: String): List<PartyEntity> = repository.searchSimilarParties(name)

    fun createParty(
        name: String,
        phone: String?,
        partyType: PartyType,
        notes: String?,
        openingType: String,
        openingAmountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int
    ) {
        viewModelScope.launch {
            if (name.trim().isBlank() || openingAmountMinor < 0L) return@launch
            repository.createPartyWithOptionalOpeningBalance(
                name = name,
                phone = phone,
                partyType = partyType,
                notes = notes,
                openingBalanceType = openingType,
                openingAmountMinor = openingAmountMinor,
                currencyCode = currencyCode,
                currencyDecimalPlaces = currencyDecimalPlaces
            )
        }
    }

    fun updateParty(party: PartyEntity) {
        viewModelScope.launch {
            if (party.name.trim().isBlank()) return@launch
            repository.updateParty(party)
        }
    }

    fun setPartyArchived(partyId: String, isArchived: Boolean) {
        viewModelScope.launch { repository.setPartyArchived(partyId, isArchived) }
    }

    fun deleteParty(partyId: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            if (repository.deletePartyIfNoTransactions(partyId)) onSuccess() else onError()
        }
    }

    fun addTransaction(
        partyId: String,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int,
        occurredAt: Long,
        note: String?
    ) {
        viewModelScope.launch {
            if (!isTransactionValidForParty(partyId, type) || amountMinor <= 0L) return@launch
            repository.addTransaction(
                partyId = partyId,
                type = type,
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                currencyDecimalPlaces = currencyDecimalPlaces,
                occurredAt = occurredAt,
                note = note
            )
        }
    }

    fun updateTransaction(
        id: String,
        partyId: String,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int,
        occurredAt: Long,
        note: String?
    ) {
        viewModelScope.launch {
            if (!isTransactionValidForParty(partyId, type) || amountMinor <= 0L) return@launch
            repository.updateTransaction(
                id = id,
                partyId = partyId,
                type = type,
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                currencyDecimalPlaces = currencyDecimalPlaces,
                occurredAt = occurredAt,
                note = note
            )
        }
    }

    private suspend fun isTransactionValidForParty(partyId: String, type: TransactionType): Boolean {
        val party = repository.getPartyWithBalancesById(partyId)?.party ?: return false
        val partyType = PartyType.from(party.partyType)
        return if (type.isReceivableImpact) {
            partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
        } else {
            partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    suspend fun loadReportSummary(currencyCode: String, fromTime: Long, toTime: Long): TransactionReportSummary {
        return repository.getReportSummary(currencyCode, fromTime, toTime)
    }

    suspend fun buildStatementData(
        partyId: String,
        currencyCode: String,
        accountType: StatementAccountType,
        fromTime: Long,
        toTime: Long
    ): StatementData? {
        val partyWithBalances = repository.getPartyWithBalancesById(partyId) ?: return null
        val currency = repository.getCurrencyByCode(currencyCode)
        val transactions = repository.getTransactionsForPartyFlow(partyId).first()
        return StatementBuilder.build(
            party = partyWithBalances.party,
            transactions = transactions,
            accountType = accountType,
            currencyCode = currencyCode,
            decimalPlaces = currency.decimalPlaces,
            fromTimestamp = fromTime,
            toTimestamp = toTime
        )
    }

    fun shareStatementPdf(
        partyId: String,
        currencyCode: String,
        accountType: StatementAccountType,
        language: AppLanguage
    ) {
        viewModelScope.launch {
            val partyWithBalances = repository.getPartyWithBalancesById(partyId) ?: return@launch
            val fromTime = partyWithBalances.party.createdAt
            val toTime = System.currentTimeMillis()
            val statementData = buildStatementData(partyId, currencyCode, accountType, fromTime, toTime) ?: return@launch
            val businessName = repository.getSettings().businessName

            val file = PdfStatementService.generateStatementPdf(
                context = getApplication(),
                businessName = businessName,
                statementData = statementData,
                accountType = accountType,
                language = language
            )

            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "application/pdf",
                title = "DeynBook"
            )
        }
    }

    fun shareImageCard(
        partyId: String,
        currencyCode: String,
        accountType: StatementAccountType,
        language: AppLanguage
    ) {
        viewModelScope.launch {
            val partyWithBal = repository.getPartyWithBalancesById(partyId) ?: return@launch
            val decimals = repository.getCurrencyByCode(currencyCode).decimalPlaces
            val balance = partyWithBal.getBalanceForCurrency(currencyCode)
                ?: com.example.data.models.PartyCurrencyBalance(currencyCode, decimals)
            val businessName = repository.getSettings().businessName
            val allTxs = repository.getTransactionsForPartyFlow(partyId).first()
            val recentTxs = allTxs
                .filter { it.currencyCode == currencyCode }
                .filter {
                    if (accountType == StatementAccountType.CUSTOMER) {
                        TransactionType.from(it.transactionType).isReceivableImpact
                    } else {
                        !TransactionType.from(it.transactionType).isReceivableImpact
                    }
                }
                .sortedByDescending { it.occurredAt }
                .take(5)

            val file = ImageShareService.generateSummaryCardImage(
                context = getApplication(),
                businessName = businessName,
                party = partyWithBal.party,
                balance = balance,
                accountType = accountType,
                recentTransactions = recentTxs,
                language = language
            )

            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "image/png",
                title = "DeynBook"
            )
        }
    }

    fun shareTextSummary(
        partyId: String,
        currencyCode: String,
        accountType: StatementAccountType,
        language: AppLanguage
    ) {
        viewModelScope.launch {
            val partyWithBal = repository.getPartyWithBalancesById(partyId) ?: return@launch
            val decimals = repository.getCurrencyByCode(currencyCode).decimalPlaces
            val balance = partyWithBal.getBalanceForCurrency(currencyCode)
                ?: com.example.data.models.PartyCurrencyBalance(currencyCode, decimals)
            val businessName = repository.getSettings().businessName

            ShareHelper.shareTextSummary(
                context = getApplication(),
                businessName = businessName,
                party = partyWithBal.party,
                balance = balance,
                accountType = accountType,
                language = language
            )
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            val backup = repository.exportBackupData()
            val file = File(getApplication<Application>().cacheDir, "DeynBook_Backup_${backup.exportedAt}.deynbook.json")
            writeUtf8(file, backup.toJsonString())
            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "application/json",
                title = "DeynBook Backup"
            )
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val csv = repository.exportAllTransactionsToCsv()
            val file = File(getApplication<Application>().cacheDir, "DeynBook_Transactions_${System.currentTimeMillis()}.csv")
            writeUtf8(file, csv)
            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "text/csv",
                title = "DeynBook CSV"
            )
        }
    }

    fun parseBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonStr = getApplication<Application>().contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: return@launch
                val backup = BackupData.fromJsonString(jsonStr)
                if (backup.schemaVersion != 1) {
                    _userMessage.value = "Unsupported backup version"
                    return@launch
                }
                _pendingRestoreBackup.value = backup
            } catch (_: Exception) {
                _userMessage.value = "Invalid backup file"
            }
        }
    }

    fun confirmRestoreBackup(backup: BackupData) {
        viewModelScope.launch {
            try {
                // Always preserve a recoverable local copy before destructive replacement.
                val current = repository.exportBackupData()
                val safetyDir = File(getApplication<Application>().filesDir, "safety_backups").apply { mkdirs() }
                val safetyFile = File(safetyDir, "before_restore_${System.currentTimeMillis()}.deynbook.json")
                writeUtf8(safetyFile, current.toJsonString())

                val success = repository.restoreBackupData(backup)
                _pendingRestoreBackup.value = null
                _userMessage.value = if (success) "Restore completed" else "Restore failed"
            } catch (_: Exception) {
                _pendingRestoreBackup.value = null
                _userMessage.value = "Restore failed"
            }
        }
    }

    fun cancelRestoreBackup() {
        _pendingRestoreBackup.value = null
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _selectedCurrencyCode.value = "DJF"
        }
    }

    private fun writeUtf8(file: File, content: String) {
        FileOutputStream(file).use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }
}
