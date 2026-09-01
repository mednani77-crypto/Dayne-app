package com.example.ui.parties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.core.localization.LocalLanguage
import com.example.core.localization.LocalStrings
import com.example.core.localization.V12StringsProvider
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.ui.components.EmptyStateView

private enum class BalanceFilter { ALL, DEBTORS, SETTLED }
private enum class PartySort { NAME, LARGEST_DEBT, RECENT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartiesScreen(
    parties: List<PartyWithBalances>,
    onPartyClick: (String) -> Unit,
    onAddPartyClick: () -> Unit,
    overduePartyIds: Set<String> = emptySet()
) {
    val strings = LocalStrings.current
    val v12 = V12StringsProvider.get(LocalLanguage.current)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var balanceFilter by remember { mutableStateOf(BalanceFilter.ALL) }
    var overdueOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(PartySort.NAME) }

    val filteredParties = remember(parties, selectedTab, searchQuery, balanceFilter, overdueOnly, sortMode, overduePartyIds) {
        val q = searchQuery.trim().lowercase()
        val digits = searchQuery.filter(Char::isDigit)
        val source = parties.filter { item ->
            val p = item.party
            val matchesTab = when (selectedTab) {
                0 -> !p.isArchived
                1 -> !p.isArchived && (p.partyType == PartyType.CUSTOMER.value || p.partyType == PartyType.BOTH.value)
                2 -> !p.isArchived && (p.partyType == PartyType.SUPPLIER.value || p.partyType == PartyType.BOTH.value)
                3 -> p.isArchived
                else -> true
            }
            val hasPositiveDebt = item.balances.any { it.customerBalance > 0L || it.supplierBalance > 0L }
            val isSettled = item.balances.isEmpty() || item.balances.all { it.customerBalance == 0L && it.supplierBalance == 0L }
            val matchesBalance = when (balanceFilter) {
                BalanceFilter.ALL -> true
                BalanceFilter.DEBTORS -> hasPositiveDebt
                BalanceFilter.SETTLED -> isSettled
            }
            val matchesOverdue = !overdueOnly || p.id in overduePartyIds
            val balanceText = item.balances.joinToString(" ") { bal ->
                listOf(
                    bal.currencyCode,
                    AmountFormatter.formatToInputString(kotlin.math.abs(bal.customerBalance), bal.decimalPlaces),
                    AmountFormatter.formatToInputString(kotlin.math.abs(bal.supplierBalance), bal.decimalPlaces)
                ).joinToString(" ")
            }.lowercase()
            val matchesSearch = q.isBlank() || p.name.lowercase().contains(q) ||
                p.phone.orEmpty().lowercase().contains(q) || p.notes.orEmpty().lowercase().contains(q) ||
                balanceText.contains(q) || (digits.isNotBlank() && balanceText.filter(Char::isDigit).contains(digits))
            matchesTab && matchesBalance && matchesOverdue && matchesSearch
        }
        when (sortMode) {
            PartySort.NAME -> source.sortedBy { it.party.name.lowercase() }
            PartySort.RECENT -> source.sortedByDescending { it.lastTransactionTime ?: 0L }
            PartySort.LARGEST_DEBT -> source.sortedByDescending { item -> item.balances.maxOfOrNull { maxOf(it.customerBalance, it.supplierBalance, 0L) } ?: 0L }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = { Text(strings.partiesTitle, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
                    actions = { IconButton(onClick = { showAdvanced = !showAdvanced }) { Icon(Icons.Default.FilterList, contentDescription = v12.advancedSearch) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("${strings.searchPlaceholder} • ${strings.amountLabel} • ${strings.notesLabel}") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("parties_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                if (showAdvanced) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(balanceFilter == BalanceFilter.ALL, { balanceFilter = BalanceFilter.ALL }, { Text(v12.allBalances) })
                            FilterChip(balanceFilter == BalanceFilter.DEBTORS, { balanceFilter = BalanceFilter.DEBTORS }, { Text(v12.debtorsOnly) })
                            FilterChip(balanceFilter == BalanceFilter.SETTLED, { balanceFilter = BalanceFilter.SETTLED }, { Text(v12.settledOnly) })
                            if (overduePartyIds.isNotEmpty()) FilterChip(overdueOnly, { overdueOnly = !overdueOnly }, { Text(v12.overdue) })
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(sortMode == PartySort.NAME, { sortMode = PartySort.NAME }, { Text(v12.sortName) })
                            FilterChip(sortMode == PartySort.LARGEST_DEBT, { sortMode = PartySort.LARGEST_DEBT }, { Text(v12.sortLargestDebt) })
                            FilterChip(sortMode == PartySort.RECENT, { sortMode = PartySort.RECENT }, { Text(v12.sortRecent) })
                        }
                    }
                }
                SecondaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                    listOf(strings.tabAll, strings.tabCustomers, strings.tabSuppliers, strings.tabArchived).forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPartyClick, modifier = Modifier.testTag("add_party_fab")) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = strings.addParty); Spacer(Modifier.width(6.dp)); Text(strings.addParty, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        if (filteredParties.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.People,
                title = strings.noPartiesFound,
                buttonText = if (selectedTab != 3) strings.addFirstParty else null,
                onButtonClick = onAddPartyClick,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredParties, key = { it.party.id }) { item ->
                    PartyListItemCard(item, overdue = item.party.id in overduePartyIds) { onPartyClick(item.party.id) }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartyListItemCard(item: PartyWithBalances, overdue: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val v12 = V12StringsProvider.get(LocalLanguage.current)
    val party = item.party
    val partyType = PartyType.from(party.partyType)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("party_card_${party.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (overdue) MaterialTheme.colorScheme.error.copy(alpha = .45f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (partyType == PartyType.CUSTOMER) Color(0xFFDDE2F1) else Color(0xFFFDE8E8)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Person, contentDescription = null, tint = if (partyType == PartyType.CUSTOMER) Color(0xFF0061A4) else Color(0xFFB3261E)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(party.name, fontWeight = FontWeight.SemiBold)
                        party.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (overdue) Text(v12.overdue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                    Text(
                        when (partyType) { PartyType.CUSTOMER -> strings.typeCustomer; PartyType.SUPPLIER -> strings.typeSupplier; PartyType.BOTH -> strings.typeBoth },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (item.balances.isEmpty()) {
                Text(strings.statusSettled, color = Color(0xFF196838), fontWeight = FontWeight.SemiBold)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.balances.forEach { bal ->
                        if (partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH) BalanceStatusChip(
                            if (partyType == PartyType.BOTH) "${strings.typeCustomer}: ${bal.getCustomerStatusText(strings)}" else bal.getCustomerStatusText(strings),
                            bal.customerBalance > 0, bal.customerBalance == 0L
                        )
                        if (partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH) BalanceStatusChip(
                            if (partyType == PartyType.BOTH) "${strings.typeSupplier}: ${bal.getSupplierStatusText(strings)}" else bal.getSupplierStatusText(strings),
                            bal.supplierBalance > 0, bal.supplierBalance == 0L
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceStatusChip(text: String, isDebt: Boolean, isSettled: Boolean) {
    val bg = when { isSettled -> Color(0xFFEAF5EE); isDebt -> Color(0xFFDDE2F1); else -> Color(0xFFFDE8E8) }
    val fg = when { isSettled -> Color(0xFF196838); isDebt -> Color(0xFF0061A4); else -> Color(0xFFB3261E) }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp), color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
