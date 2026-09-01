package com.example.ui.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.localization.V12StringsProvider
import com.example.data.models.CollectionItem
import com.example.data.models.CollectionStatus

private enum class CollectionFilter { ALL, OVERDUE, TODAY, SOON }
private enum class CollectionSort { MOST_OVERDUE, LARGEST, OLDEST_DUE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionCenterScreen(
    items: List<CollectionItem>,
    onBack: () -> Unit,
    onOpenParty: (CollectionItem) -> Unit,
    onWhatsAppReminder: (CollectionItem) -> Unit,
    onRecordPayment: (CollectionItem) -> Unit
) {
    val language = LocalLanguage.current
    val strings = FeatureStringsProvider.get(language)
    val v12 = V12StringsProvider.get(language)
    var filter by remember { mutableStateOf(CollectionFilter.ALL) }
    var sort by remember { mutableStateOf(CollectionSort.MOST_OVERDUE) }

    val filtered = remember(items, filter, sort) {
        val base = items.filter { item ->
            when (filter) {
                CollectionFilter.ALL -> true
                CollectionFilter.OVERDUE -> item.status == CollectionStatus.OVERDUE
                CollectionFilter.TODAY -> item.status == CollectionStatus.DUE_TODAY
                CollectionFilter.SOON -> item.status == CollectionStatus.DUE_SOON || item.status == CollectionStatus.UPCOMING
            }
        }
        when (sort) {
            CollectionSort.MOST_OVERDUE -> base.sortedWith(compareBy<CollectionItem> { it.dayDelta }.thenByDescending { it.outstandingAmountMinor })
            CollectionSort.LARGEST -> base.sortedByDescending { it.outstandingAmountMinor }
            CollectionSort.OLDEST_DUE -> base.sortedBy { it.dueAt }
        }
    }

    Scaffold(
        topBar = { TopAppBar(
            title = { Column { Text(strings.collectionCenter, fontWeight = FontWeight.Bold); Text(strings.collectionSubtitle, style = MaterialTheme.typography.bodySmall) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
        ) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(filter == CollectionFilter.ALL, { filter = CollectionFilter.ALL }, { Text(strings.collectionCenter) })
                    FilterChip(filter == CollectionFilter.OVERDUE, { filter = CollectionFilter.OVERDUE }, { Text(strings.overdue) })
                    FilterChip(filter == CollectionFilter.TODAY, { filter = CollectionFilter.TODAY }, { Text(strings.dueToday) })
                    FilterChip(filter == CollectionFilter.SOON, { filter = CollectionFilter.SOON }, { Text(strings.dueSoon) })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(sort == CollectionSort.MOST_OVERDUE, { sort = CollectionSort.MOST_OVERDUE }, { Text(v12.overdue) })
                    FilterChip(sort == CollectionSort.LARGEST, { sort = CollectionSort.LARGEST }, { Text(v12.sortLargestDebt) })
                    FilterChip(sort == CollectionSort.OLDEST_DUE, { sort = CollectionSort.OLDEST_DUE }, { Text(v12.sortRecent) })
                }
            }

            if (filtered.isEmpty()) {
                item { Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary); Text(strings.noCollections)
                    }
                } }
            } else {
                items(filtered, key = { it.debtTransactionId }) { item -> CollectionCard(item, { onOpenParty(item) }, { onWhatsAppReminder(item) }, { onRecordPayment(item) }) }
            }
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}

@Composable
private fun CollectionCard(item: CollectionItem, onOpen: () -> Unit, onWhatsApp: () -> Unit, onPayment: () -> Unit) {
    val strings = FeatureStringsProvider.get(LocalLanguage.current)
    val statusText = when (item.status) {
        CollectionStatus.OVERDUE -> strings.overdueDays(-item.dayDelta)
        CollectionStatus.DUE_TODAY -> strings.dueToday
        CollectionStatus.DUE_SOON, CollectionStatus.UPCOMING -> strings.dueInDays(item.dayDelta)
    }
    val statusColor = when (item.status) {
        CollectionStatus.OVERDUE -> MaterialTheme.colorScheme.error
        CollectionStatus.DUE_TODAY -> Color(0xFFB65D00)
        CollectionStatus.DUE_SOON -> Color(0xFF0061A4)
        CollectionStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) { Text(item.party.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); item.party.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
                Text(AmountFormatter.formatAmount(item.outstandingAmountMinor, item.decimalPlaces, item.currencyCode), fontWeight = FontWeight.Bold, color = statusColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = statusColor)
                Text("${strings.dueDate}: ${DateFormatter.formatDate(item.dueAt)} • $statusText", modifier = Modifier.padding(start = 6.dp), color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            item.note?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.party.phone.isNullOrBlank()) {
                    Button(onClick = onWhatsApp, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E))) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null); Text(strings.remindWhatsApp, modifier = Modifier.padding(start = 5.dp))
                    }
                }
                Button(onClick = onPayment, modifier = Modifier.weight(1f)) { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null); Text(strings.recordPayment, modifier = Modifier.padding(start = 5.dp)) }
            }
        }
    }
}
