package com.example.ui.parties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.formatting.AmountFormatter
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType

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
        currencyDecimalPlaces: Int
    ) -> Unit
) {
    val strings = LocalStrings.current

    var name by remember { mutableStateOf(initialParty?.name ?: "") }
    var phone by remember { mutableStateOf(initialParty?.phone ?: "") }
    var partyType by remember {
        mutableStateOf(
            if (initialParty != null) PartyType.from(initialParty.partyType) else PartyType.CUSTOMER
        )
    }
    var notes by remember { mutableStateOf(initialParty?.notes ?: "") }

    // Opening Balance state (only for new party)
    var openingType by remember { mutableStateOf("NONE") } // "NONE", "HE_OWES_ME", "I_OWE_HIM"
    var openingAmountInput by remember { mutableStateOf("") }
    var selectedCurrencyCode by remember { mutableStateOf(defaultCurrencyCode) }

    var nameError by remember { mutableStateOf(false) }
    var similarParties by remember { mutableStateOf<List<PartyEntity>>(emptyList()) }

    val activeCurrency = currencies.find { it.code == selectedCurrencyCode }
        ?: CurrencyEntity(code = selectedCurrencyCode, name = selectedCurrencyCode, decimalPlaces = 0)

    // Check for similar names on input change
    LaunchedEffect(name) {
        if (initialParty == null && name.trim().length >= 3) {
            val results = onSearchSimilar(name)
            similarParties = results.filter { it.name.trim().lowercase() != name.trim().lowercase() }
        } else {
            similarParties = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialParty == null) strings.addParty else strings.editParty,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text(strings.partyNameLabel + " *") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(strings.errorNameRequired, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                // Similar Name Warning (Non-blocking)
                if (similarParties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.similarNameWarning,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = similarParties.take(3).joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.partyPhoneLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Party Type Selection
                Text(
                    text = strings.partyTypeLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                // Opening Balance Section (Only on create)
                if (initialParty == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = strings.openingBalanceTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Radio Options
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openingType = "NONE" }
                            ) {
                                RadioButton(
                                    selected = openingType == "NONE",
                                    onClick = { openingType = "NONE" }
                                )
                                Text(strings.openingBalanceNone, style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openingType = "HE_OWES_ME" }
                            ) {
                                RadioButton(
                                    selected = openingType == "HE_OWES_ME",
                                    onClick = { openingType = "HE_OWES_ME" }
                                )
                                Text(strings.openingBalanceHeOwesMe, style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openingType = "I_OWE_HIM" }
                            ) {
                                RadioButton(
                                    selected = openingType == "I_OWE_HIM",
                                    onClick = { openingType = "I_OWE_HIM" }
                                )
                                Text(strings.openingBalanceIOweHim, style = MaterialTheme.typography.bodyMedium)
                            }

                            if (openingType != "NONE") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = openingAmountInput,
                                        onValueChange = { openingAmountInput = it },
                                        label = { Text(strings.amountLabel) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )

                                    // Currency selector chips
                                    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Text(
                                            text = selectedCurrencyCode,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
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
                onClick = {
                    if (name.trim().isBlank()) {
                        nameError = true
                    } else {
                        val minor = if (openingType != "NONE") {
                            AmountFormatter.parseToMinorUnits(openingAmountInput, activeCurrency.decimalPlaces) ?: 0L
                        } else 0L

                        onSave(
                            name.trim(),
                            phone.trim().takeIf { it.isNotBlank() },
                            partyType,
                            notes.trim().takeIf { it.isNotBlank() },
                            openingType,
                            minor,
                            selectedCurrencyCode,
                            activeCurrency.decimalPlaces
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_party_button")
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
