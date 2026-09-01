package com.example.ui.activity

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.localization.V12StringsProvider
import com.example.data.models.TransactionType
import com.example.data.models.TransactionWithParty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    transactions: List<TransactionWithParty>,
    language: AppLanguage,
    onBack: () -> Unit,
    onOpenParty: (String) -> Unit
) {
    val t = V12StringsProvider.get(language)
    val s = AppStrings.get(language)
    var filter by remember { mutableIntStateOf(0) }
    val activity = remember(transactions, filter) {
        transactions.filter {
            when (TransactionType.from(it.transaction.transactionType)) {
                TransactionType.CUSTOMER_PAYMENT -> filter == 0 || filter == 1
                TransactionType.SUPPLIER_PAYMENT -> filter == 0 || filter == 2
                else -> false
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(
            title = { Column { Text(t.activity, fontWeight = FontWeight.Bold); Text(t.collectionActivity, style = MaterialTheme.typography.bodySmall) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
        ) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(filter == 0, { filter = 0 }, { Text(s.filterAll) })
                    FilterChip(filter == 1, { filter = 1 }, { Text(s.customerPaymentsReceived) })
                    FilterChip(filter == 2, { filter = 2 }, { Text(s.supplierPaymentsPaid) })
                }
            }
            items(activity, key = { it.transaction.id }) { item ->
                val tx = item.transaction
                val type = TransactionType.from(tx.transactionType)
                Card(
                    onClick = { onOpenParty(tx.partyId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(item.partyName, fontWeight = FontWeight.SemiBold)
                                Text(if (type == TransactionType.CUSTOMER_PAYMENT) s.customerPaymentsReceived else s.supplierPaymentsPaid, style = MaterialTheme.typography.bodySmall)
                                Text(DateFormatter.formatDateTime(tx.occurredAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(AmountFormatter.formatAmount(tx.amountMinor, tx.currencyDecimalPlaces, tx.currencyCode), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
