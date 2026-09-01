package com.example.ui.parties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.FeatureStringsProvider
import com.example.core.localization.LocalLanguage
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.data.models.TransactionType
import com.example.services.StatementAccountType
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(
    partyWithBalances: PartyWithBalances,
    transactions: List<LedgerTransactionEntity>,
    onBack: () -> Unit,
    onEditParty: () -> Unit,
    onArchiveParty: (Boolean) -> Unit,
    onDeleteParty: () -> Unit,
    onCallParty: (String) -> Unit,
    onShareStatementPdf: (currencyCode: String, accountType: StatementAccountType) -> Unit,
    onShareImageCard: (currencyCode: String, accountType: StatementAccountType) -> Unit,
    onShareTextSummary: (currencyCode: String, accountType: StatementAccountType) -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onQuickSettle: (type: TransactionType, amountMinor: Long, currencyCode: String, decimalPlaces: Int) -> Unit,
    onOpenAttachment: (LedgerTransactionEntity) -> Unit,
    onEditTransaction: (LedgerTransactionEntity) -> Unit,
    onDeleteTransaction: (LedgerTransactionEntity) -> Unit
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    val party = partyWithBalances.party
    val partyType = PartyType.from(party.partyType)
    val supportsCustomer = partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
    val supportsSupplier = partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH

    val availableCurrencies = partyWithBalances.balances.map { it.currencyCode }.distinct().ifEmpty { listOf("DJF") }
    var selectedCurrencyCode by remember(availableCurrencies) { mutableStateOf(availableCurrencies.first()) }
    val currentBalance = partyWithBalances.getBalanceForCurrency(selectedCurrencyCode)

    var filterIndex by remember { mutableIntStateOf(0) }
    var shareMenuExpanded by remember { mutableStateOf(false) }
    var optionsExpanded by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<LedgerTransactionEntity?>(null) }
    var showDeletePartyDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedCurrencyCode, filterIndex) {
        transactions.filter { tx ->
            if (tx.currencyCode != selectedCurrencyCode) return@filter false
            val type = TransactionType.from(tx.transactionType)
            when (filterIndex) {
                1 -> type.isPositiveImpact
                2 -> !type.isPositiveImpact
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(party.name, fontWeight = FontWeight.Bold, maxLines = 1)
                        party.phone?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    party.phone?.let { phone ->
                        IconButton(onClick = { onCallParty(phone) }) {
                            Icon(Icons.Default.Call, contentDescription = strings.callParty)
                        }
                    }

                    Box {
                        IconButton(onClick = { shareMenuExpanded = true }) {
                            Icon(Icons.Default.Share, contentDescription = strings.shareStatement)
                        }
                        DropdownMenu(expanded = shareMenuExpanded, onDismissRequest = { shareMenuExpanded = false }) {
                            if (supportsCustomer) {
                                ShareItems(
                                    accountLabel = strings.typeCustomer,
                                    onPdf = { shareMenuExpanded = false; onShareStatementPdf(selectedCurrencyCode, StatementAccountType.CUSTOMER) },
                                    onImage = { shareMenuExpanded = false; onShareImageCard(selectedCurrencyCode, StatementAccountType.CUSTOMER) },
                                    onText = { shareMenuExpanded = false; onShareTextSummary(selectedCurrencyCode, StatementAccountType.CUSTOMER) }
                                )
                            }
                            if (supportsSupplier) {
                                ShareItems(
                                    accountLabel = strings.typeSupplier,
                                    onPdf = { shareMenuExpanded = false; onShareStatementPdf(selectedCurrencyCode, StatementAccountType.SUPPLIER) },
                                    onImage = { shareMenuExpanded = false; onShareImageCard(selectedCurrencyCode, StatementAccountType.SUPPLIER) },
                                    onText = { shareMenuExpanded = false; onShareTextSummary(selectedCurrencyCode, StatementAccountType.SUPPLIER) }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { optionsExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = optionsExpanded, onDismissRequest = { optionsExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(strings.editParty) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { optionsExpanded = false; onEditParty() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (party.isArchived) strings.unarchiveParty else strings.archiveParty) },
                                leadingIcon = { Icon(if (party.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                                onClick = { optionsExpanded = false; onArchiveParty(!party.isArchived) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.deleteParty, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { optionsExpanded = false; showDeletePartyDialog = true }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (availableCurrencies.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableCurrencies.forEach { code ->
                            FilterChip(
                                selected = selectedCurrencyCode == code,
                                onClick = { selectedCurrencyCode = code },
                                label = { Text(code, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (supportsCustomer) {
                            AccountBalanceBlock(
                                title = strings.typeCustomer,
                                status = currentBalance?.getCustomerStatusText(strings) ?: strings.statusSettled,
                                totalDebt = currentBalance?.customerTotalDebt ?: 0L,
                                totalPaid = currentBalance?.customerTotalPaid ?: 0L,
                                decimals = currentBalance?.decimalPlaces ?: 0,
                                currencyCode = selectedCurrencyCode,
                                debtColor = Color(0xFF196838)
                            )
                        }
                        if (supportsSupplier) {
                            AccountBalanceBlock(
                                title = strings.typeSupplier,
                                status = currentBalance?.getSupplierStatusText(strings) ?: strings.statusSettled,
                                totalDebt = currentBalance?.supplierTotalDebt ?: 0L,
                                totalPaid = currentBalance?.supplierTotalPaid ?: 0L,
                                decimals = currentBalance?.decimalPlaces ?: 0,
                                currencyCode = selectedCurrencyCode,
                                debtColor = Color(0xFFB3261E)
                            )
                        }
                    }
                }
            }

            if (supportsCustomer) {
                item {
                    ActionRow(
                        firstText = strings.quickActionCustomerDebt,
                        secondText = strings.quickActionCustomerPayment,
                        firstColor = Color(0xFF196838),
                        secondColor = Color(0xFF0061A4),
                        onFirst = { onAddTransaction(TransactionType.CUSTOMER_DEBT) },
                        onSecond = { onAddTransaction(TransactionType.CUSTOMER_PAYMENT) }
                    )
                }
                val customerBalance = currentBalance?.customerBalance ?: 0L
                if (customerBalance > 0L) {
                    item {
                        Button(
                            onClick = {
                                onQuickSettle(
                                    TransactionType.CUSTOMER_PAYMENT,
                                    customerBalance,
                                    selectedCurrencyCode,
                                    currentBalance?.decimalPlaces ?: 0
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(featureStrings.settleFullCustomer)
                        }
                    }
                }
            }

            if (supportsSupplier) {
                item {
                    ActionRow(
                        firstText = strings.quickActionSupplierDebt,
                        secondText = strings.quickActionSupplierPayment,
                        firstColor = Color(0xFFB3261E),
                        secondColor = Color(0xFF0061A4),
                        onFirst = { onAddTransaction(TransactionType.SUPPLIER_DEBT) },
                        onSecond = { onAddTransaction(TransactionType.SUPPLIER_PAYMENT) }
                    )
                }
                val supplierBalance = currentBalance?.supplierBalance ?: 0L
                if (supplierBalance > 0L) {
                    item {
                        Button(
                            onClick = {
                                onQuickSettle(
                                    TransactionType.SUPPLIER_PAYMENT,
                                    supplierBalance,
                                    selectedCurrencyCode,
                                    currentBalance?.decimalPlaces ?: 0
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(featureStrings.settleFullSupplier)
                        }
                    }
                }
            }

            item {
                SecondaryTabRow(selectedTabIndex = filterIndex) {
                    listOf(strings.filterAll, strings.filterDebts, strings.filterPayments).forEachIndexed { index, label ->
                        Tab(selected = filterIndex == index, onClick = { filterIndex = index }, text = { Text(label) })
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item { EmptyStateView(icon = Icons.Default.ReceiptLong, title = strings.noTransactionsYet) }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onClick = { onEditTransaction(tx) },
                        onOpenAttachment = { onOpenAttachment(tx) },
                        onDelete = { transactionToDelete = tx }
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(strings.confirmDeleteTxTitle, fontWeight = FontWeight.Bold) },
            text = { Text("${strings.confirmDeleteTxMessage}\n${AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode)}") },
            confirmButton = {
                Button(
                    onClick = { transactionToDelete = null; onDeleteTransaction(tx) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = { TextButton(onClick = { transactionToDelete = null }) { Text(strings.cancel) } }
        )
    }

    if (showDeletePartyDialog) {
        val hasTransactions = partyWithBalances.transactionCount > 0
        AlertDialog(
            onDismissRequest = { showDeletePartyDialog = false },
            title = { Text(strings.deleteParty, fontWeight = FontWeight.Bold) },
            text = { Text(if (hasTransactions) strings.cannotDeletePartyWithTx else strings.deletePartyWarning) },
            confirmButton = {
                if (hasTransactions) {
                    Button(onClick = { showDeletePartyDialog = false }) { Text(strings.cancel) }
                } else {
                    Button(
                        onClick = { showDeletePartyDialog = false; onDeleteParty() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(strings.delete) }
                }
            },
            dismissButton = {
                if (!hasTransactions) TextButton(onClick = { showDeletePartyDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ShareItems(
    accountLabel: String,
    onPdf: () -> Unit,
    onImage: () -> Unit,
    onText: () -> Unit
) {
    val strings = LocalStrings.current
    DropdownMenuItem(
        text = { Text("${strings.exportPdfStatement} • $accountLabel") },
        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
        onClick = onPdf
    )
    DropdownMenuItem(
        text = { Text("${strings.shareImageCard} • $accountLabel") },
        leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
        onClick = onImage
    )
    DropdownMenuItem(
        text = { Text("${strings.shareTextSummary} • $accountLabel") },
        leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
        onClick = onText
    )
}

@Composable
private fun AccountBalanceBlock(
    title: String,
    status: String,
    totalDebt: Long,
    totalPaid: Long,
    decimals: Int,
    currencyCode: String,
    debtColor: Color
) {
    val strings = LocalStrings.current
    Text("$title • $status", fontWeight = FontWeight.SemiBold)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(strings.totalDebtRecorded, style = MaterialTheme.typography.bodySmall)
            Text(AmountFormatter.formatAmount(totalDebt, decimals, currencyCode), color = debtColor, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(strings.totalPaidRecorded, style = MaterialTheme.typography.bodySmall)
            Text(AmountFormatter.formatAmount(totalPaid, decimals, currencyCode), color = Color(0xFF0061A4), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionRow(
    firstText: String,
    secondText: String,
    firstColor: Color,
    secondColor: Color,
    onFirst: () -> Unit,
    onSecond: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onFirst, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = firstColor)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(firstText)
        }
        Button(onClick = onSecond, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = secondColor)) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(secondText)
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: LedgerTransactionEntity,
    onClick: () -> Unit,
    onOpenAttachment: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    val type = TransactionType.from(transaction.transactionType)
    val title = when (type) {
        TransactionType.CUSTOMER_DEBT -> strings.quickActionCustomerDebt
        TransactionType.CUSTOMER_PAYMENT -> strings.quickActionCustomerPayment
        TransactionType.SUPPLIER_DEBT -> strings.quickActionSupplierDebt
        TransactionType.SUPPLIER_PAYMENT -> strings.quickActionSupplierPayment
        TransactionType.OPENING_RECEIVABLE -> strings.txOpeningReceivable
        TransactionType.OPENING_PAYABLE -> strings.txOpeningPayable
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (type.isPositiveImpact) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (type.isPositiveImpact) Color(0xFF196838) else Color(0xFFB3261E)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(DateFormatter.formatDateTime(transaction.occurredAt), style = MaterialTheme.typography.bodySmall)
                    transaction.dueAt?.let {
                        Text("${featureStrings.dueDate}: ${DateFormatter.formatDate(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    transaction.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (!transaction.attachmentPath.isNullOrBlank()) {
                        TextButton(onClick = onOpenAttachment) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Text(featureStrings.openAttachment, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (type.isPositiveImpact) "+" else "-") +
                        AmountFormatter.formatAmount(transaction.amountMinor, transaction.currencyDecimalPlaces, transaction.currencyCode),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = strings.delete) }
            }
        }
    }
}
