package com.example.ui.parties

import android.app.DatePickerDialog
import java.util.Calendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPartyDialog(
    initialParty: PartyEntity? = null,
    defaultCurrencyCode: String,
    currencies: List<CurrencyEntity>,
    onDismiss: () -> Unit,
    onSearchSimilar: suspend (String) -> List<PartyEntity>,
    onSave: (
        name: String,
        phone: String?,
        partyType: PartyType,
        notes: String?,
        openingType: String,
        openingAmountMinor: Long,
        currencyCode: String,
        currencyDecimalPlaces: Int,
        openingOccurredAt: Long
    ) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialParty?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initialParty?.phone.orEmpty()) }
    var notes by remember { mutableStateOf(initialParty?.notes.orEmpty()) }
    var partyType by remember { mutableStateOf(initialParty?.let { PartyType.from(it.partyType) } ?: PartyType.CUSTOMER) }

    var openingType by remember { mutableStateOf("NONE") }
    var openingAmountInput by remember { mutableStateOf("") }
    var selectedCurrencyCode by remember { mutableStateOf(defaultCurrencyCode) }
    var openingOccurredAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var openingAmountError by remember { mutableStateOf(false) }
    var similarParties by remember { mutableStateOf<List<PartyEntity>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val activeCurrency = currencies.find { it.code == selectedCurrencyCode }
        ?: CurrencyEntity(code = selectedCurrencyCode, name = selectedCurrencyCode, decimalPlaces = 0)

    LaunchedEffect(name) {
        if (initialParty == null && name.trim().length >= 3) {
            similarParties = onSearchSimilar(name).filterNot {
                it.name.trim().equals(name.trim(), ignoreCase = true)
            }
        } else {
            similarParties = emptyList()
        }
    }

    LaunchedEffect(partyType) {
        val valid = when (openingType) {
            "HE_OWES_ME" -> partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH
            "I_OWE_HIM" -> partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH
            else -> true
        }
        if (!valid) {
            openingType = "NONE"
            openingAmountInput = ""
        }
    }

    fun pickOpeningDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = openingOccurredAt }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = openingOccurredAt
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                openingOccurredAt = updated.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialParty == null) strings.addParty else strings.editParty, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(strings.partyNameLabel) },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text(strings.errorNameRequired) }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("party_name_input")
                )

                if (similarParties.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null)
                                Text(strings.similarNameWarning, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                            }
                            Text(similarParties.take(3).joinToString(", ") { it.name })
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.partyPhoneLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("party_phone_input")
                )

                Text(strings.partyTypeLabel, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = partyType == PartyType.CUSTOMER,
                        onClick = { partyType = PartyType.CUSTOMER },
                        label = { Text(strings.typeCustomer) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = partyType == PartyType.SUPPLIER,
                        onClick = { partyType = PartyType.SUPPLIER },
                        label = { Text(strings.typeSupplier) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = partyType == PartyType.BOTH,
                        onClick = { partyType = PartyType.BOTH },
                        label = { Text(strings.typeBoth) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (initialParty == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.openingBalanceTitle, fontWeight = FontWeight.Bold)
                            FilterChip(
                                selected = openingType == "NONE",
                                onClick = { openingType = "NONE" },
                                label = { Text(strings.openingBalanceNone) }
                            )
                            if (partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH) {
                                FilterChip(
                                    selected = openingType == "HE_OWES_ME",
                                    onClick = { openingType = "HE_OWES_ME" },
                                    label = { Text(strings.openingBalanceHeOwesMe) }
                                )
                            }
                            if (partyType == PartyType.SUPPLIER || partyType == PartyType.BOTH) {
                                FilterChip(
                                    selected = openingType == "I_OWE_HIM",
                                    onClick = { openingType = "I_OWE_HIM" },
                                    label = { Text(strings.openingBalanceIOweHim) }
                                )
                            }

                            if (openingType != "NONE") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = openingAmountInput,
                                        onValueChange = {
                                            openingAmountInput = it
                                            openingAmountError = false
                                        },
                                        label = { Text(strings.amountLabel) },
                                        isError = openingAmountError,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.5f)
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
                                        ExposedDropdownMenu(
                                            expanded = currencyMenuExpanded,
                                            onDismissRequest = { currencyMenuExpanded = false }
                                        ) {
                                            currencies.filter { it.isEnabled }.forEach { currency ->
                                                DropdownMenuItem(
                                                    text = { Text(currency.code) },
                                                    onClick = {
                                                        selectedCurrencyCode = currency.code
                                                        openingAmountInput = ""
                                                        openingAmountError = false
                                                        currencyMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { pickOpeningDate() },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                                        Text(DateFormatter.formatDate(openingOccurredAt), modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                modifier = Modifier.testTag("save_party_button"),
                onClick = {
                    if (name.trim().isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val openingAmount = if (openingType == "NONE") {
                        0L
                    } else {
                        AmountFormatter.parseToMinorUnits(openingAmountInput, activeCurrency.decimalPlaces) ?: 0L
                    }
                    if (openingType != "NONE" && openingAmount <= 0L) {
                        openingAmountError = true
                        return@Button
                    }
                    isSaving = true
                    onSave(
                        name.trim(),
                        phone.trim().takeIf { it.isNotBlank() },
                        partyType,
                        notes.trim().takeIf { it.isNotBlank() },
                        openingType,
                        openingAmount,
                        selectedCurrencyCode,
                        activeCurrency.decimalPlaces,
                        openingOccurredAt
                    )
                }
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
