package com.example.ui.transactions

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyWithBalances
import com.example.data.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionDialog(
    initialTransaction: LedgerTransactionEntity? = null,
    preselectedType: TransactionType? = null,
    preselectedPartyId: String? = null,
    defaultCurrencyCode: String,
    partiesWithBalances: List<PartyWithBalances>,
    currencies: List<CurrencyEntity>,
    onDismiss: () -> Unit,
    onOpenAddParty: () -> Unit,
    onSaveTransaction: (
        partyId: String,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int,
        occurredAt: Long,
        note: String?
    ) -> Unit
) {
    val strings = LocalStrings.current

    var selectedType by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                TransactionType.from(initialTransaction.transactionType)
            } else preselectedType ?: TransactionType.CUSTOMER_DEBT
        )
    }

    var selectedPartyId by remember {
        mutableStateOf(initialTransaction?.partyId ?: preselectedPartyId ?: "")
    }

    var selectedCurrencyCode by remember {
        mutableStateOf(initialTransaction?.currencyCode ?: defaultCurrencyCode)
    }

    val activeCurrency = currencies.find { it.code == selectedCurrencyCode }
        ?: CurrencyEntity(code = selectedCurrencyCode, name = selectedCurrencyCode, decimalPlaces = 0)

    var amountInput by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                AmountFormatter.formatToInputString(initialTransaction.amountMinor, initialTransaction.currencyDecimalPlaces)
            } else ""
        )
    }

    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var occurredAt by remember { mutableLongStateOf(initialTransaction?.occurredAt ?: System.currentTimeMillis()) }

    var partyDropdownExpanded by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    var partyError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Filter candidate parties according to selected transaction type
    val filteredParties = remember(partiesWithBalances, selectedType) {
        partiesWithBalances.filter { !it.party.isArchived }
    }

    val selectedPartyWithBal = partiesWithBalances.find { it.party.id == selectedPartyId }

    // Check for Overpayment / Advance warning
    val parsedMinor = AmountFormatter.parseToMinorUnits(amountInput, activeCurrency.decimalPlaces) ?: 0L
    val warningMessage = remember(selectedPartyWithBal, selectedType, parsedMinor, selectedCurrencyCode) {
        if (selectedPartyWithBal != null && parsedMinor > 0L) {
            val bal = selectedPartyWithBal.getBalanceForCurrency(selectedCurrencyCode)
            when (selectedType) {
                TransactionType.CUSTOMER_PAYMENT -> {
                    val currentOwed = bal?.customerBalance ?: 0L
                    if (parsedMinor > currentOwed && currentOwed > 0L) strings.overpaymentWarning else null
                }
                TransactionType.SUPPLIER_PAYMENT -> {
                    val currentOwed = bal?.supplierBalance ?: 0L
                    if (parsedMinor > currentOwed && currentOwed > 0L) strings.advancePaymentWarning else null
                }
                else -> null
            }
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialTransaction == null) strings.addTransaction else strings.edit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 4 Transaction Type Selection Cards
                Text(
                    text = strings.partyTypeLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Top row: Customer types
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeSelectCard(
                        title = strings.quickActionCustomerDebt,
                        subtitle = strings.txSubtitleCustomerDebt,
                        isSelected = selectedType == TransactionType.CUSTOMER_DEBT,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = Color(0xFF196838),
                        onClick = { selectedType = TransactionType.CUSTOMER_DEBT },
                        modifier = Modifier.weight(1f)
                    )
                    TypeSelectCard(
                        title = strings.quickActionCustomerPayment,
                        subtitle = strings.txSubtitleCustomerPayment,
                        isSelected = selectedType == TransactionType.CUSTOMER_PAYMENT,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = Color(0xFF0061A4),
                        onClick = { selectedType = TransactionType.CUSTOMER_PAYMENT },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row: Supplier types
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeSelectCard(
                        title = strings.quickActionSupplierDebt,
                        subtitle = strings.txSubtitleSupplierDebt,
                        isSelected = selectedType == TransactionType.SUPPLIER_DEBT,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = Color(0xFFB3261E),
                        onClick = { selectedType = TransactionType.SUPPLIER_DEBT },
                        modifier = Modifier.weight(1f)
                    )
                    TypeSelectCard(
                        title = strings.quickActionSupplierPayment,
                        subtitle = strings.txSubtitleSupplierPayment,
                        isSelected = selectedType == TransactionType.SUPPLIER_PAYMENT,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = Color(0xFF0061A4),
                        onClick = { selectedType = TransactionType.SUPPLIER_PAYMENT },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Party Selector with Exposed Dropdown & Add Option
                ExposedDropdownMenuBox(
                    expanded = partyDropdownExpanded,
                    onExpandedChange = { partyDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPartyWithBal?.party?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectParty + " *") },
                        placeholder = { Text(strings.selectParty) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                        isError = partyError,
                        supportingText = if (partyError) {
                            { Text(strings.errorSelectParty, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("tx_party_selector"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = partyDropdownExpanded,
                        onDismissRequest = { partyDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.addNewPartyOption,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                partyDropdownExpanded = false
                                onOpenAddParty()
                            }
                        )

                        filteredParties.forEach { item ->
                            val party = item.party
                            val bal = item.getBalanceForCurrency(selectedCurrencyCode)
                            val balText = when (selectedType) {
                                TransactionType.CUSTOMER_DEBT, TransactionType.CUSTOMER_PAYMENT -> bal?.getCustomerStatusText(strings) ?: ""
                                else -> bal?.getSupplierStatusText(strings) ?: ""
                            }

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(party.name, fontWeight = FontWeight.SemiBold)
                                        if (balText.isNotBlank()) {
                                            Text(
                                                text = balText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedPartyId = party.id
                                    partyError = false
                                    partyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount and Currency Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            if (it.isNotBlank()) amountError = false
                        },
                        label = { Text(strings.amountLabel + " *") },
                        isError = amountError,
                        supportingText = if (amountError) {
                            { Text(strings.errorAmountMustBePositive, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("tx_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // Currency Dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyDropdownExpanded,
                        onExpandedChange = { currencyDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrencyCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.currencyLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = currencyDropdownExpanded,
                            onDismissRequest = { currencyDropdownExpanded = false }
                        ) {
                            currencies.filter { it.isEnabled }.forEach { cur ->
                                DropdownMenuItem(
                                    text = { Text(cur.code, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCurrencyCode = cur.code
                                        currencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Overpayment / Advance Warning Alert
                if (warningMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warningMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.dateLabel + ":",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Text(
                        text = DateFormatter.formatDateTime(occurredAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Note Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chips = listOf(
                        strings.noteChipCash,
                        strings.noteChipWaafi,
                        strings.noteChipGoods,
                        strings.noteChipPartial
                    )
                    chips.forEach { chipText ->
                        FilterChip(
                            selected = note == chipText,
                            onClick = { note = chipText },
                            label = { Text(chipText, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val minor = AmountFormatter.parseToMinorUnits(amountInput, activeCurrency.decimalPlaces)

                    if (selectedPartyId.isBlank()) {
                        partyError = true
                        return@Button
                    }
                    if (minor == null || minor <= 0L) {
                        amountError = true
                        return@Button
                    }

                    isSaving = true
                    onSaveTransaction(
                        selectedPartyId,
                        selectedType,
                        minor,
                        selectedCurrencyCode,
                        activeCurrency.decimalPlaces,
                        occurredAt,
                        note.trim().takeIf { it.isNotBlank() }
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_transaction_button"),
                enabled = !isSaving
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun TypeSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
