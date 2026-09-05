package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.localization.DeynBookLocalizationProvider
import com.example.core.localization.FeatureStringsProvider
import com.example.core.localization.LocalStrings
import com.example.core.localization.V12StringsProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.CollectionItem
import com.example.data.models.CollectionStatus
import com.example.data.models.DashboardSummary
import com.example.data.models.TransactionType
import com.example.data.repository.DeynBookV12Repository
import com.example.services.AttachmentService
import com.example.services.ReportExportOptions
import com.example.services.ReportExportResult
import com.example.services.ShareHelper
import com.example.services.StatementAccountType
import com.example.services.V12BackupPackage
import com.example.services.V12BackupService
import com.example.ui.activity.ActivityScreen
import com.example.ui.collection.CollectionCenterScreen
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.parties.AddEditPartyDialog
import com.example.ui.parties.PartiesScreen
import com.example.ui.parties.PartyDetailScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.reports.ReportExportDialog
import com.example.ui.reports.reportExportResultMessage
import com.example.ui.security.BiometricGate
import com.example.ui.settings.LedgerManagerScreen
import com.example.ui.settings.MoreHubV12
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.DeynBookTheme
import com.example.ui.transactions.AddEditTransactionDialog
import com.example.ui.transactions.AttachmentManagerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private enum class ReportExportKind { STATEMENT_PDF, IMAGE_CARD, TEXT_SUMMARY, CSV, EXCEL }

private data class PendingReportExport(
    val kind: ReportExportKind,
    val partyId: String? = null,
    val currencyCode: String? = null,
    val accountType: StatementAccountType? = null,
    val fromTime: Long = Long.MIN_VALUE,
    val toTime: Long = Long.MAX_VALUE
)

