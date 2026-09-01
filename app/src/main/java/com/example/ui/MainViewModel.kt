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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        .flatMapLatest { code ->
            repository.getDashboardSummaryFlow(code)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            DashboardSummary("DJF", 0, 0L, 0L, 0, 0)
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Backup restore preview state
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
            repository.setCurrencyEnabled(code, isEnabled)
        }
    }

    fun addCustomCurrency(code: String, name: String, decimals: Int) {
        viewModelScope.launch {
            val entity = CurrencyEntity(
                code = code,
                name = name,
                decimalPlaces = decimals,
                isCustom = true,
                isEnabled = true
            )
            repository.saveCurrency(entity)
        }
    }

    suspend fun searchSimilarParties(name: String): List<PartyEntity> {
        return repository.searchSimilarParties(name)
    }

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
            repository.updateParty(party)
        }
    }

    fun setPartyArchived(partyId: String, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setPartyArchived(partyId, isArchived)
        }
    }

    fun deleteParty(partyId: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val deleted = repository.deletePartyIfNoTransactions(partyId)
            if (deleted) {
                onSuccess()
            } else {
                onError()
            }
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

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    suspend fun loadReportSummary(currencyCode: String, fromTime: Long, toTime: Long): TransactionReportSummary {
        return repository.getReportSummary(currencyCode, fromTime, toTime)
    }

    suspend fun getStatementData(partyId: String, currencyCode: String, fromTime: Long, toTime: Long): StatementData? {
        return repository.getStatementData(partyId, currencyCode, fromTime, toTime)
    }

    fun shareStatementPdf(partyId: String, currencyCode: String, language: AppLanguage) {
        viewModelScope.launch {
            val partyWithBal = repository.getPartyWithBalancesById(partyId) ?: return@launch
            val fromTime = partyWithBal.party.createdAt
            val toTime = System.currentTimeMillis()

            val statementData = repository.getStatementData(partyId, currencyCode, fromTime, toTime) ?: return@launch
            val businessName = repository.getSettings().businessName

            val file = PdfStatementService.generateStatementPdf(
                context = getApplication(),
                businessName = businessName,
                statementData = statementData,
                language = language
            )

            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "application/pdf",
                title = "Share Statement PDF"
            )
        }
    }

    fun shareImageCard(partyId: String, currencyCode: String, language: AppLanguage) {
        viewModelScope.launch {
            val partyWithBal = repository.getPartyWithBalancesById(partyId) ?: return@launch
            val decimals = repository.getCurrencyByCode(currencyCode).decimalPlaces
            val balance = partyWithBal.getBalanceForCurrency(currencyCode) ?: com.example.data.models.PartyCurrencyBalance(currencyCode, decimals, 0L, 0L, 0L, 0L)

            val txs = repository.getTransactionsForPartyFlow(partyId)
            val businessName = repository.getSettings().businessName

            // fetch transactions
            val statementData = repository.getStatementData(partyId, currencyCode, 0L, System.currentTimeMillis())
            val recentTxs = statementData?.rows?.map { it.transaction }?.reversed() ?: emptyList()

            val file = ImageShareService.generateSummaryCardImage(
                context = getApplication(),
                businessName = businessName,
                party = partyWithBal.party,
                balance = balance,
                recentTransactions = recentTxs,
                language = language
            )

            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "image/png",
                title = "Share Summary Card"
            )
        }
    }

    fun shareTextSummary(partyId: String, currencyCode: String, language: AppLanguage) {
        viewModelScope.launch {
            val partyWithBal = repository.getPartyWithBalancesById(partyId) ?: return@launch
            val decimals = repository.getCurrencyByCode(currencyCode).decimalPlaces
            val balance = partyWithBal.getBalanceForCurrency(currencyCode) ?: com.example.data.models.PartyCurrencyBalance(currencyCode, decimals, 0L, 0L, 0L, 0L)
            val businessName = repository.getSettings().businessName

            ShareHelper.shareTextSummary(
                context = getApplication(),
                businessName = businessName,
                party = partyWithBal.party,
                balance = balance,
                language = language
            )
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            val backup = repository.exportBackupData()
            val jsonStr = backup.toJsonString()

            val file = File(getApplication<Application>().cacheDir, "DeynBook_Backup_${backup.exportedAt}.json")
            val fos = FileOutputStream(file)
            fos.write(jsonStr.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()

            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "application/json",
                title = "Export DeynBook Backup"
            )
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val csv = repository.exportAllTransactionsToCsv()
            val file = File(getApplication<Application>().cacheDir, "DeynBook_Transactions_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(csv.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()

            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "text/csv",
                title = "Export CSV Report"
            )
        }
    }

    fun parseBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val jsonStr = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (jsonStr != null) {
                    val backup = BackupData.fromJsonString(jsonStr)
                    _pendingRestoreBackup.value = backup
                }
            } catch (_: Exception) {
                _userMessage.value = "Failed to parse backup file"
            }
        }
    }

    fun confirmRestoreBackup(backup: BackupData) {
        viewModelScope.launch {
            val success = repository.restoreBackupData(backup)
            _pendingRestoreBackup.value = null
            if (success) {
                _userMessage.value = "تم استرجاع البيانات بنجاح"
            } else {
                _userMessage.value = "فشل في استرجاع البيانات"
            }
        }
    }

    fun cancelRestoreBackup() {
        _pendingRestoreBackup.value = null
    }
}
