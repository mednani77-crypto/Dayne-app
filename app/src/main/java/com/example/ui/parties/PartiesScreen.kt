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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.example.core.localization.LocalStrings
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartiesScreen(
    parties: List<PartyWithBalances>,
    onPartyClick: (String) -> Unit,
    onAddPartyClick: () -> Unit
) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Customers, 2: Suppliers, 3: Archived
    var searchQuery by remember { mutableStateOf("") }

    val filteredParties = remember(parties, selectedTab, searchQuery) {
        parties.filter { item ->
            val p = item.party
            val matchesTab = when (selectedTab) {
                0 -> !p.isArchived // All active
                1 -> !p.isArchived && (p.partyType == PartyType.CUSTOMER.value || p.partyType == PartyType.BOTH.value)
                2 -> !p.isArchived && (p.partyType == PartyType.SUPPLIER.value || p.partyType == PartyType.BOTH.value)
                3 -> p.isArchived
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                p.name.lowercase().contains(q) || (p.phone?.contains(q) == true)
            }

            matchesTab && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = strings.partiesTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("parties_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tabs: All, Customers, Suppliers, Archived
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    val tabTitles = listOf(
                        strings.tabAll,
                        strings.tabCustomers,
                        strings.tabSuppliers,
                        strings.tabArchived
                    )
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = index },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPartyClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_party_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = strings.addParty)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.addParty, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (filteredParties.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.People,
                title = strings.noPartiesFound,
                buttonText = if (selectedTab != 3) strings.addFirstParty else null,
                onButtonClick = onAddPartyClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredParties, key = { it.party.id }) { item ->
                    PartyListItemCard(
                        item = item,
                        onClick = { onPartyClick(item.party.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartyListItemCard(
    item: PartyWithBalances,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val party = item.party
    val partyType = PartyType.from(party.partyType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("party_card_${party.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (partyType == PartyType.CUSTOMER) Color(0xFFDDE2F1) else Color(0xFFFDE8E8)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (partyType == PartyType.CUSTOMER) Color(0xFF0061A4) else Color(0xFFB3261E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        party.phone?.let { p ->
                            Text(
                                text = p,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Party Type Chip Badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = when (partyType) {
                            PartyType.CUSTOMER -> strings.typeCustomer
                            PartyType.SUPPLIER -> strings.typeSupplier
                            PartyType.BOTH -> strings.typeBoth
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Balances per Currency
            if (item.balances.isEmpty()) {
                Text(
                    text = strings.statusSettled,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF196838)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.balances.forEach { bal ->
                        val isCustomer = partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
                        val isSupplier = partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH

                        if (isCustomer) {
                            val text = bal.getCustomerStatusText(strings)
                            val isPositive = bal.customerBalance > 0
                            BalanceStatusChip(
                                text = if (partyType == PartyType.BOTH) "${strings.typeCustomer}: $text" else text,
                                isDebt = isPositive,
                                isSettled = bal.customerBalance == 0L
                            )
                        }

                        if (isSupplier) {
                            val text = bal.getSupplierStatusText(strings)
                            val isPositive = bal.supplierBalance > 0
                            BalanceStatusChip(
                                text = if (partyType == PartyType.BOTH) "${strings.typeSupplier}: $text" else text,
                                isDebt = isPositive,
                                isSettled = bal.supplierBalance == 0L
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceStatusChip(
    text: String,
    isDebt: Boolean,
    isSettled: Boolean
) {
    val bgColor = when {
        isSettled -> Color(0xFFEAF5EE)
        isDebt -> Color(0xFFDDE2F1)
        else -> Color(0xFFFDE8E8)
    }
    val textColor = when {
        isSettled -> Color(0xFF196838)
        isDebt -> Color(0xFF0061A4)
        else -> Color(0xFFB3261E)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
