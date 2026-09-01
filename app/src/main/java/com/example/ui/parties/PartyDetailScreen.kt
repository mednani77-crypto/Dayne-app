package com.example.ui.parties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.data.models.TransactionType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.QuickActionButton

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
    onShareStatementPdf: (currencyCode: String) -> Unit,
    onShareImageCard: (currencyCode: String) -> Unit,
    onShareTextSummary: (currencyCode: String) -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onEditTransaction: (LedgerTransactionEntity) -> Unit,
    onDeleteTransaction: (LedgerTransactionEntity) -> Unit
) {
    val strings = LocalStrings.current
    val party = partyWithBalances.party
    val partyType = PartyType.from(party.partyType)

    var menuExpanded by remember { mutableStateOf(false) }
    var shareMenuExpanded by remember { mutableStateOf(false) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Debts, 2: Payments
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<LedgerTransactionEntity?>(null) }

    // Selected currency for actions & summaries
    val availableCurrencies = partyWithBalances.balances.map { it.currencyCode }.ifEmpty { listOf("DJF") }
    var selectedCurrencyCode by remember { mutableStateOf(availableCurrencies.first()) }
    val currentBalance = partyWithBalances.getBalanceForCurrency(selectedCurrencyCode)

    val filteredTransactions = remember(transactions, selectedFilterIndex, selectedCurrencyCode) {
        transactions.filter { tx ->
            val matchCurrency = tx.currencyCode == selectedCurrencyCode
            val type = TransactionType.from(tx.transactionType)
            val matchType = when (selectedFilterIndex) {
                1 -> type.isPositiveImpact
                2 -> !type.isPositiveImpact
                else -> true
            }
            matchCurrency && matchType
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        party.phone?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    // Call Button if phone present
                    party.phone?.let { phone ->
                        IconButton(onClick = { onCallParty(phone) }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = strings.callParty,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Share Menu Action
                    Box {
                        IconButton(onClick = { shareMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = strings.shareStatement,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = shareMenuExpanded,
                            onDismissRequest = { shareMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.exportPdfStatement) },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    shareMenuExpanded = false
                                    onShareStatementPdf(selectedCurrencyCode)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.shareImageCard) },
                                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                onClick = {
                                    shareMenuExpanded = false
                                    onShareImageCard(selectedCurrencyCode)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.shareTextSummary) },
                                leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
                                onClick = {
                                    shareMenuExpanded = false
                                    onShareTextSummary(selectedCurrencyCode)
                                }
                            )
                        }
                    }

                    // Options Menu (Edit, Archive, Delete)
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.editParty) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditParty()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (party.isArchived) strings.unarchiveParty else strings.archiveParty) },
                                leadingIcon = { Icon(if (party.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onArchiveParty(!party.isArchived)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.deleteParty, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    if (partyWithBalances.transactionCount == 0) {
                                        showDeleteConfirmDialog = true
                                    } else {
                                        // Can't delete if has transactions, show message
                                        showDeleteConfirmDialog = true
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))

                // Currency Selector row if multiple currencies
                if (availableCurrencies.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCurrencies.forEach { code ->
                            FilterChip(
                                selected = code == selectedCurrencyCode,
                                onClick = { selectedCurrencyCode = code },
                                label = { Text(code, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Balance Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        val isCustomer = partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
                        val isSupplier = partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH

                        if (isCustomer && currentBalance != null) {
                            Text(
                                text = "${strings.typeCustomer} • ${currentBalance.getCustomerStatusText(strings)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = if (currentBalance.customerBalance > 0) Color(0xFF196838) else Color(0xFF0061A4)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.totalDebtRecorded, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        AmountFormatter.formatAmount(currentBalance.customerTotalDebt, currentBalance.decimalPlaces, selectedCurrencyCode),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF196838)
                                    )
                                }
                                Column {
                                    Text(strings.totalPaidRecorded, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        AmountFormatter.formatAmount(currentBalance.customerTotalPaid, currentBalance.decimalPlaces, selectedCurrencyCode),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF0061A4)
                                    )
                                }
                            }
                        }

                        if (partyType == PartyType.BOTH && currentBalance != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (isSupplier && currentBalance != null) {
                            Text(
                                text = "${strings.typeSupplier} • ${currentBalance.getSupplierStatusText(strings)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = if (currentBalance.supplierBalance > 0) Color(0xFFB3261E) else Color(0xFF0061A4)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.totalDebtRecorded, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        AmountFormatter.formatAmount(currentBalance.supplierTotalDebt, currentBalance.decimalPlaces, selectedCurrencyCode),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFB3261E)
                                    )
                                }
                                Column {
                                    Text(strings.totalPaidRecorded, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        AmountFormatter.formatAmount(currentBalance.supplierTotalPaid, currentBalance.decimalPlaces, selectedCurrencyCode),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF0061A4)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Quick Action Buttons based on Party Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH) {
                        Button(
                            onClick = { onAddTransaction(TransactionType.CUSTOMER_DEBT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF196838))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.quickActionCustomerDebt, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onAddTransaction(TransactionType.CUSTOMER_PAYMENT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.quickActionCustomerPayment, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { onAddTransaction(TransactionType.SUPPLIER_DEBT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.quickActionSupplierDebt, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onAddTransaction(TransactionType.SUPPLIER_PAYMENT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.quickActionSupplierPayment, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                // Filter Tabs: All, Debts, Payments
                SecondaryTabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Tab(
                        selected = selectedFilterIndex == 0,
                        onClick = { selectedFilterIndex = 0 },
                        text = { Text(strings.filterAll) }
                    )
                    Tab(
                        selected = selectedFilterIndex == 1,
                        onClick = { selectedFilterIndex = 1 },
                        text = { Text(strings.filterDebts) }
                    )
                    Tab(
                        selected = selectedFilterIndex == 2,
                        onClick = { selectedFilterIndex = 2 },
                        text = { Text(strings.filterPayments) }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = strings.noTransactionsYet
                    )
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    PartyTransactionCard(
                        transaction = tx,
                        onClick = { onEditTransaction(tx) },
                        onDelete = { transactionToDelete = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Delete Transaction Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(strings.confirmDeleteTxTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(strings.confirmDeleteTxMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode)} - ${DateFormatter.formatDate(tx.occurredAt)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = transactionToDelete
                        transactionToDelete = null
                        if (toDel != null) onDeleteTransaction(toDel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Party Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val hasTx = partyWithBalances.transactionCount > 0
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(strings.deleteParty, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (hasTx) strings.cannotDeletePartyWithTx else strings.deletePartyWarning
                )
            },
            confirmButton = {
                if (!hasTx) {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteParty()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.delete)
                    }
                } else {
                    Button(onClick = { showDeleteConfirmDialog = false }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (!hasTx) {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            }
        )
    }
}

@Composable
private fun PartyTransactionCard(
    transaction: LedgerTransactionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    val type = TransactionType.from(transaction.transactionType)
    val isDebit = type.isPositiveImpact

    val badgeColor = when (type) {
        TransactionType.CUSTOMER_DEBT -> Color(0xFF196838)
        TransactionType.CUSTOMER_PAYMENT -> Color(0xFF0061A4)
        TransactionType.SUPPLIER_DEBT -> Color(0xFFB3261E)
        TransactionType.SUPPLIER_PAYMENT -> Color(0xFF0061A4)
        TransactionType.OPENING_RECEIVABLE -> Color(0xFF196838)
        TransactionType.OPENING_PAYABLE -> Color(0xFFB3261E)
    }

    val typeTitle = when (type) {
        TransactionType.CUSTOMER_DEBT -> strings.quickActionCustomerDebt
        TransactionType.CUSTOMER_PAYMENT -> strings.quickActionCustomerPayment
        TransactionType.SUPPLIER_DEBT -> strings.quickActionSupplierDebt
        TransactionType.SUPPLIER_PAYMENT -> strings.quickActionSupplierPayment
        TransactionType.OPENING_RECEIVABLE -> strings.txOpeningReceivable
        TransactionType.OPENING_PAYABLE -> strings.txOpeningPayable
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = typeTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateFormatter.formatDateTime(transaction.occurredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    transaction.note?.let { n ->
                        Text(
                            text = n,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val sign = if (isDebit) "+" else "-"
                Text(
                    text = "$sign${AmountFormatter.formatAmount(transaction.amountMinor, transaction.currencyDecimalPlaces, transaction.currencyCode)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDebit) Color(0xFF196838) else Color(0xFFB3261E)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
