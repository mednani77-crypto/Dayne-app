package com.example.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.BuildConfig
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.FeatureStringsProvider
import com.example.core.localization.LocalStrings
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.SettingsEntity
import com.example.services.BiometricLock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    currencies: List<CurrencyEntity>,
    currentLanguage: AppLanguage,
    onUpdateSettings: (SettingsEntity) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onToggleCurrency: (String, Boolean) -> Unit,
    onAddCustomCurrency: (code: String, name: String, decimals: Int) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackupFile: (Uri) -> Unit,
    onExportCsv: () -> Unit,
    onResetAllData: () -> Unit
) {
    val strings = LocalStrings.current
    val featureStrings = FeatureStringsProvider.get(currentLanguage)
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = activity?.let(BiometricLock::isAvailable) == true

    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showDefaultCurrencyDialog by remember { mutableStateOf(false) }
    var showAddCurrencyDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val backupPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(onImportBackupFile)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold) }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsCard(strings.businessProfileTitle, Icons.Default.Store) {
                    SettingsRow(
                        title = settings.businessName.ifBlank { strings.businessNamePlaceholder },
                        subtitle = settings.businessPhone ?: strings.businessAddressLabel,
                        onClick = { showProfileDialog = true }
                    )
                }
            }

            item {
                SettingsCard(strings.languageSettingTitle, Icons.Default.Language) {
                    SettingsRow(
                        title = strings.languageSettingTitle,
                        subtitle = currentLanguage.nativeName,
                        onClick = { showLanguageDialog = true }
                    )
                    SettingsRow(
                        title = strings.themeTitle,
                        subtitle = when (settings.themeMode) {
                            "DARK" -> strings.themeDark
                            "LIGHT" -> strings.themeLight
                            else -> strings.themeSystem
                        },
                        onClick = { showThemeDialog = true }
                    )
                    SettingsRow(
                        title = featureStrings.calendarTitle,
                        subtitle = if (settings.calendarMode == DateFormatter.CALENDAR_ETHIOPIAN) featureStrings.ethiopianCalendar else featureStrings.gregorianCalendar,
                        onClick = { showCalendarDialog = true }
                    )
                }
            }

            item {
                SettingsCard(featureStrings.biometricLock, Icons.Default.Fingerprint) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(featureStrings.biometricLock, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (biometricAvailable) featureStrings.biometricLockDesc else featureStrings.biometricUnavailable,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.biometricLockEnabled && biometricAvailable,
                            enabled = biometricAvailable,
                            onCheckedChange = { enabled ->
                                onUpdateSettings(settings.copy(biometricLockEnabled = enabled))
                            }
                        )
                    }
                }
            }

            item {
                SettingsCard(strings.currencySettingTitle, Icons.Default.CurrencyExchange) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsRow(
                            title = strings.defaultCurrencyLabel,
                            subtitle = settings.defaultCurrencyCode,
                            onClick = { showDefaultCurrencyDialog = true },
                            compact = true
                        )

                        Text(strings.enabledCurrenciesLabel, style = MaterialTheme.typography.labelMedium)
                        currencies.forEach { cur ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cur.code, fontWeight = FontWeight.Bold)
                                    Text(cur.name, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = cur.isEnabled,
                                    enabled = cur.code != settings.defaultCurrencyCode,
                                    onCheckedChange = { onToggleCurrency(cur.code, it) }
                                )
                            }
                        }

                        Button(onClick = { showAddCurrencyDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text(strings.addCustomCurrency)
                        }
                    }
                }
            }

            item {
                SettingsCard(strings.backupAndRestoreTitle, Icons.Default.Security) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.backupShareDesc, style = MaterialTheme.typography.bodySmall)
                        settings.lastBackupAt?.let {
                            Text("${strings.lastBackupDate} ${DateFormatter.formatDateTime(it)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onExportBackup, modifier = Modifier.weight(1f).testTag("export_backup_button")) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Text(strings.createBackup)
                            }
                            Button(
                                onClick = { backupPickerLauncher.launch("application/json") },
                                modifier = Modifier.weight(1f).testTag("import_backup_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Text(strings.restoreBackup)
                            }
                        }
                        Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth().testTag("export_all_csv_button")) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Text(strings.exportCsv)
                        }
                    }
                }
            }

            item {
                SettingsCard(strings.privacyPolicyTitle, Icons.Default.Security) {
                    SettingsRow(
                        title = strings.privacyPolicyTitle,
                        subtitle = strings.aboutAppTitle,
                        onClick = { showPrivacyDialog = true }
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(strings.appName, fontWeight = FontWeight.SemiBold)
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(strings.dangerZoneTitle, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(strings.deleteAllData) }
                    }
                }
            }

            item { Spacer(Modifier.height(50.dp)) }
        }
    }

    if (showProfileDialog) {
        var name by remember(settings.businessName) { mutableStateOf(settings.businessName) }
        var phone by remember(settings.businessPhone) { mutableStateOf(settings.businessPhone ?: "") }
        var address by remember(settings.businessAddress) { mutableStateOf(settings.businessAddress ?: "") }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(strings.businessProfileTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.businessNameLabel) })
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.businessPhoneLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.businessAddressLabel) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.trim().isNotBlank()) {
                        onUpdateSettings(
                            settings.copy(
                                businessName = name.trim(),
                                businessPhone = phone.trim().takeIf { it.isNotEmpty() },
                                businessAddress = address.trim().takeIf { it.isNotEmpty() }
                            )
                        )
                        showProfileDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.languageSettingTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onLanguageChange(lang)
                                showLanguageDialog = false
                            }.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang.nativeName, fontWeight = FontWeight.SemiBold)
                            if (lang == currentLanguage) Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(strings.themeTitle) },
            text = {
                Column {
                    listOf("SYSTEM" to strings.themeSystem, "LIGHT" to strings.themeLight, "DARK" to strings.themeDark).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = {
                                onUpdateSettings(settings.copy(themeMode = mode))
                                showThemeDialog = false
                            },
                            label = { Text(label) }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text(featureStrings.calendarTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.calendarMode != DateFormatter.CALENDAR_ETHIOPIAN,
                        onClick = {
                            onUpdateSettings(settings.copy(calendarMode = DateFormatter.CALENDAR_GREGORIAN))
                            showCalendarDialog = false
                        },
                        label = { Text(featureStrings.gregorianCalendar) }
                    )
                    FilterChip(
                        selected = settings.calendarMode == DateFormatter.CALENDAR_ETHIOPIAN,
                        onClick = {
                            onUpdateSettings(settings.copy(calendarMode = DateFormatter.CALENDAR_ETHIOPIAN))
                            showCalendarDialog = false
                        },
                        label = { Text(featureStrings.ethiopianCalendar) }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showCalendarDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showDefaultCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultCurrencyDialog = false },
            title = { Text(strings.defaultCurrencyLabel) },
            text = {
                Column {
                    currencies.filter { it.isEnabled }.forEach { cur ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onUpdateSettings(settings.copy(defaultCurrencyCode = cur.code))
                                showDefaultCurrencyDialog = false
                            }.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${cur.code} • ${cur.name}")
                            if (cur.code == settings.defaultCurrencyCode) Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDefaultCurrencyDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showAddCurrencyDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var decimals by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showAddCurrencyDialog = false },
            title = { Text(strings.addCustomCurrency) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit).take(5) },
                        label = { Text(strings.currencyCodeLabel) }
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.currencyNameLabel) })
                    OutlinedTextField(
                        value = decimals,
                        onValueChange = { decimals = it.filter(Char::isDigit).take(1) },
                        label = { Text(strings.decimalPlacesLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = decimals.toIntOrNull()
                    if (code.isNotBlank() && name.isNotBlank() && parsed != null && parsed in 0..2) {
                        onAddCustomCurrency(code, name, parsed)
                        showAddCurrencyDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showAddCurrencyDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyPolicyTitle, fontWeight = FontWeight.Bold) },
            text = { Text(privacyBody(currentLanguage)) },
            confirmButton = { Button(onClick = { showPrivacyDialog = false }) { Text(strings.back) } }
        )
    }

    if (showDeleteAllDialog) {
        var confirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(strings.deleteAllDataConfirmTitle, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.deleteAllDataConfirmMessage)
                    Text(strings.typeDeleteToConfirm)
                    OutlinedTextField(value = confirmation, onValueChange = { confirmation = it })
                }
            },
            confirmButton = {
                Button(
                    enabled = confirmation.trim().equals(strings.deleteKeyword, ignoreCase = true),
                    onClick = {
                        onResetAllData()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.deleteAllData) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun privacyBody(language: AppLanguage): String = when (language) {
    AppLanguage.ARABIC -> "يعمل DeynBook محليًا على جهازك. لا نرسل أسماء العملاء أو أرقام الهواتف أو المبالغ أو العمليات إلى خادم، ولا نستخدم التحليلات أو الإعلانات. المرفقات تحفظ داخل مساحة التطبيق الخاصة. النسخ الاحتياطية وملفات PDF لا تغادر جهازك إلا عندما تختار أنت مشاركتها. التطبيق أداة لتسجيل الديون وليس بنكًا ولا يقدم قروضًا أو تحويلات مالية."
    AppLanguage.SOMALI -> "DeynBook wuxuu xogta ku hayaa qalabkaaga. Magacyada, lambarrada telefoonka, lacagaha iyo dhaqdhaqaaqyada looma diro server. Lifaaqyada waxaa lagu kaydiyaa gudaha app-ka. Backup iyo PDF waxay ka baxaan qalabka oo keliya marka adigu wadaagto. App-ku waa buug deyn, ma aha bangi mana bixiyo amaah ama xawaalad."
    AppLanguage.AMHARIC -> "DeynBook መረጃዎን በመሣሪያዎ ላይ ብቻ ያስቀምጣል። ስሞች፣ ስልክ ቁጥሮች፣ መጠኖች እና ግብይቶች ወደ ሰርቨር አይላኩም። አባሪዎች በመተግበሪያው የግል ማከማቻ ውስጥ ይቀመጣሉ። Backup እና PDF እርስዎ ሲያጋሩ ብቻ ከመሣሪያው ይወጣሉ።"
    AppLanguage.FRENCH -> "DeynBook conserve les données localement sur votre appareil. Les noms, numéros de téléphone, montants et opérations ne sont envoyés à aucun serveur. Les pièces jointes restent dans l’espace privé de l’application. Les sauvegardes et PDF ne quittent l’appareil que lorsque vous choisissez de les partager."
    AppLanguage.ENGLISH -> "DeynBook stores data locally on your device. Names, phone numbers, amounts and transactions are not sent to a server. Attachments are kept in the app's private storage. Backups and PDFs leave the device only when you choose to share them. DeynBook is a debt-recording tool, not a bank."
}
