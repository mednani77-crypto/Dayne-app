package com.example.ui.reports

import android.app.DatePickerDialog
import java.util.Calendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.models.PartyWithBalances
import com.example.data.models.TransactionReportSummary

private enum class ReportPeriod { TODAY, LAST_7, LAST_30, THIS_MONTH, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    defaultCurrencyCode: String,
    currencies: List<CurrencyEntity>,
    parties: List<PartyWithBalances>,
    onLoadReport: suspend (currencyCode: String, fromTime: Long, toTime: Long, partyId: String?) -> TransactionReportSummary,
    onExportCsv: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var selectedCurrencyCode by remember(defaultCurrencyCode) { mutableStateOf(defaultCurrencyCode) }
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var selectedPartyId by remember { mutableStateOf<String?>(null) }
    var partyMenuExpanded by remember { mutableStateOf(false) }
    var customFrom by remember { mutableLongStateOf(firstDayOfCurrentMonth()) }
    var customTo by remember { mutableLongStateOf(DateFormatter.endOfDay(System.currentTimeMillis())) }
    var reportSummary by remember { mutableStateOf<TransactionReportSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val range = remember(selectedPeriod, customFrom, customTo) {
        reportRange(selectedPeriod, customFrom, customTo)
    }

    LaunchedEffect(selectedCurrencyCode, range.first, range.second, selectedPartyId) {
        isLoading = true
        reportSummary = onLoadReport(selectedCurrencyCode, range.first, range.second, selectedPartyId)
        isLoading = false
    }

    fun pickDate(initial: Long, onSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initial }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onSelected(picked)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.reportsTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    Button(
                        onClick = onExportCsv,
                        modifier = Modifier.padding(end = 8.dp).testTag("export_csv_button")
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.FileDownload, contentDescription = null)
                        Text(strings.exportCsv)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencies.filter { it.isEnabled }.size) { index ->
                        val currency = currencies.filter { it.isEnabled }[index]
                        FilterChip(
                            selected = currency.code == selectedCurrencyCode,
                            onClick = { selectedCurrencyCode = currency.code },
                            label = { Text(currency.code, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val periods = listOf(
                        ReportPeriod.TODAY to strings.periodToday,
                        ReportPeriod.LAST_7 to strings.periodLast7Days,
                        ReportPeriod.LAST_30 to strings.periodLast30Days,
                        ReportPeriod.THIS_MONTH to strings.periodThisMonth,
                        ReportPeriod.CUSTOM to strings.periodCustom
                    )
                    items(periods.size) { index ->
                        val (period, label) = periods[index]
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (selectedPeriod == ReportPeriod.CUSTOM) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                pickDate(customFrom) { customFrom = DateFormatter.startOfDay(it) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(DateFormatter.formatDate(customFrom)) }
                        Button(
                            onClick = {
                                pickDate(customTo) { picked ->
                                    val end = DateFormatter.endOfDay(picked)
                                    if (end >= customFrom) customTo = end
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(DateFormatter.formatDate(customTo)) }
                    }
                }
            }

            item {
                val selectedPartyName = parties.find { it.party.id == selectedPartyId }?.party?.name ?: strings.tabAll
                ExposedDropdownMenuBox(
                    expanded = partyMenuExpanded,
                    onExpandedChange = { partyMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPartyName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.partiesTitle) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(partyMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = partyMenuExpanded, onDismissRequest = { partyMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.tabAll) },
                            onClick = {
                                selectedPartyId = null
                                partyMenuExpanded = false
                            }
                        )
                        parties.filter { !it.party.isArchived }.sortedBy { it.party.name.lowercase() }.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.party.name) },
                                onClick = {
                                    selectedPartyId = item.party.id
                                    partyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            reportSummary?.let { report ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            strings.receivableTitle,
                            report.totalCustomerReceivableBalance,
                            report,
                            Color(0xFF196838),
                            Modifier.weight(1f)
                        )
                        MetricCard(
                            strings.payableTitle,
                            report.totalSupplierPayableBalance,
                            report,
                            Color(0xFFB3261E),
                            Modifier.weight(1f)
                        )
                    }
                }

                item {
                    ReportSection(
                        title = strings.reportsTitle,
                        rows = listOf(
                            strings.newDebtsOnCustomers to report.newCustomerDebtsInPeriod,
                            strings.customerPaymentsReceived to report.customerPaymentsInPeriod,
                            strings.newDebtsToSuppliers to report.newSupplierDebtsInPeriod,
                            strings.supplierPaymentsPaid to report.supplierPaymentsInPeriod,
                            strings.customerCreditsSum to report.totalCustomerCredits,
                            strings.supplierAdvancesSum to report.totalSupplierAdvances
                        ),
                        report = report
                    )
                }

                if (report.topDebtorCustomers.isNotEmpty()) {
                    item {
                        RankedSection(
                            title = strings.topDebtorCustomers,
                            rows = report.topDebtorCustomers.map { it.first.name to it.second },
                            report = report,
                            accent = Color(0xFF196838)
                        )
                    }
                }

                if (report.topCreditorSuppliers.isNotEmpty()) {
                    item {
                        RankedSection(
                            title = strings.topCreditorSuppliers,
                            rows = report.topCreditorSuppliers.map { it.first.name to it.second },
                            report = report,
                            accent = Color(0xFFB3261E)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.totalTxCount)
                            Text(report.totalTransactionCount.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

private fun reportRange(period: ReportPeriod, customFrom: Long, customTo: Long): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val end = DateFormatter.endOfDay(now)
    val cal = Calendar.getInstance()
    return when (period) {
        ReportPeriod.TODAY -> DateFormatter.startOfDay(now) to end
        ReportPeriod.LAST_7 -> {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -6)
            DateFormatter.startOfDay(cal.timeInMillis) to end
        }
        ReportPeriod.LAST_30 -> {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -29)
            DateFormatter.startOfDay(cal.timeInMillis) to end
        }
        ReportPeriod.THIS_MONTH -> firstDayOfCurrentMonth() to end
        ReportPeriod.CUSTOM -> customFrom.coerceAtMost(customTo) to customTo.coerceAtLeast(customFrom)
    }
}

private fun firstDayOfCurrentMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
private fun MetricCard(
    title: String,
    amount: Long,
    report: TransactionReportSummary,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                AmountFormatter.formatAmount(amount, report.decimalPlaces, report.currencyCode),
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReportSection(
    title: String,
    rows: List<Pair<String, Long>>,
    report: TransactionReportSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            rows.forEach { (label, amount) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, modifier = Modifier.weight(1f))
                    Text(AmountFormatter.formatAmount(amount, report.decimalPlaces, report.currencyCode), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RankedSection(
    title: String,
    rows: List<Pair<String, Long>>,
    report: TransactionReportSummary,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            rows.forEach { (name, amount) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, modifier = Modifier.weight(1f))
                    Text(AmountFormatter.formatAmount(amount, report.decimalPlaces, report.currencyCode), color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
