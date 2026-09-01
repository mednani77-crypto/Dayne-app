package com.example.ui.settings

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLanguage
import com.example.core.localization.V12StringsProvider
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerManagerScreen(
    ledgers: List<LedgerEntity>,
    activeLedgerId: String,
    currencies: List<CurrencyEntity>,
    language: AppLanguage,
    onBack: () -> Unit,
    onSwitch: (String) -> Unit,
    onCreate: (name: String, country: String, currency: String, phone: String?, address: String?) -> Unit,
    onUpdate: (LedgerEntity) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onLogoSelected: (ledgerId: String, Uri?) -> Unit
) {
    val t = V12StringsProvider.get(language)
    var editing by remember { mutableStateOf<LedgerEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var logoTarget by remember { mutableStateOf<String?>(null) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = logoTarget
        if (target != null && uri != null) onLogoSelected(target, uri)
        logoTarget = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t.ledgers, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, contentDescription = t.newLedger) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ledgers, key = { it.id }) { ledger ->
                val active = ledger.id == activeLedgerId
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !ledger.isArchived) { onSwitch(ledger.id) },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LedgerLogo(ledger)
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(ledger.name, fontWeight = FontWeight.Bold)
                                Text("${ledger.defaultCurrencyCode} • ${ledger.countryCode}", style = MaterialTheme.typography.bodySmall)
                                if (active) Text(t.currentLedger, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                if (ledger.isArchived) Text(t.archive, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                            if (active) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { editing = ledger }) { Icon(Icons.Default.Edit, null); Text(t.editLedger) }
                            TextButton(onClick = { logoTarget = ledger.id; logoPicker.launch("image/*") }) { Icon(Icons.Default.Image, null); Text(t.chooseLogo) }
                            if (ledger.logoPath != null) TextButton(onClick = { onLogoSelected(ledger.id, null) }) { Text(t.removeLogo) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                enabled = !active,
                                onClick = { onArchive(ledger.id, !ledger.isArchived) }
                            ) { Icon(if (ledger.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, null); Text(if (ledger.isArchived) t.unarchive else t.archive) }
                            TextButton(
                                enabled = !active,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                onClick = { onDelete(ledger.id) }
                            ) { Icon(Icons.Default.Delete, null); Text(t.delete) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreate) {
        LedgerEditorDialog(
            initial = null,
            currencies = currencies,
            t = t,
            onDismiss = { showCreate = false },
            onSave = { name, country, currency, phone, address, _ ->
                onCreate(name, country, currency, phone, address)
                showCreate = false
            }
        )
    }
    editing?.let { ledger ->
        LedgerEditorDialog(
            initial = ledger,
            currencies = currencies,
            t = t,
            onDismiss = { editing = null },
            onSave = { name, country, currency, phone, address, footer ->
                onUpdate(ledger.copy(name = name, countryCode = country, defaultCurrencyCode = currency, phone = phone, address = address, footerNote = footer))
                editing = null
            }
        )
    }
}

@Composable
private fun LedgerLogo(ledger: LedgerEntity) {
    val bitmap = remember(ledger.logoPath) {
        ledger.logoPath?.let { path -> try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null } }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
    } else {
        Card(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Image, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LedgerEditorDialog(
    initial: LedgerEntity?,
    currencies: List<CurrencyEntity>,
    t: com.example.core.localization.V12Strings,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String?, String?) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var country by remember(initial?.id) { mutableStateOf(initial?.countryCode ?: "DJ") }
    var currency by remember(initial?.id) { mutableStateOf(initial?.defaultCurrencyCode ?: currencies.firstOrNull { it.isEnabled }?.code ?: "DJF") }
    var phone by remember(initial?.id) { mutableStateOf(initial?.phone.orEmpty()) }
    var address by remember(initial?.id) { mutableStateOf(initial?.address.orEmpty()) }
    var footer by remember(initial?.id) { mutableStateOf(initial?.footerNote.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) t.newLedger else t.editLedger, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(t.ledgerName) }, singleLine = true)
                OutlinedTextField(country, { country = it.uppercase().take(3) }, label = { Text("Country") }, singleLine = true)
                OutlinedTextField(currency, { currency = it.uppercase().take(5) }, label = { Text("Currency") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Address") })
                OutlinedTextField(footer, { footer = it }, label = { Text(t.footerNote) }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(enabled = name.trim().isNotBlank(), onClick = {
                onSave(name.trim(), country.ifBlank { "DJ" }, currency.ifBlank { "DJF" }, phone.trim().takeIf(String::isNotBlank), address.trim().takeIf(String::isNotBlank), footer.trim().takeIf(String::isNotBlank))
            }) { Text(t.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t.cancel) } }
    )
}
