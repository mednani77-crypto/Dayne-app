package com.example.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.models.TransactionReportSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    defaultCurrencyCode: String,
    currencies: List<CurrencyEntity>,
    onLoadReport: suspend (currencyCode: String, fromTime: Long, toTime: Long) -> TransactionReportSummary,
    onExportCsv: () -> Unit
) {
    val strings = LocalStrings.current

    var selectedCurrencyCode by remember { mutableStateOf(defaultCurrencyCode) }
    var selectedPeriodIndex by remember { mutableIntStateOf(2) } // 0: Today, 1: 7 Days, 2: This Month, 3: All Time
    var reportSummary by remember { mutableStateOf<TransactionReportSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val now = System.currentTimeMillis()
    val fromTimestamp = remember(selectedPeriodIndex) {
        val cal = java.util.Calendar.getInstance()
        when (selectedPeriodIndex) {
            0 -> { // Today
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            1 -> { // Last 7 days
                now - (7L * 24 * 60 * 60 * 1000)
            }
            2 -> { // This month
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L // All time
        }
    }

    LaunchedEffect(selectedCurrencyCode, fromTimestamp) {
        isLoading = true
        reportSummary = onLoadReport(selectedCurrencyCode, fromTimestamp, now)
        isLoading = false
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = strings.reportsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        Button(
                            onClick = onExportCsv,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportCsv, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                // Period Tabs
                SecondaryTabRow(
                    selectedTabIndex = selectedPeriodIndex,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    val periods: List<String> = listOf(
                        strings.periodToday,
                        strings.periodLast7Days,
                        strings.periodThisMonth,
                        strings.periodCustom
                    )
                    periods.forEachIndexed { idx, title ->
                        val isSelected = selectedPeriodIndex == idx
                        Tab(
                            selected = isSelected,
                            onClick = { selectedPeriodIndex = idx },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
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
                Spacer(modifier = Modifier.height(6.dp))

                // Currency selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.filter { it.isEnabled }.forEach { cur ->
                        FilterChip(
                            selected = cur.code == selectedCurrencyCode,
                            onClick = { selectedCurrencyCode = cur.code },
                            label = { Text(cur.code, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            reportSummary?.let { rep ->
                item {
                    // Overall Totals for this currency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportMetricCard(
                            title = strings.receivableTitle,
                            amount = rep.totalCustomerReceivableBalance,
                            currencyCode = rep.currencyCode,
                            decimalPlaces = rep.decimalPlaces,
                            accentColor = Color(0xFF196838),
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = strings.payableTitle,
                            amount = rep.totalSupplierPayableBalance,
                            currencyCode = rep.currencyCode,
                            decimalPlaces = rep.decimalPlaces,
                            accentColor = Color(0xFFB3261E),
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Period Movement Breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = strings.reportsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            ReportBreakdownRow(
                                title = strings.newDebtsOnCustomers,
                                amount = rep.newCustomerDebtsInPeriod,
                                currency = rep.currencyCode,
                                decimals = rep.decimalPlaces,
                                color = Color(0xFF196838)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ReportBreakdownRow(
                                title = strings.customerPaymentsReceived,
                                amount = rep.customerPaymentsInPeriod,
                                currency = rep.currencyCode,
                                decimals = rep.decimalPlaces,
                                color = Color(0xFF0061A4)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ReportBreakdownRow(
                                title = strings.newDebtsToSuppliers,
                                amount = rep.newSupplierDebtsInPeriod,
                                currency = rep.currencyCode,
                                decimals = rep.decimalPlaces,
                                color = Color(0xFFB3261E)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ReportBreakdownRow(
                                title = strings.supplierPaymentsPaid,
                                amount = rep.supplierPaymentsInPeriod,
                                currency = rep.currencyCode,
                                decimals = rep.decimalPlaces,
                                color = Color(0xFF0061A4)
                            )
                        }
                    }
                }

                // Top Debtor Customers
                if (rep.topDebtorCustomers.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = strings.topDebtorCustomers,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                rep.topDebtorCustomers.forEach { (party, bal) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFDDE2F1)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0061A4),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                party.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = AmountFormatter.formatAmount(bal, rep.decimalPlaces, rep.currencyCode),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF196838)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Creditor Suppliers
                if (rep.topCreditorSuppliers.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = strings.topCreditorSuppliers,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                rep.topCreditorSuppliers.forEach { (party, bal) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFFDE8E8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB3261E),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                party.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = AmountFormatter.formatAmount(bal, rep.decimalPlaces, rep.currencyCode),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFFB3261E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    amount: Long,
    currencyCode: String,
    decimalPlaces: Int,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AmountFormatter.formatAmount(amount, decimalPlaces, currencyCode),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                color = accentColor
            )
        }
    }
}

@Composable
private fun ReportBreakdownRow(
    title: String,
    amount: Long,
    currency: String,
    decimals: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = AmountFormatter.formatAmount(amount, decimals, currency),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}
