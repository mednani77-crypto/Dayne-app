package com.example.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.localization.FeatureStringsProvider
import com.example.core.localization.LocalLanguage
import com.example.core.localization.LocalStrings
import com.example.core.localization.V12StringsProvider
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.models.DashboardSummary
import com.example.data.models.TransactionType
import com.example.data.models.TransactionWithParty
import com.example.ui.components.EmptyStateView
import com.example.ui.components.QuickActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    businessName: String,
    selectedCurrencyCode: String,
    currencies: List<CurrencyEntity>,
    summary: DashboardSummary,
    recentTransactions: List<TransactionWithParty>,
    overdueCount: Int,
    onSelectCurrency: (String) -> Unit,
    onQuickAction: (TransactionType) -> Unit,
    onTransactionClick: (LedgerTransactionEntity) -> Unit,
    onNavigateToParties: () -> Unit,
    onOpenCollectionCenter: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenLedgerManager: () -> Unit = {},
    onOpenActivity: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    val v12 = V12StringsProvider.get(LocalLanguage.current)
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpenLedgerManager)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if (businessName.isNotBlank()) businessName else strings.appName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(" ${v12.switchLedger}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                actions = {
                    Box {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(50)).clickable { currencyMenuExpanded = true }.testTag("home_currency_selector"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedCurrencyCode, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = strings.selectCurrency, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(expanded = currencyMenuExpanded, onDismissRequest = { currencyMenuExpanded = false }) {
                            currencies.filter { it.isEnabled }.forEach { cur ->
                                DropdownMenuItem(text = { Text("${cur.code} - ${cur.name}") }, onClick = { currencyMenuExpanded = false; onSelectCurrency(cur.code) })
                            }
                        }
                    }
                    IconButton(onClick = onOpenActivity) { Icon(Icons.Default.History, contentDescription = v12.activity) }
                    IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = strings.searchPlaceholder) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryBalanceCard(strings.receivableTitle, summary.totalReceivable, summary.currencyCode, summary.decimalPlaces, Color(0xFF196838), Color(0xFFEAF5EE), "${summary.activeCustomerCount} ${strings.tabCustomers}", Modifier.weight(1f), "receivables_card")
                    SummaryBalanceCard(strings.payableTitle, summary.totalPayable, summary.currencyCode, summary.decimalPlaces, Color(0xFFB3261E), Color(0xFFFDF0ED), "${summary.activeSupplierCount} ${strings.tabSuppliers}", Modifier.weight(1f), "payables_card")
                }
            }

            item { SmartOverview(summary = summary, overdueCount = overdueCount, onOpenCollections = onOpenCollectionCenter) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCollectionCenter),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(featureStrings.collectionCenter, fontWeight = FontWeight.Bold)
                                Text(featureStrings.collectionSubtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                        Surface(color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
                            Text(overdueCount.toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
            }

            item {
                Text(strings.addTransaction.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(strings.quickActionCustomerDebt, Icons.Default.ArrowUpward, Color(0xFF0061A4), MaterialTheme.colorScheme.surface, { onQuickAction(TransactionType.CUSTOMER_DEBT) }, Modifier.weight(1f), "quick_action_customer_debt")
                    QuickActionButton(strings.quickActionCustomerPayment, Icons.Default.ArrowDownward, Color(0xFF196838), MaterialTheme.colorScheme.surface, { onQuickAction(TransactionType.CUSTOMER_PAYMENT) }, Modifier.weight(1f), "quick_action_customer_payment")
                    QuickActionButton(strings.quickActionSupplierDebt, Icons.Default.ArrowUpward, Color(0xFFB3261E), MaterialTheme.colorScheme.surface, { onQuickAction(TransactionType.SUPPLIER_DEBT) }, Modifier.weight(1f), "quick_action_supplier_debt")
                    QuickActionButton(strings.quickActionSupplierPayment, Icons.Default.ArrowDownward, Color(0xFF44474E), MaterialTheme.colorScheme.surface, { onQuickAction(TransactionType.SUPPLIER_PAYMENT) }, Modifier.weight(1f), "quick_action_supplier_payment")
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.recentTransactions, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    if (recentTransactions.isNotEmpty()) TextButton(onClick = onOpenActivity) { Text(v12.activity) }
                }
            }

            if (recentTransactions.isEmpty()) {
                item { EmptyStateView(Icons.Default.ReceiptLong, strings.noTransactionsYet, strings.addFirstTransaction, { onQuickAction(TransactionType.CUSTOMER_DEBT) }) }
            } else {
                items(recentTransactions, key = { it.transaction.id }) { item -> TransactionListItem(item) { onTransactionClick(item.transaction) } }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun SmartOverview(summary: DashboardSummary, overdueCount: Int, onOpenCollections: () -> Unit) {
    val v12 = V12StringsProvider.get(LocalLanguage.current)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(v12.smartOverview, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Insight(v12.overdue, overdueCount.toString(), overdueCount > 0)
                Insight(v12.dueNext7Days, summary.dueNext7DaysCount.toString(), false)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Insight(v12.collectedThisMonth, AmountFormatter.formatAmount(summary.collectedThisMonth, summary.decimalPlaces, summary.currencyCode), false)
                Insight(v12.paidSuppliersThisMonth, AmountFormatter.formatAmount(summary.paidToSuppliersThisMonth, summary.decimalPlaces, summary.currencyCode), false)
            }
            if (summary.topDebtors.isNotEmpty()) {
                Text(v12.topDebtors, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                summary.topDebtors.take(3).forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}. ${item.name}", style = MaterialTheme.typography.bodySmall)
                        Text(AmountFormatter.formatAmount(item.amountMinor, summary.decimalPlaces, summary.currencyCode), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (overdueCount > 0) TextButton(onClick = onOpenCollections) { Text(v12.overdue) }
        }
    }
}

@Composable
private fun Insight(label: String, value: String, alert: Boolean) {
    Column(modifier = Modifier.width(150.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, color = if (alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SummaryBalanceCard(title: String, amount: Long, currencyCode: String, decimalPlaces: Int, accentColor: Color, containerColor: Color, countLabel: String, modifier: Modifier = Modifier, testTag: String = "") {
    Card(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = containerColor), border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
            Spacer(Modifier.height(10.dp))
            Text(AmountFormatter.formatAmount(amount, decimalPlaces, currencyCode), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp), color = Color(0xFF1A1C1E))
            Spacer(Modifier.height(8.dp))
            Surface(color = accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) { Text(countLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = accentColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
        }
    }
}

@Composable
fun TransactionListItem(item: TransactionWithParty, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    val tx = item.transaction
    val type = TransactionType.from(tx.transactionType)
    val isDebit = type.isPositiveImpact
    val badgeColor = when (type) {
        TransactionType.CUSTOMER_DEBT, TransactionType.OPENING_RECEIVABLE -> Color(0xFF0061A4)
        TransactionType.CUSTOMER_PAYMENT -> Color(0xFF196838)
        TransactionType.SUPPLIER_DEBT, TransactionType.OPENING_PAYABLE -> Color(0xFFB3261E)
        TransactionType.SUPPLIER_PAYMENT -> Color(0xFF44474E)
    }
    val typeTitle = when (type) {
        TransactionType.CUSTOMER_DEBT -> strings.quickActionCustomerDebt
        TransactionType.CUSTOMER_PAYMENT -> strings.quickActionCustomerPayment
        TransactionType.SUPPLIER_DEBT -> strings.quickActionSupplierDebt
        TransactionType.SUPPLIER_PAYMENT -> strings.quickActionSupplierPayment
        TransactionType.OPENING_RECEIVABLE -> strings.txOpeningReceivable
        TransactionType.OPENING_PAYABLE -> strings.txOpeningPayable
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(badgeColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, tint = badgeColor)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(item.partyName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("$typeTitle • ${DateFormatter.formatDate(tx.occurredAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    tx.dueAt?.let { Text("${featureStrings.dueDate}: ${DateFormatter.formatDate(it)}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary) }
                    tx.note?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), maxLines = 1) }
                }
            }
            Text((if (isDebit) "+" else "-") + AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode), fontWeight = FontWeight.SemiBold, color = if (isDebit) Color(0xFF196838) else Color(0xFFB3261E))
        }
    }
}