@Composable
fun DeynBookAppV12(
    viewModel: MainViewModel = viewModel(),
    sharedUri: Uri? = null,
    sharedMimeType: String? = null,
    onSharedConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val database = remember { AppDatabase.getInstance(context.applicationContext) }
    val v12Repository = remember { DeynBookV12Repository(database) }

    val settings by viewModel.settings.collectAsState()
    val allCurrencies by viewModel.allCurrencies.collectAsState()
    val enabledCurrencies by viewModel.enabledCurrencies.collectAsState()
    val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val ledgers by v12Repository.getLedgersFlow().collectAsState(initial = emptyList())
    val activeLedger by v12Repository.getActiveLedgerFlow().collectAsState(initial = null)
    val scopedParties by v12Repository.getScopedPartiesWithBalancesFlow().collectAsState(initial = emptyList())
    val scopedRecent by v12Repository.getScopedRecentTransactionsFlow(25).collectAsState(initial = emptyList())
    val activityTransactions by v12Repository.getScopedRecentTransactionsFlow(250).collectAsState(initial = emptyList())
    val smartDashboardFlow = remember(selectedCurrencyCode, activeLedger?.id) {
        v12Repository.getSmartDashboardFlow(selectedCurrencyCode)
    }
    val smartDashboard by smartDashboardFlow.collectAsState(
        initial = DashboardSummary(selectedCurrencyCode, 0, 0L, 0L, 0, 0)
    )

    val currentLanguage = remember(settings?.languageCode) {
        AppLanguage.fromCode(settings?.languageCode ?: "ar")
    }
    val nonComposableStrings = remember(currentLanguage) { AppStrings.get(currentLanguage) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var activePartyId by remember { mutableStateOf<String?>(null) }
    var showCollectionCenter by remember { mutableStateOf(false) }
    var showLedgerManager by remember { mutableStateOf(false) }
    var showActivity by remember { mutableStateOf(false) }
    var showGeneralSettings by remember { mutableStateOf(false) }
    var collectionItems by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }

    var showPartyDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<PartyEntity?>(null) }
    var reopenTransactionAfterParty by remember { mutableStateOf(false) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<LedgerTransactionEntity?>(null) }
    var preselectedTransactionType by remember { mutableStateOf<TransactionType?>(null) }
    var preselectedPartyId by remember { mutableStateOf<String?>(null) }
    var transactionForAttachments by remember { mutableStateOf<LedgerTransactionEntity?>(null) }

    var pendingSharedUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestore by remember { mutableStateOf<V12BackupPackage?>(null) }
    var pendingExcelRange by remember { mutableStateOf(Long.MIN_VALUE to Long.MAX_VALUE) }
    var pendingExcelOptions by remember(currentLanguage) {
        mutableStateOf(ReportExportOptions(currentLanguage))
    }
    var pendingReportExport by remember { mutableStateOf<PendingReportExport?>(null) }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val backup = V12BackupService.export(database)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write(backup.toJsonString())
                    }
                }
                snackbarHostState.showSnackbar(nonComposableStrings.backupSuccess)
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(nonComposableStrings.errorGeneric)
            }
        }
    }

    val excelCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri ->
        if (uri != null) viewModel.exportExcelV12(
            uri,
            pendingExcelOptions,
            pendingExcelRange.first,
            pendingExcelRange.second
        ) { result ->
            scope.launch { snackbarHostState.showSnackbar(reportExportResultMessage(currentLanguage, result)) }
        }
    }

    val excelTemplateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri -> if (uri != null) viewModel.exportExcelImportTemplate(uri, currentLanguage) }

    val excelImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importExcelV12(uri) { result ->
                scope.launch {
                    val t = V12StringsProvider.get(currentLanguage)
                    if (result == null) {
                        snackbarHostState.showSnackbar(t.excelImportFailed)
                    } else {
                        snackbarHostState.showSnackbar(
                            "${t.excelImportSuccess}: ${t.importedAccounts} ${result.accountsCreated}, ${t.importedTransactions} ${result.transactionsCreated}" +
                                if (result.warnings.isNotEmpty()) " • ${result.warnings.size}" else ""
                        )
                    }
                }
            }
        }
    }

    fun requestExcelExport(from: Long = Long.MIN_VALUE, to: Long = Long.MAX_VALUE) {
        pendingReportExport = PendingReportExport(
            kind = ReportExportKind.EXCEL,
            fromTime = from,
            toTime = to
        )
    }

    fun parseBackup(uri: Uri) {
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Empty backup")
                }
                pendingRestore = V12BackupService.parse(json)
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(nonComposableStrings.restoreInvalidFile)
            }
        }
    }

    LaunchedEffect(settings?.defaultCurrencyCode) {
        settings?.defaultCurrencyCode?.let(viewModel::setSelectedCurrencyCode)
    }
    LaunchedEffect(settings?.calendarMode) {
        DateFormatter.setCalendarMode(settings?.calendarMode ?: DateFormatter.CALENDAR_GREGORIAN)
    }
    LaunchedEffect(settings?.onboardingCompleted, settings?.businessName) {
        if (settings?.onboardingCompleted == true) v12Repository.syncDefaultLedgerFromSettings()
    }
    LaunchedEffect(scopedParties, activityTransactions) {
        collectionItems = viewModel.loadCollectionItems()
    }
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }
    LaunchedEffect(sharedUri, settings?.onboardingCompleted) {
        if (sharedUri != null && settings?.onboardingCompleted == true) {
            pendingSharedUri = sharedUri
            transactionToEdit = null
            preselectedPartyId = null
            preselectedTransactionType = TransactionType.CUSTOMER_DEBT
            showTransactionDialog = true
            snackbarHostState.showSnackbar(V12StringsProvider.get(currentLanguage).sharedToDeynBook)
        }
    }

    BackHandler(enabled = showGeneralSettings) { showGeneralSettings = false }
    BackHandler(enabled = showLedgerManager) { showLedgerManager = false }
    BackHandler(enabled = showActivity) { showActivity = false }
    BackHandler(enabled = showCollectionCenter) { showCollectionCenter = false }
    BackHandler(enabled = activePartyId != null) { activePartyId = null }

    DeynBookLocalizationProvider(language = currentLanguage) {
        val strings = LocalStrings.current
        val featureStrings = FeatureStringsProvider.get(currentLanguage)
        val v12Strings = V12StringsProvider.get(currentLanguage)

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

            BiometricGate(
                enabled = settings?.biometricLockEnabled == true,
                language = currentLanguage
            ) {
                val showMainNavigation = activePartyId == null &&
                    !showCollectionCenter && !showLedgerManager && !showActivity && !showGeneralSettings

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showMainNavigation) {
                            NavigationBar {
                                listOf(
                                    Triple(Icons.Default.Home, strings.navHome, 0),
                                    Triple(Icons.Default.People, strings.navParties, 1),
                                    Triple(Icons.Default.Assessment, strings.navReports, 2),
                                    Triple(Icons.Default.Settings, strings.navMore, 3)
                                ).forEach { (icon, label, index) ->
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
                        if (showMainNavigation && selectedTab == 0) {
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
                        when {
                            showLedgerManager -> LedgerManagerScreen(
                                ledgers = ledgers,
                                activeLedgerId = activeLedger?.id ?: settings?.activeLedgerId.orEmpty(),
                                currencies = allCurrencies,
                                language = currentLanguage,
                                onBack = { showLedgerManager = false },
                                onSwitch = { id ->
                                    scope.launch {
                                        if (v12Repository.switchLedger(id)) {
                                            activePartyId = null
                                            collectionItems = viewModel.loadCollectionItems()
                                            showLedgerManager = false
                                        }
                                    }
                                },
                                onCreate = { name, country, currency, phone, address ->
                                    scope.launch {
                                        val ledger = v12Repository.createLedger(name, country, currency, phone, address)
                                        viewModel.setSelectedCurrencyCode(ledger.defaultCurrencyCode)
                                    }
                                },
                                onUpdate = { ledger -> scope.launch { v12Repository.updateLedger(ledger) } },
                                onArchive = { id, archived ->
                                    scope.launch {
                                        if (!v12Repository.archiveLedger(id, archived)) {
                                            snackbarHostState.showSnackbar(v12Strings.cannotDeleteLedger)
                                        }
                                    }
                                },
                                onDelete = { id ->
                                    scope.launch {
                                        if (!v12Repository.deleteLedgerIfEmpty(id)) {
                                            snackbarHostState.showSnackbar(v12Strings.cannotDeleteLedger)
                                        }
                                    }
                                },
                                onLogoSelected = { id, uri ->
                                    viewModel.saveLedgerLogoV12(id, uri) { ok ->
                                        if (!ok) scope.launch { snackbarHostState.showSnackbar(strings.errorGeneric) }
                                    }
                                }
                            )

                            showActivity -> ActivityScreen(
                                transactions = activityTransactions,
                                language = currentLanguage,
                                onBack = { showActivity = false },
                                onOpenParty = { id -> showActivity = false; activePartyId = id }
                            )

                            showCollectionCenter -> CollectionCenterScreen(
                                items = collectionItems,
                                onBack = { showCollectionCenter = false },
                                onOpenParty = { item -> showCollectionCenter = false; activePartyId = item.party.id },
                                onWhatsAppReminder = { item ->
                                    val amount = AmountFormatter.formatAmount(
                                        item.outstandingAmountMinor,
                                        item.decimalPlaces,
                                        item.currencyCode
                                    )
                                    ShareHelper.openWhatsAppReminder(
                                        context,
                                        item.party.phone,
                                        featureStrings.reminderMessage(
                                            item.party.name,
                                            amount,
                                            DateFormatter.formatDate(item.dueAt)
                                        )
                                    )
                                },
                                onRecordPayment = { item ->
                                    transactionToEdit = null
                                    preselectedTransactionType = TransactionType.CUSTOMER_PAYMENT
                                    preselectedPartyId = item.party.id
                                    showTransactionDialog = true
                                }
                            )

                            showGeneralSettings -> settings?.let { currentSettings ->
                                SettingsScreen(
                                    settings = currentSettings,
                                    currencies = allCurrencies,
                                    currentLanguage = currentLanguage,
                                    onUpdateSettings = { newSettings ->
                                        viewModel.updateSettings(newSettings)
                                        activeLedger?.let { ledger ->
                                            scope.launch {
                                                v12Repository.updateLedger(
                                                    ledger.copy(
                                                        name = newSettings.businessName,
                                                        phone = newSettings.businessPhone,
                                                        address = newSettings.businessAddress,
                                                        countryCode = newSettings.countryCode,
                                                        defaultCurrencyCode = newSettings.defaultCurrencyCode
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onLanguageChange = viewModel::setLanguage,
                                    onToggleCurrency = viewModel::toggleCurrency,
                                    onAddCustomCurrency = viewModel::addCustomCurrency,
                                    onExportBackup = {
                                        backupCreateLauncher.launch(
                                            "DeynBook_Backup_${DateFormatter.formatForFileName(System.currentTimeMillis())}.deynbook.json"
                                        )
                                    },
                                    onImportBackupFile = ::parseBackup,
                                    onExportCsv = {
                                        pendingReportExport = PendingReportExport(
                                            kind = ReportExportKind.CSV
                                        )
                                    },
                                    onResetAllData = { viewModel.resetEverythingV12 { showGeneralSettings = false } }
                                )
                            }

                            activePartyId != null -> {
                                val id = activePartyId!!
                                val partyWithBalances = scopedParties.find { it.party.id == id }
                                if (partyWithBalances == null) {
                                    LaunchedEffect(id) { activePartyId = null }
                                } else {
                                    val transactions by viewModel.repository
                                        .getTransactionsForPartyFlow(id)
                                        .collectAsState(initial = emptyList())
                                    PartyDetailScreen(
                                        partyWithBalances = partyWithBalances,
                                        transactions = transactions,
                                        onBack = { activePartyId = null },
                                        onEditParty = { partyToEdit = partyWithBalances.party; showPartyDialog = true },
                                        onArchiveParty = { viewModel.setPartyArchived(id, it) },
                                        onDeleteParty = {
                                            viewModel.deleteParty(id, { activePartyId = null }, {
                                                scope.launch { snackbarHostState.showSnackbar(strings.cannotDeletePartyWithTx) }
                                            })
                                        },
                                        onCallParty = { ShareHelper.dialPhoneNumber(context, it) },
                                        onShareStatementPdf = { currency, accountType ->
                                            pendingReportExport = PendingReportExport(
                                                ReportExportKind.STATEMENT_PDF, id, currency, accountType
                                            )
                                        },
                                        onShareImageCard = { currency, accountType ->
                                            pendingReportExport = PendingReportExport(
                                                ReportExportKind.IMAGE_CARD, id, currency, accountType
                                            )
                                        },
                                        onShareTextSummary = { currency, accountType ->
                                            pendingReportExport = PendingReportExport(
                                                ReportExportKind.TEXT_SUMMARY, id, currency, accountType
                                            )
                                        },
                                        onAddTransaction = { type ->
                                            transactionToEdit = null
                                            preselectedTransactionType = type
                                            preselectedPartyId = id
                                            showTransactionDialog = true
                                        },
                                        onQuickSettle = { type, amount, currency, decimals ->
                                            viewModel.addTransaction(
                                                id,
                                                type,
                                                amount,
                                                currency,
                                                decimals,
                                                System.currentTimeMillis(),
                                                featureStrings.fullSettlementNote
                                            )
                                        },
                                        onOpenAttachment = { tx -> transactionForAttachments = tx },
                                        onEditTransaction = { tx ->
                                            transactionToEdit = tx
                                            preselectedTransactionType = null
                                            preselectedPartyId = tx.partyId
                                            showTransactionDialog = true
                                        },
                                        onDeleteTransaction = { tx ->
                                            scope.launch {
                                                v12Repository.getAttachments(tx.id).forEach {
                                                    AttachmentService.deletePath(it.filePath)
                                                }
                                                AttachmentService.deletePath(tx.attachmentPath)
                                                viewModel.deleteTransaction(tx.id)
                                            }
                                        }
                                    )
                                }
                            }

                            else -> when (selectedTab) {
                                0 -> HomeScreen(
                                    businessName = activeLedger?.name ?: settings?.businessName.orEmpty(),
                                    selectedCurrencyCode = selectedCurrencyCode,
                                    currencies = enabledCurrencies.ifEmpty { allCurrencies },
                                    summary = smartDashboard,
                                    recentTransactions = scopedRecent,
                                    overdueCount = collectionItems.count { it.status == CollectionStatus.OVERDUE },
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
                                    onOpenCollectionCenter = { showCollectionCenter = true },
                                    onSearchClick = { selectedTab = 1 },
                                    onOpenLedgerManager = { showLedgerManager = true },
                                    onOpenActivity = { showActivity = true }
                                )

                                1 -> PartiesScreen(
                                    parties = scopedParties,
                                    onPartyClick = { activePartyId = it },
                                    onAddPartyClick = {
                                        partyToEdit = null
                                        reopenTransactionAfterParty = false
                                        showPartyDialog = true
                                    },
                                    overduePartyIds = collectionItems
                                        .filter { it.status == CollectionStatus.OVERDUE }
                                        .mapTo(hashSetOf()) { it.party.id }
                                )

                                2 -> ReportsScreen(
                                    defaultCurrencyCode = selectedCurrencyCode,
                                    currencies = enabledCurrencies.ifEmpty { allCurrencies },
                                    parties = scopedParties,
                                    onLoadReport = { currency, from, to, partyId ->
                                        if (partyId == null) v12Repository.getScopedReportSummary(currency, from, to)
                                        else viewModel.loadReportSummary(currency, from, to, partyId)
                                    },
                                    onExportCsv = { from, to ->
                                        pendingReportExport = PendingReportExport(
                                            kind = ReportExportKind.CSV,
                                            fromTime = from,
                                            toTime = to
                                        )
                                    }
                                )

                                else -> MoreHubV12(
                                    language = currentLanguage,
                                    activeLedger = activeLedger,
                                    onLedgers = { showLedgerManager = true },
                                    onExcelExport = { requestExcelExport() },
                                    onExcelImport = { excelImportLauncher.launch(XLSX_MIME) },
                                    onExcelTemplate = {
                                        excelTemplateLauncher.launch("DeynBook_Import_Template.xlsx")
                                    },
                                    onGeneralSettings = { showGeneralSettings = true }
                                )
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
                        onSearchSimilar = { query ->
                            viewModel.searchSimilarParties(query).filter {
                                it.ledgerId == (activeLedger?.id ?: AppDatabase.DEFAULT_LEDGER_ID)
                            }
                        },
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
                                    scope.launch { v12Repository.assignPartyToActiveLedger(newPartyId) }
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
                        partiesWithBalances = scopedParties,
                        currencies = enabledCurrencies.ifEmpty { allCurrencies },
                        onDismiss = {
                            showTransactionDialog = false
                            transactionToEdit = null
                            preselectedTransactionType = null
                            preselectedPartyId = null
                            if (pendingSharedUri != null) {
                                pendingSharedUri = null
                                onSharedConsumed()
                            }
                        },
                        onOpenAddParty = {
                            showTransactionDialog = false
                            reopenTransactionAfterParty = true
                            partyToEdit = null
                            showPartyDialog = true
                        },
                        onSaveTransaction = { partyId, type, amount, currency, decimals, occurredAt, note, dueAt, attachmentUri, removeAttachment ->
                            val actualAttachment = pendingSharedUri ?: attachmentUri
                            val existing = transactionToEdit
                            if (existing == null) {
                                viewModel.addTransactionWithExtras(
                                    partyId,
                                    type,
                                    amount,
                                    currency,
                                    decimals,
                                    occurredAt,
                                    note,
                                    dueAt,
                                    actualAttachment
                                )
                            } else {
                                viewModel.updateTransactionWithExtras(
                                    existing.id,
                                    partyId,
                                    type,
                                    amount,
                                    currency,
                                    decimals,
                                    occurredAt,
                                    note,
                                    dueAt,
                                    actualAttachment,
                                    removeAttachment
                                )
                            }
                            showTransactionDialog = false
                            transactionToEdit = null
                            preselectedTransactionType = null
                            preselectedPartyId = null
                            if (pendingSharedUri != null) {
                                pendingSharedUri = null
                                onSharedConsumed()
                            }
                        }
                    )
                }

                transactionForAttachments?.let { tx ->
                    val attachmentFlow = remember(tx.id) { v12Repository.getAttachmentsFlow(tx.id) }
                    val attachments by attachmentFlow.collectAsState(initial = emptyList())
                    AttachmentManagerDialog(
                        attachments = attachments,
                        language = currentLanguage,
                        onDismiss = { transactionForAttachments = null },
                        onAdd = { uri -> viewModel.addAttachmentV12(tx.id, uri) },
                        onOpen = { attachment ->
                            AttachmentService.open(context, attachment.filePath, attachment.mimeType)
                        },
                        onDelete = viewModel::deleteAttachmentV12
                    )
                }

                pendingRestore?.let { backup ->
                    AlertDialog(
                        onDismissRequest = { pendingRestore = null },
                        title = {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Text(strings.restorePreviewTitle)
                        },
                        text = {
                            Text(
                                "${strings.restoreWarningMessage}\n" +
                                    "${strings.lastBackupDate} ${DateFormatter.formatDateTime(backup.exportedAt)}\n" +
                                    "${v12Strings.ledgers}: ${backup.ledgers.size}\n" +
                                    "${strings.partiesTitle}: ${backup.parties.size}\n" +
                                    "${strings.totalTxCount}: ${backup.transactions.size}"
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val safety = V12BackupService.export(database)
                                        withContext(Dispatchers.IO) {
                                            val dir = File(context.filesDir, "safety_backups").apply { mkdirs() }
                                            File(dir, "before_restore_${System.currentTimeMillis()}.deynbook.json")
                                                .writeText(safety.toJsonString(), Charsets.UTF_8)
                                        }
                                        V12BackupService.restore(database, backup)
                                        pendingRestore = null
                                        val restoredLedger = v12Repository.getActiveLedger()
                                        viewModel.setSelectedCurrencyCode(restoredLedger.defaultCurrencyCode)
                                        snackbarHostState.showSnackbar(strings.restoreSuccess)
                                    } catch (_: Exception) {
                                        pendingRestore = null
                                        snackbarHostState.showSnackbar(strings.errorGeneric)
                                    }
                                }
                            }) { Text(strings.restoreBackup) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingRestore = null }) { Text(strings.cancel) }
                        }
                    )
                }

                pendingReportExport?.let { request ->
                    ReportExportDialog(
                        appLanguage = currentLanguage,
                        onDismiss = { pendingReportExport = null },
                        onConfirm = { options ->
                            pendingReportExport = null
                            val reportResult: (ReportExportResult) -> Unit = { result ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        reportExportResultMessage(currentLanguage, result)
                                    )
                                }
                            }
                            when (request.kind) {
                                ReportExportKind.STATEMENT_PDF -> viewModel.shareStatementPdfV12(
                                    partyId = requireNotNull(request.partyId),
                                    currencyCode = requireNotNull(request.currencyCode),
                                    accountType = requireNotNull(request.accountType),
                                    options = options,
                                    onResult = reportResult
                                )
                                ReportExportKind.IMAGE_CARD -> viewModel.shareImageCardTranslatedV12(
                                    partyId = requireNotNull(request.partyId),
                                    currencyCode = requireNotNull(request.currencyCode),
                                    accountType = requireNotNull(request.accountType),
                                    options = options,
                                    onResult = reportResult
                                )
                                ReportExportKind.TEXT_SUMMARY -> viewModel.shareTextSummaryTranslatedV12(
                                    partyId = requireNotNull(request.partyId),
                                    currencyCode = requireNotNull(request.currencyCode),
                                    accountType = requireNotNull(request.accountType),
                                    options = options,
                                    onResult = reportResult
                                )
                                ReportExportKind.CSV -> viewModel.requestScopedCsvExport(
                                    context = context,
                                    options = options,
                                    fromTime = request.fromTime,
                                    toTime = request.toTime,
                                    onResult = reportResult
                                )
                                ReportExportKind.EXCEL -> {
                                    pendingExcelOptions = options
                                    pendingExcelRange = request.fromTime to request.toTime
                                    val suffix = if (
                                        request.fromTime == Long.MIN_VALUE && request.toTime == Long.MAX_VALUE
                                    ) "all" else {
                                        "${DateFormatter.formatForFileName(request.fromTime)}_to_" +
                                            DateFormatter.formatForFileName(request.toTime)
                                    }
                                    excelCreateLauncher.launch(
                                        "DeynBook_${activeLedger?.name ?: "Ledger"}_$suffix.xlsx"
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
