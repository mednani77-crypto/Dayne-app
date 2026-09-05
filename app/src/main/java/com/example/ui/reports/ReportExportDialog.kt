package com.example.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLanguage
import com.example.services.ReportContentMode
import com.example.services.ReportExportOptions
import com.example.services.ReportExportResult

@Composable
fun ReportExportDialog(
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (ReportExportOptions) -> Unit
) {
    val text = reportExportText(appLanguage)
    var targetLanguage by remember(appLanguage) { mutableStateOf(appLanguage) }
    var contentMode by remember { mutableStateOf(ReportContentMode.ORIGINAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text.reportLanguage, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppLanguage.entries.size) { index ->
                        val language = AppLanguage.entries[index]
                        AssistChip(
                            onClick = { targetLanguage = language },
                            label = {
                                Text(
                                    if (targetLanguage == language) "✓ ${language.nativeName}" else language.nativeName
                                )
                            }
                        )
                    }
                }
                HorizontalDivider()
                Text(text.userContent, fontWeight = FontWeight.SemiBold)
                ModeRow(text.originalOnly, contentMode == ReportContentMode.ORIGINAL) {
                    contentMode = ReportContentMode.ORIGINAL
                }
                ModeRow(text.translatedOnly, contentMode == ReportContentMode.TRANSLATED) {
                    contentMode = ReportContentMode.TRANSLATED
                }
                ModeRow(text.originalAndTranslation, contentMode == ReportContentMode.BILINGUAL) {
                    contentMode = ReportContentMode.BILINGUAL
                }
                Text(
                    if (contentMode == ReportContentMode.ORIGINAL) text.offlineNotice else text.onlineNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ReportExportOptions(targetLanguage, contentMode)) }) {
                Text(text.continueLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text.cancel) } }
    )
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

data class ReportExportText(
    val title: String,
    val reportLanguage: String,
    val userContent: String,
    val originalOnly: String,
    val translatedOnly: String,
    val originalAndTranslation: String,
    val offlineNotice: String,
    val onlineNotice: String,
    val continueLabel: String,
    val cancel: String,
    val success: String,
    val noInternet: String,
    val notConfigured: String,
    val translationFailed: String,
    val exportFailed: String
)

fun reportExportResultMessage(language: AppLanguage, result: ReportExportResult): String {
    val text = reportExportText(language)
    return when (result) {
        ReportExportResult.SUCCESS -> text.success
        ReportExportResult.NO_INTERNET -> text.noInternet
        ReportExportResult.TRANSLATION_NOT_CONFIGURED -> text.notConfigured
        ReportExportResult.TRANSLATION_FAILED -> text.translationFailed
        ReportExportResult.EXPORT_FAILED -> text.exportFailed
    }
}

private fun reportExportText(language: AppLanguage): ReportExportText = when (language) {
    AppLanguage.ARABIC -> ReportExportText(
        "لغة التقرير", "اختر لغة التقرير", "الأسماء والملاحظات والمحتوى المكتوب",
        "النص الأصلي", "الترجمة فقط", "الأصل والترجمة معًا",
        "يعمل هذا الخيار بالكامل دون إنترنت.",
        "تحتاج الترجمة إلى الإنترنت. تُرسل النصوص المكتوبة فقط إلى خدمة الترجمة، ولا تُرسل الأرقام أو قاعدة البيانات.",
        "متابعة", "إلغاء", "تم إنشاء التقرير", "لا يوجد اتصال بالإنترنت",
        "خدمة الترجمة غير مهيأة في هذه النسخة", "تعذرت ترجمة بعض المحتوى", "تعذر إنشاء التقرير"
    )
    AppLanguage.SOMALI -> ReportExportText(
        "Luqadda warbixinta", "Dooro luqadda warbixinta", "Magacyada, qoraallada iyo xogta la qoray",
        "Qoraalka asalka ah", "Tarjumaadda keliya", "Asalka iyo tarjumaadda",
        "Doorashadani internet la'aan ayay shaqaysaa.",
        "Tarjumaaddu internet ayay u baahan tahay. Qoraalka keliya ayaa loo diraa adeegga tarjumaadda; tirooyinka iyo kaydka xogta lama diro.",
        "Sii wad", "Jooji", "Warbixinta waa la sameeyay", "Internet ma jiro",
        "Adeegga tarjumaadda laguma diyaarin nuqulkan", "Tarjumaaddu way fashilantay", "Warbixinta lama samayn karin"
    )
    AppLanguage.AMHARIC -> ReportExportText(
        "የሪፖርት ቋንቋ", "የሪፖርቱን ቋንቋ ይምረጡ", "ስሞች፣ ማስታወሻዎች እና የተጻፈ ይዘት",
        "የመጀመሪያው ጽሑፍ", "ትርጉም ብቻ", "ዋናው እና ትርጉሙ",
        "ይህ አማራጭ ያለ በይነመረብ ይሰራል።",
        "ትርጉም በይነመረብ ይፈልጋል። የተጻፈ ጽሑፍ ብቻ ይላካል፤ ቁጥሮችና የመረጃ ቋቱ አይላኩም።",
        "ቀጥል", "ሰርዝ", "ሪፖርቱ ተፈጥሯል", "የበይነመረብ ግንኙነት የለም",
        "የትርጉም አገልግሎቱ በዚህ ስሪት አልተዘጋጀም", "ትርጉሙ አልተሳካም", "ሪፖርቱን መፍጠር አልተቻለም"
    )
    AppLanguage.FRENCH -> ReportExportText(
        "Langue du rapport", "Choisir la langue du rapport", "Noms, notes et contenu saisi",
        "Texte original", "Traduction uniquement", "Original et traduction",
        "Cette option fonctionne entièrement hors ligne.",
        "La traduction nécessite Internet. Seul le texte saisi est envoyé au service de traduction; les montants et la base de données ne le sont pas.",
        "Continuer", "Annuler", "Rapport créé", "Pas de connexion Internet",
        "Le service de traduction n'est pas configuré dans cette version", "Échec de la traduction", "Impossible de créer le rapport"
    )
    AppLanguage.ENGLISH -> ReportExportText(
        "Report language", "Choose the report language", "Names, notes and entered content",
        "Original text", "Translation only", "Original and translation",
        "This option works entirely offline.",
        "Translation requires internet. Only entered text is sent to the translation service; amounts and the database are not sent.",
        "Continue", "Cancel", "Report created", "No internet connection",
        "Translation is not configured in this build", "Translation failed", "Could not create the report"
    )
}
