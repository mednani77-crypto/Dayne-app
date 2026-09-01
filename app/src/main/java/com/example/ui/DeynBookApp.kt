package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.DeynBookLocalizationProvider
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.TransactionType
import com.example.services.ShareHelper
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.parties.AddEditPartyDialog
import com.example.ui.parties.PartiesScreen
import com.example.ui.parties.PartyDetailScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.DeynBookTheme
import com.example.ui.transactions.AddEditTransactionDialog
import kotlinx.coroutines.launch

@Composable
fun DeynBookApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsState()
    val allCurrencies by viewModel.allCurrencies.collectAsState()
    val enabledCurrencies by viewModel.enabledCurrencies.collectAsState()
    val partiesWithBalances by viewModel.partiesWithBalances.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val pendingRestoreBackup by viewModel.pendingRestoreBackup.collectAsState()

    val currentLanguage = remember(settings?.languageCode) {
        AppLanguage.fromCode(settings?.languageCode ?: "ar")
    }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(viewModel::exportBackupToUri)
    }

    val csvCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let(viewModel::exportCsvToUri)
    }

    fun requestBackupDestination() {
        val date = DateFormatter.formatForFileName(System.currentTimeMillis())
        backupCreateLauncher.launch("DeynBook_Backup_$date.deynbook.json")
    }

    fun requestCsvDestination() {
        val date = DateFormatter.formatForFileName(System.currentTimeMillis())
        csvCreateLauncher.launch("DeynBook_Transactions_$date.csv")
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var activePartyId by remember { mutableStateOf<String?>(null) }

    var showPartyDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<PartyEntity?>(null) }
    var reopenTransactionAfterParty by remember { mutableStateOf(false) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<LedgerTransactionEntity?>(null) }
    var preselectedTransactionType by remember { mutableStateOf<TransactionType?>(null) }
    var preselectedPartyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings?.defaultCurrencyCode) {
        settings?.defaultCurrencyCode?.let(viewModel::setSelectedCurrencyCode)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    DeynBookLocalizationProvider(language = currentLanguage) {
        val strings = LocalStrings.current
        DeynBookTheme(themeMode = settings?.themeMode ?: "SYSTEM") {
            if (settings?.onboardingCompleted != true) {
                OnboardingScreen(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = viewModel::setLanguage,
                    onFinishOnboarding = { lang, country, currency, name, phone ->
                        viewModel.configureOnboardingCurrencies(currency)
                        viewModel.completeOnboarding(lang, country, currency, name, phone)
                    }
                )
                return@DeynBookTheme
            }

            val showMainNavigation = activePartyId == null
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showMainNavigation) {
                        NavigationBar {
                            val tabs = listOf(
                                Triple(Icons.Default.Home, strings.navHome, 0),
                                Triple(Icons.Default.People, strings.navParties, 1),
                                Triple(Icons.Default.Assessment, strings.navReports, 2),
                                Triple(Icons.Default.Settings, strings.navMore, 3)
                            )
                            tabs.forEach { (icon, label, index) ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (showMainNavigation) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                transactionToEdit = null
                                preselectedTransactionType = null
                                preselectedPartyId = null
                                showTransactionDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text(strings.addTransaction) }
                        )
                    }
                }
            ) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    val detailId = activePartyId
                    if (detailId != null) {
                        val partyWithBalances = partiesWithBalances.find { it.party.id == detailId }
                        if (partyWithBalances == null) {
                            LaunchedEffect(detailId) { activePartyId = null }
                        } else {
                            val transactions by viewModel.repository
                                .getTransactionsForPartyFlow(detailId)
                                .collectAsState(initial = emptyList())

                            PartyDetailScreen(
                                partyWithBalances = partyWithBalances,
                                transactions = transactions,
                                onBack = { activePartyId = null },
                                onEditParty = {
                                    partyToEdit = partyWithBalances.party
                                    showPartyDialog = true
                                },
                                onArchiveParty = { viewModel.setPartyArchived(detailId, it) },
                                onDeleteParty = {
                                    viewModel.deleteParty(
                                        partyId = detailId,
                                        onSuccess = { activePartyId = null },
                                        onError = {
                                            scope.launch { snackbarHostState.showSnackbar(strings.cannotDeletePartyWithTx) }
                                        }
                                    )
                                },
                                onCallParty = { ShareHelper.dialPhoneNumber(context, it) },
                                onShareStatementPdf = { currency, accountType ->
                                    viewModel.shareStatementPdf(detailId, currency, accountType, currentLanguage)
                                },
                                onShareImageCard = { currency, accountType ->
                                    viewModel.shareImageCard(detailId, currency, accountType, currentLanguage)
                                },
                                onShareTextSummary = { currency, accountType ->
                                    viewModel.shareTextSummary(detailId, currency, accountType, currentLanguage)
                                },
                                onAddTransaction = { type ->
                                    transactionToEdit = null
                                    preselectedTransactionType = type
                                    preselectedPartyId = detailId
                                    showTransactionDialog = true
                                },
                                onEditTransaction = { tx ->
                                    transactionToEdit = tx
                                    preselectedTransactionType = null
                                    preselectedPartyId = tx.partyId
                                    showTransactionDialog = true
                                },
                                onDeleteTransaction = { viewModel.deleteTransaction(it.id) }
                            )
                        }
                    } else {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                businessName = settings?.businessName.orEmpty(),
                                selectedCurrencyCode = selectedCurrencyCode,
                                currencies = enabledCurrencies.ifEmpty { allCurrencies },
                                summary = dashboardSummary,
                                recentTransactions = recentTransactions,
                                onSelectCurrency = viewModel::setSelectedCurrencyCode,
                                onQuickAction = { type ->
                                    transactionToEdit = null
                                    preselectedTransactionType = type
                                    preselectedPartyId = null
                                    showTransactionDialog = true
                                },
                                onTransactionClick = { tx ->
                                    transactionToEdit = tx
                                    preselectedPartyId = tx.partyId
                                    showTransactionDialog = true
                                },
                                onNavigateToParties = { selectedTab = 1 },
                                onSearchClick = { selectedTab = 1 }
                            )

                            1 -> PartiesScreen(
                                parties = partiesWithBalances,
                                onPartyClick = { activePartyId = it },
                                onAddPartyClick = {
                                    partyToEdit = null
                                    reopenTransactionAfterParty = false
                                    showPartyDialog = true
                                }
                            )

                            2 -> ReportsScreen(
                                defaultCurrencyCode = selectedCurrencyCode,
                                currencies = enabledCurrencies.ifEmpty { allCurrencies },
                                parties = partiesWithBalances,
                                onLoadReport = { currency, from, to, partyId ->
                                    viewModel.loadReportSummary(currency, from, to, partyId)
                                },
                                onExportCsv = ::requestCsvDestination
                            )

                            else -> settings?.let { currentSettings ->
                                SettingsScreen(
                                    settings = currentSettings,
                                    currencies = allCurrencies,
                                    currentLanguage = currentLanguage,
                                    onUpdateSettings = viewModel::updateSettings,
                                    onLanguageChange = viewModel::setLanguage,
                                    onToggleCurrency = viewModel::toggleCurrency,
                                    onAddCustomCurrency = viewModel::addCustomCurrency,
                                    onExportBackup = ::requestBackupDestination,
                                    onImportBackupFile = viewModel::parseBackupFromUri,
                                    onExportCsv = ::requestCsvDestination,
                                    onResetAllData = viewModel::resetAllData
                                )
                            }
                        }
                    }
                }
            }

            if (showPartyDialog) {
                AddEditPartyDialog(
                    initialParty = partyToEdit,
                    defaultCurrencyCode = selectedCurrencyCode,
                    currencies = enabledCurrencies.ifEmpty { allCurrencies },
                    onDismiss = {
                        showPartyDialog = false
                        partyToEdit = null
                        reopenTransactionAfterParty = false
                    },
                    onSearchSimilar = viewModel::searchSimilarParties,
                    onSave = { name, phone, type, notes, openingType, openingAmount, currency, decimals, openingOccurredAt ->
                        val editing = partyToEdit
                        if (editing != null) {
                            viewModel.updateParty(
                                editing.copy(
                                    name = name,
                                    normalizedName = name.trim().lowercase(),
                                    phone = phone,
                                    partyType = type.value,
                                    notes = notes
                                )
                            )
                            showPartyDialog = false
                            partyToEdit = null
                        } else {
                            viewModel.createPartyWithCallback(
                                name,
                                phone,
                                type,
                                notes,
                                openingType,
                                openingAmount,
                                currency,
                                decimals,
                                openingOccurredAt
                            ) { newPartyId ->
                                showPartyDialog = false
                                partyToEdit = null
                                if (reopenTransactionAfterParty) {
                                    preselectedPartyId = newPartyId
                                    reopenTransactionAfterParty = false
                                    showTransactionDialog = true
                                }
                            }
                        }
                    }
                )
            }

            if (showTransactionDialog) {
                AddEditTransactionDialog(
                    initialTransaction = transactionToEdit,
                    preselectedType = preselectedTransactionType,
                    preselectedPartyId = preselectedPartyId,
                    defaultCurrencyCode = selectedCurrencyCode,
                    partiesWithBalances = partiesWithBalances,
                    currencies = enabledCurrencies.ifEmpty { allCurrencies },
                    onDismiss = {
                        showTransactionDialog = false
                        transactionToEdit = null
                        preselectedTransactionType = null
                        preselectedPartyId = null
                    },
                    onOpenAddParty = {
                        showTransactionDialog = false
                        reopenTransactionAfterParty = true
                        partyToEdit = null
                        showPartyDialog = true
                    },
                    onSaveTransaction = { partyId, type, amount, currency, decimals, occurredAt, note ->
                        val existing = transactionToEdit
                        if (existing == null) {
                            viewModel.addTransaction(partyId, type, amount, currency, decimals, occurredAt, note)
                        } else {
                            viewModel.updateTransaction(existing.id, partyId, type, amount, currency, decimals, occurredAt, note)
                        }
                        showTransactionDialog = false
                        transactionToEdit = null
                        preselectedTransactionType = null
                        preselectedPartyId = null
                    }
                )
            }

            pendingRestoreBackup?.let { backup ->
                AlertDialog(
                    onDismissRequest = viewModel::cancelRestoreBackup,
                    title = {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Text(strings.restorePreviewTitle)
                    },
                    text = {
                        Text(
                            "${strings.restoreWarningMessage}\n" +
                                "${strings.lastBackupDate} ${DateFormatter.formatDateTime(backup.exportedAt)}\n" +
                                "${strings.partiesTitle}: ${backup.parties.size}\n" +
                                "${strings.totalTxCount}: ${backup.transactions.size}"
                        )
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.confirmRestoreBackup(backup) }) {
                            Text(strings.restoreBackup)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::cancelRestoreBackup) { Text(strings.cancel) }
                    }
                )
            }
        }
    }
}
