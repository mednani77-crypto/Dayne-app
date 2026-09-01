package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.DeynBookLocalizationProvider
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType
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

sealed class NavTab(val icon: ImageVector, val tag: String) {
    data object Home : NavTab(Icons.Default.Home, "nav_home")
    data object Parties : NavTab(Icons.Default.People, "nav_parties")
    data object Reports : NavTab(Icons.Default.Assessment, "nav_reports")
    data object Settings : NavTab(Icons.Default.Settings, "nav_settings")
}

@Composable
fun DeynBookApp(
    viewModel: MainViewModel = viewModel()
) {
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

    var currentNavTab by remember { mutableStateOf<NavTab>(NavTab.Home) }
    var activePartyDetailId by remember { mutableStateOf<String?>(null) }

    // Dialogs state
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<PartyEntity?>(null) }

    var showAddTxDialog by remember { mutableStateOf(false) }
    var preselectedTxType by remember { mutableStateOf<TransactionType?>(null) }
    var preselectedTxPartyId by remember { mutableStateOf<String?>(null) }
    var txToEdit by remember { mutableStateOf<LedgerTransactionEntity?>(null) }

    // Synchronize default currency if none selected yet
    LaunchedEffect(settings?.defaultCurrencyCode) {
        settings?.defaultCurrencyCode?.let { def ->
            if (selectedCurrencyCode == "DJF" && def != "DJF") {
                viewModel.setSelectedCurrencyCode(def)
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    DeynBookLocalizationProvider(language = currentLanguage) {
        val strings = LocalStrings.current

        DeynBookTheme(themeMode = settings?.themeMode ?: "SYSTEM") {
            if (settings?.onboardingCompleted != true) {
                // Show Onboarding Flow
                OnboardingScreen(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { lang ->
                        viewModel.setLanguage(lang)
                    },
                    onFinishOnboarding = { lang, country, currency, name, phone ->
                        viewModel.completeOnboarding(lang, country, currency, name, phone)
                    }
                )
            } else {
                // Main App Structure
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (activePartyDetailId == null) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                val tabs = listOf(
                                    NavTab.Home to strings.navHome,
                                    NavTab.Parties to strings.navParties,
                                    NavTab.Reports to strings.navReports,
                                    NavTab.Settings to strings.settingsTitle
                                )
                                tabs.forEach { (tab, label) ->
                                    val isSelected = currentNavTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            currentNavTab = tab
                                        },
                                        icon = {
                                            Icon(
                                                tab.icon,
                                                contentDescription = label,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (activePartyDetailId != null) {
                            val activePartyWithBal = partiesWithBalances.find { it.party.id == activePartyDetailId }
                            if (activePartyWithBal != null) {
                                val partyTxs by viewModel.repository.getTransactionsForPartyFlow(activePartyDetailId!!)
                                    .collectAsState(initial = emptyList())

                                PartyDetailScreen(
                                    partyWithBalances = activePartyWithBal,
                                    transactions = partyTxs,
                                    onBack = { activePartyDetailId = null },
                                    onEditParty = {
                                        partyToEdit = activePartyWithBal.party
                                        showAddPartyDialog = true
                                    },
                                    onArchiveParty = { isArchived ->
                                        viewModel.setPartyArchived(activePartyWithBal.party.id, isArchived)
                                    },
                                    onDeleteParty = {
                                        viewModel.deleteParty(
                                            partyId = activePartyWithBal.party.id,
                                            onSuccess = { activePartyDetailId = null },
                                            onError = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(strings.cannotDeletePartyWithTx)
                                                }
                                            }
                                        )
                                    },
                                    onCallParty = { phone ->
                                        ShareHelper.dialPhoneNumber(context, phone)
                                    },
                                    onShareStatementPdf = { curCode ->
                                        viewModel.shareStatementPdf(activePartyWithBal.party.id, curCode, currentLanguage)
                                    },
                                    onShareImageCard = { curCode ->
                                        viewModel.shareImageCard(activePartyWithBal.party.id, curCode, currentLanguage)
                                    },
                                    onShareTextSummary = { curCode ->
                                        viewModel.shareTextSummary(activePartyWithBal.party.id, curCode, currentLanguage)
                                    },
                                    onAddTransaction = { type ->
                                        preselectedTxType = type
                                        preselectedTxPartyId = activePartyWithBal.party.id
                                        txToEdit = null
                                        showAddTxDialog = true
                                    },
                                    onEditTransaction = { tx ->
                                        txToEdit = tx
                                        showAddTxDialog = true
                                    },
                                    onDeleteTransaction = { tx ->
                                        viewModel.deleteTransaction(tx.id)
                                    }
                                )
                            } else {
                                activePartyDetailId = null
                            }
                        } else {
                            when (currentNavTab) {
                                NavTab.Home -> {
                                    HomeScreen(
                                        businessName = settings?.businessName ?: "",
                                        selectedCurrencyCode = selectedCurrencyCode,
                                        currencies = enabledCurrencies.ifEmpty { allCurrencies },
                                        summary = dashboardSummary,
                                        recentTransactions = recentTransactions,
                                        onSelectCurrency = { viewModel.setSelectedCurrencyCode(it) },
                                        onQuickAction = { type ->
                                            preselectedTxType = type
                                            preselectedTxPartyId = null
                                            txToEdit = null
                                            showAddTxDialog = true
                                        },
                                        onTransactionClick = { tx ->
                                            txToEdit = tx
                                            showAddTxDialog = true
                                        },
                                        onNavigateToParties = { currentNavTab = NavTab.Parties },
                                        onSearchClick = { currentNavTab = NavTab.Parties }
                                    )
                                }
                                NavTab.Parties -> {
                                    PartiesScreen(
                                        parties = partiesWithBalances,
                                        onPartyClick = { partyId -> activePartyDetailId = partyId },
                                        onAddPartyClick = {
                                            partyToEdit = null
                                            showAddPartyDialog = true
                                        }
                                    )
                                }
                                NavTab.Reports -> {
                                    ReportsScreen(
                                        defaultCurrencyCode = selectedCurrencyCode,
                                        currencies = enabledCurrencies.ifEmpty { allCurrencies },
                                        onLoadReport = { cur, from, to ->
                                            viewModel.loadReportSummary(cur, from, to)
                                        },
                                        onExportCsv = { viewModel.exportCsv() }
                                    )
                                }
                                NavTab.Settings -> {
                                    settings?.let { st ->
                                        SettingsScreen(
                                            settings = st,
                                            currencies = allCurrencies,
                                            currentLanguage = currentLanguage,
                                            onUpdateSettings = { viewModel.updateSettings(it) },
                                            onLanguageChange = { viewModel.setLanguage(it) },
                                            onToggleCurrency = { code, enabled -> viewModel.toggleCurrency(code, enabled) },
                                            onAddCustomCurrency = { code, name, dec -> viewModel.addCustomCurrency(code, name, dec) },
                                            onExportBackup = { viewModel.exportBackup() },
                                            onImportBackupFile = { uri -> viewModel.parseBackupFromUri(uri) },
                                            onExportCsv = { viewModel.exportCsv() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add / Edit Party Dialog
                if (showAddPartyDialog) {
                    AddEditPartyDialog(
                        initialParty = partyToEdit,
                        defaultCurrencyCode = selectedCurrencyCode,
                        currencies = enabledCurrencies.ifEmpty { allCurrencies },
                        onDismiss = {
                            showAddPartyDialog = false
                            partyToEdit = null
                        },
                        onSearchSimilar = { query ->
                            viewModel.searchSimilarParties(query)
                        },
                        onSave = { name, phone, type, notes, opType, opAmount, curCode, curDec ->
                            if (partyToEdit == null) {
                                viewModel.createParty(name, phone, type, notes, opType, opAmount, curCode, curDec)
                            } else {
                                viewModel.updateParty(
                                    partyToEdit!!.copy(
                                        name = name,
                                        normalizedName = name.lowercase(),
                                        phone = phone,
                                        partyType = type.value,
                                        notes = notes
                                    )
                                )
                            }
                            showAddPartyDialog = false
                            partyToEdit = null
                        }
                    )
                }

                // Add / Edit Transaction Dialog
                if (showAddTxDialog) {
                    AddEditTransactionDialog(
                        initialTransaction = txToEdit,
                        preselectedType = preselectedTxType,
                        preselectedPartyId = preselectedTxPartyId,
                        defaultCurrencyCode = selectedCurrencyCode,
                        partiesWithBalances = partiesWithBalances,
                        currencies = enabledCurrencies.ifEmpty { allCurrencies },
                        onDismiss = {
                            showAddTxDialog = false
                            txToEdit = null
                            preselectedTxType = null
                            preselectedTxPartyId = null
                        },
                        onOpenAddParty = {
                            partyToEdit = null
                            showAddPartyDialog = true
                        },
                        onSaveTransaction = { partyId, type, amountMinor, currencyCode, currencyDecimalPlaces, occurredAt, note ->
                            if (txToEdit == null) {
                                viewModel.addTransaction(
                                    partyId = partyId,
                                    type = type,
                                    amountMinor = amountMinor,
                                    currencyCode = currencyCode,
                                    currencyDecimalPlaces = currencyDecimalPlaces,
                                    occurredAt = occurredAt,
                                    note = note
                                )
                            } else {
                                viewModel.updateTransaction(
                                    id = txToEdit!!.id,
                                    partyId = partyId,
                                    type = type,
                                    amountMinor = amountMinor,
                                    currencyCode = currencyCode,
                                    currencyDecimalPlaces = currencyDecimalPlaces,
                                    occurredAt = occurredAt,
                                    note = note
                                )
                            }
                            showAddTxDialog = false
                            txToEdit = null
                            preselectedTxType = null
                            preselectedTxPartyId = null
                        }
                    )
                }

                // Backup Restore Preview Dialog
                pendingRestoreBackup?.let { backup ->
                    AlertDialog(
                        onDismissRequest = { viewModel.cancelRestoreBackup() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.restorePreviewTitle, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column {
                                Text(strings.restoreWarningMessage)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("• ${strings.lastBackupDate} ${DateFormatter.formatDateTime(backup.exportedAt)}")
                                Text("• ${strings.partiesTitle}: ${backup.parties.size}")
                                Text("• ${strings.totalTxCount}: ${backup.transactions.size}")
                                Text("• ${strings.currencySettingTitle}: ${backup.currencies.size}")
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.confirmRestoreBackup(backup) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(strings.restoreBackup)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.cancelRestoreBackup() }) {
                                Text(strings.cancel)
                            }
                        }
                    )
                }
            }
        }
    }
}
