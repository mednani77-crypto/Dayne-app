package com.example.ui.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Calendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.FeatureStringsProvider
import com.example.core.localization.LocalLanguage
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.models.PartyType
import com.example.data.models.PartyWithBalances
import com.example.data.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
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
        note: String?,
        dueAt: Long?,
        attachmentUri: Uri?,
        removeAttachment: Boolean
    ) -> Unit
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(LocalLanguage.current)
    val context = LocalContext.current
    val amountFocusRequester = remember { FocusRequester() }

    var selectedType by remember {
        mutableStateOf(initialTransaction?.let { TransactionType.from(it.transactionType) } ?: preselectedType ?: TransactionType.CUSTOMER_DEBT)
    }
    var selectedPartyId by remember { mutableStateOf(initialTransaction?.partyId ?: preselectedPartyId.orEmpty()) }
    var selectedCurrencyCode by remember { mutableStateOf(initialTransaction?.currencyCode ?: defaultCurrencyCode) }
    var amountInput by remember {
        mutableStateOf(
            initialTransaction?.let {
                AmountFormatter.formatToInputString(it.amountMinor, it.currencyDecimalPlaces)
            }.orEmpty()
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note.orEmpty()) }
    var occurredAt by remember { mutableLongStateOf(initialTransaction?.occurredAt ?: System.currentTimeMillis()) }
    var dueAt by remember { mutableStateOf(initialTransaction?.dueAt) }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var removeAttachment by remember { mutableStateOf(false) }

    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            attachmentUri = uri
            removeAttachment = false
        }
    }

    var partyMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var partyError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val activeCurrency = currencies.find { it.code == selectedCurrencyCode }
        ?: CurrencyEntity(code = selectedCurrencyCode, name = selectedCurrencyCode, decimalPlaces = 0)

    val isDebtType = selectedType == TransactionType.CUSTOMER_DEBT || selectedType == TransactionType.SUPPLIER_DEBT

    val filteredParties = remember(partiesWithBalances, selectedType) {
        partiesWithBalances.filter { item ->
            if (item.party.isArchived) return@filter false
            val type = PartyType.from(item.party.partyType)
            if (selectedType.isReceivableImpact) {
                type == PartyType.CUSTOMER || type == PartyType.BOTH
            } else {
                type == PartyType.SUPPLIER || type == PartyType.BOTH
            }
        }
    }

    val selectedParty = partiesWithBalances.find { it.party.id == selectedPartyId }

    LaunchedEffect(selectedType) {
        val selectedTypeOfParty = selectedParty?.party?.let { PartyType.from(it.partyType) }
        val compatible = if (selectedType.isReceivableImpact) {
            selectedTypeOfParty == PartyType.CUSTOMER || selectedTypeOfParty == PartyType.BOTH
        } else {
            selectedTypeOfParty == PartyType.SUPPLIER || selectedTypeOfParty == PartyType.BOTH
        }
        if (selectedPartyId.isNotBlank() && !compatible) selectedPartyId = ""
    }

    LaunchedEffect(Unit) {
        if (initialTransaction == null) amountFocusRequester.requestFocus()
    }

    val parsedMinor = AmountFormatter.parseToMinorUnits(amountInput, activeCurrency.decimalPlaces) ?: 0L
    val overpaymentWarning = remember(selectedParty, selectedType, parsedMinor, selectedCurrencyCode) {
        if (selectedParty == null || parsedMinor <= 0L) return@remember null
        val balance = selectedParty.getBalanceForCurrency(selectedCurrencyCode)
        when (selectedType) {
            TransactionType.CUSTOMER_PAYMENT -> {
                val outstanding = balance?.customerBalance ?: 0L
                if (parsedMinor > outstanding) strings.overpaymentWarning else null
            }
            TransactionType.SUPPLIER_PAYMENT -> {
                val outstanding = balance?.supplierBalance ?: 0L
                if (parsedMinor > outstanding) strings.advancePaymentWarning else null
            }
            else -> null
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = occurredAt }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = occurredAt
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                occurredAt = updated.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openDueDatePicker() {
        val seed = dueAt ?: occurredAt
        val cal = Calendar.getInstance().apply { timeInMillis = seed }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    clear()
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dueAt = updated.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = occurredAt }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = occurredAt
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                occurredAt = updated.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTransaction == null) strings.addTransaction else strings.edit, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransactionTypeGrid(selectedType = selectedType, onSelected = { selectedType = it })

                ExposedDropdownMenuBox(expanded = partyMenuExpanded, onExpandedChange = { partyMenuExpanded = it }) {
                    OutlinedTextField(
                        value = selectedParty?.party?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectParty) },
                        isError = partyError,
                        supportingText = if (partyError) ({ Text(strings.errorSelectParty) }) else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(partyMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("tx_party_selector")
                    )
                    ExposedDropdownMenu(expanded = partyMenuExpanded, onDismissRequest = { partyMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.addNewPartyOption, color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                partyMenuExpanded = false
                                onOpenAddParty()
                            }
                        )
                        filteredParties.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.party.name) },
                                onClick = {
                                    selectedPartyId = item.party.id
                                    partyError = false
                                    partyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            amountError = false
                        },
                        label = { Text(strings.amountLabel) },
                        isError = amountError,
                        supportingText = if (amountError) ({ Text(strings.errorAmountMustBePositive) }) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .focusRequester(amountFocusRequester)
                            .testTag("tx_amount_input")
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyMenuExpanded,
                        onExpandedChange = { currencyMenuExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrencyCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.currencyLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = currencyMenuExpanded, onDismissRequest = { currencyMenuExpanded = false }) {
                            currencies.filter { it.isEnabled }.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.code, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCurrencyCode = currency.code
                                        amountInput = ""
                                        amountError = false
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                overpaymentWarning?.let { warning ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Text(warning, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).clickable { openDatePicker() },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Text(DateFormatter.formatDate(occurredAt), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable { openTimePicker() },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Text(DateFormatter.formatTime(occurredAt), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }

                if (isDebtType) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { openDueDatePicker() },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(featureStrings.dueDate, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        dueAt?.let(DateFormatter::formatDate) ?: featureStrings.noDueDate,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (dueAt != null) {
                                TextButton(onClick = { dueAt = null }) { Text(featureStrings.clearDueDate) }
                            } else {
                                Text(featureStrings.setDueDate, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { attachmentPicker.launch(arrayOf("image/*", "application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Text(
                                if (initialTransaction?.attachmentPath != null || attachmentUri != null) featureStrings.replaceAttachment else featureStrings.attachDocument,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        if ((initialTransaction?.attachmentPath != null && !removeAttachment) || attachmentUri != null) {
                            Text(featureStrings.attachmentAdded, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(
                                onClick = {
                                    attachmentUri = null
                                    removeAttachment = true
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Text(featureStrings.removeAttachment, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                modifier = Modifier.testTag("save_transaction_button"),
                onClick = {
                    if (selectedPartyId.isBlank()) {
                        partyError = true
                        return@Button
                    }
                    val minor = AmountFormatter.parseToMinorUnits(amountInput, activeCurrency.decimalPlaces)
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
                        note.trim().takeIf { it.isNotBlank() },
                        if (isDebtType) dueAt else null,
                        attachmentUri,
                        removeAttachment
                    )
                }
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun TransactionTypeGrid(
    selectedType: TransactionType,
    onSelected: (TransactionType) -> Unit
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeCard(strings.quickActionCustomerDebt, selectedType == TransactionType.CUSTOMER_DEBT, Color(0xFF196838), Modifier.weight(1f)) {
                onSelected(TransactionType.CUSTOMER_DEBT)
            }
            TypeCard(strings.quickActionCustomerPayment, selectedType == TransactionType.CUSTOMER_PAYMENT, Color(0xFF0061A4), Modifier.weight(1f)) {
                onSelected(TransactionType.CUSTOMER_PAYMENT)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeCard(strings.quickActionSupplierDebt, selectedType == TransactionType.SUPPLIER_DEBT, Color(0xFFB3261E), Modifier.weight(1f)) {
                onSelected(TransactionType.SUPPLIER_DEBT)
            }
            TypeCard(strings.quickActionSupplierPayment, selectedType == TransactionType.SUPPLIER_PAYMENT, Color(0xFF0061A4), Modifier.weight(1f)) {
                onSelected(TransactionType.SUPPLIER_PAYMENT)
            }
        }
    }
}

@Composable
private fun TypeCard(
    title: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        border = BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(title, modifier = Modifier.padding(12.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
