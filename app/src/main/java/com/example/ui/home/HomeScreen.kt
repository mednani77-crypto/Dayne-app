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
    onSearchClick: () -> Unit
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (businessName.isNotBlank()) businessName else strings.appName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = strings.appTagline.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { currencyMenuExpanded = true }
                                .testTag("home_currency_selector"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCurrencyCode,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = strings.selectCurrency,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(expanded = currencyMenuExpanded, onDismissRequest = { currencyMenuExpanded = false }) {
                            currencies.filter { it.isEnabled }.forEach { cur ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${cur.code} - ${cur.name}",
                                            fontWeight = if (cur.code == selectedCurrencyCode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        currencyMenuExpanded = false
                                        onSelectCurrency(cur.code)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = strings.searchPlaceholder,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryBalanceCard(
                        title = strings.receivableTitle,
                        amount = summary.totalReceivable,
                        currencyCode = summary.currencyCode,
                        decimalPlaces = summary.decimalPlaces,
                        accentColor = Color(0xFF196838),
                        containerColor = Color(0xFFEAF5EE),
                        countLabel = "${summary.activeCustomerCount} ${strings.tabCustomers}",
                        modifier = Modifier.weight(1f),
                        testTag = "receivables_card"
                    )
                    SummaryBalanceCard(
                        title = strings.payableTitle,
                        amount = summary.totalPayable,
                        currencyCode = summary.currencyCode,
                        decimalPlaces = summary.decimalPlaces,
                        accentColor = Color(0xFFB3261E),
                        containerColor = Color(0xFFFDF0ED),
                        countLabel = "${summary.activeSupplierCount} ${strings.tabSuppliers}",
                        modifier = Modifier.weight(1f),
                        testTag = "payables_card"
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCollectionCenter),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(featureStrings.collectionCenter, fontWeight = FontWeight.Bold)
                                Text(featureStrings.collectionSubtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                        Surface(
                            color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                overdueCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.addTransaction.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        title = strings.quickActionCustomerDebt,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = Color(0xFF0061A4),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = { onQuickAction(TransactionType.CUSTOMER_DEBT) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_customer_debt"
                    )
                    QuickActionButton(
                        title = strings.quickActionCustomerPayment,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = Color(0xFF196838),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = { onQuickAction(TransactionType.CUSTOMER_PAYMENT) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_customer_payment"
                    )
                    QuickActionButton(
                        title = strings.quickActionSupplierDebt,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = Color(0xFFB3261E),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = { onQuickAction(TransactionType.SUPPLIER_DEBT) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_supplier_debt"
                    )
                    QuickActionButton(
                        title = strings.quickActionSupplierPayment,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = Color(0xFF44474E),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = { onQuickAction(TransactionType.SUPPLIER_PAYMENT) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_supplier_payment"
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentTransactions,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (recentTransactions.isNotEmpty()) {
                        TextButton(onClick = onNavigateToParties) {
                            Text(strings.seeAll, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = strings.noTransactionsYet,
                        buttonText = strings.addFirstTransaction,
                        onButtonClick = { onQuickAction(TransactionType.CUSTOMER_DEBT) }
                    )
                }
            } else {
                items(recentTransactions, key = { it.transaction.id }) { item ->
                    TransactionListItem(item = item, onClick = { onTransactionClick(item.transaction) })
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun SummaryBalanceCard(
    title: String,
    amount: Long,
    currencyCode: String,
    decimalPlaces: Int,
    accentColor: Color,
    containerColor: Color,
    countLabel: String,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = AmountFormatter.formatAmount(amount, decimalPlaces, currencyCode),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
                color = Color(0xFF1A1C1E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionListItem(
    item: TransactionWithParty,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    val tx = item.transaction
    val type = TransactionType.from(tx.transactionType)
    val isDebit = type.isPositiveImpact

    val badgeColor = when (type) {
        TransactionType.CUSTOMER_DEBT -> Color(0xFF0061A4)
        TransactionType.CUSTOMER_PAYMENT -> Color(0xFF196838)
        TransactionType.SUPPLIER_DEBT -> Color(0xFFB3261E)
        TransactionType.SUPPLIER_PAYMENT -> Color(0xFF44474E)
        TransactionType.OPENING_RECEIVABLE -> Color(0xFF0061A4)
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.partyName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "$typeTitle • ${DateFormatter.formatDate(tx.occurredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    tx.dueAt?.let {
                        Text(
                            "${featureStrings.dueDate}: ${DateFormatter.formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    tx.note?.let { n ->
                        Text(
                            text = n,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }
            }

            val sign = if (isDebit) "+" else "-"
            Text(
                text = "$sign${AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDebit) Color(0xFF196838) else Color(0xFFB3261E)
            )
        }
    }
}
