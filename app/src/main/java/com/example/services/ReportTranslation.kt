package com.example.services

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.Html
import com.example.BuildConfig
import com.example.core.localization.AppLanguage
import com.example.data.local.entities.LedgerEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.StatementData
import com.example.data.models.StatementRow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class ReportContentMode {
    ORIGINAL,
    TRANSLATED,
    BILINGUAL
}

data class ReportExportOptions(
    val targetLanguage: AppLanguage,
    val contentMode: ReportContentMode = ReportContentMode.ORIGINAL
) {
    val needsOnlineTranslation: Boolean
        get() = contentMode != ReportContentMode.ORIGINAL
}

enum class ReportExportResult {
    SUCCESS,
    NO_INTERNET,
    TRANSLATION_NOT_CONFIGURED,
    TRANSLATION_FAILED,
    EXPORT_FAILED
}

class ReportTranslationException(val result: ReportExportResult) : Exception(result.name)

/**
 * Translates only user-entered report content. Built-in labels always come from AppStrings
 * in the selected report language and therefore remain available fully offline.
 */
class ReportTranslationService(private val context: Context) {
    private val appContext = context.applicationContext
    private val cache = appContext.getSharedPreferences("report_translation_cache", Context.MODE_PRIVATE)

    suspend fun translateValues(
        values: Collection<String>,
        options: ReportExportOptions
    ): Map<String, String> {
        val originals = values.map(String::trim).filter(String::isNotBlank).distinct()
        if (!options.needsOnlineTranslation || originals.isEmpty()) return originals.associateWith { it }
        if (BuildConfig.TRANSLATION_API_KEY.isBlank()) {
            throw ReportTranslationException(ReportExportResult.TRANSLATION_NOT_CONFIGURED)
        }
        if (!hasInternetConnection()) {
            throw ReportTranslationException(ReportExportResult.NO_INTERNET)
        }

        val target = options.targetLanguage.code
        val result = linkedMapOf<String, String>()
        val missing = mutableListOf<String>()
        originals.forEach { original ->
            val cached = cache.getString(cacheKey(target, original), null)
            if (cached != null) result[original] = render(original, cached, options.contentMode) else missing += original
        }

        try {
            missing.chunked(100).forEach { chunk ->
                val translated = translateChunk(chunk, target)
                chunk.zip(translated).forEach { (original, value) ->
                    result[original] = render(original, value, options.contentMode)
                    cache.edit().putString(cacheKey(target, original), value).apply()
                }
            }
        } catch (error: ReportTranslationException) {
            throw error
        } catch (_: Exception) {
            throw ReportTranslationException(ReportExportResult.TRANSLATION_FAILED)
        }
        return result
    }

    suspend fun translateStatement(
        ledger: LedgerEntity,
        data: StatementData,
        options: ReportExportOptions
    ): Pair<LedgerEntity, StatementData> {
        val values = buildList {
            add(ledger.name)
            ledger.address?.let(::add)
            ledger.footerNote?.let(::add)
            add(data.party.name)
            data.party.notes?.let(::add)
            data.rows.mapNotNullTo(this) { it.transaction.note }
        }
        val translated = translateValues(values, options)
        fun value(original: String?): String? = original?.let { translated[it.trim()] ?: it }
        val translatedLedger = ledger.copy(
            name = value(ledger.name) ?: ledger.name,
            address = value(ledger.address),
            footerNote = value(ledger.footerNote)
        )
        val translatedParty = data.party.copy(
            name = value(data.party.name) ?: data.party.name,
            notes = value(data.party.notes)
        )
        val translatedRows = data.rows.map { row: StatementRow ->
            row.copy(transaction = row.transaction.copy(note = value(row.transaction.note)))
        }
        return translatedLedger to data.copy(party = translatedParty, rows = translatedRows)
    }

    suspend fun translateExportData(
        ledger: LedgerEntity,
        parties: List<PartyEntity>,
        transactions: List<LedgerTransactionEntity>,
        options: ReportExportOptions
    ): Triple<LedgerEntity, List<PartyEntity>, List<LedgerTransactionEntity>> {
        val values = buildList {
            add(ledger.name)
            ledger.address?.let(::add)
            ledger.footerNote?.let(::add)
            parties.forEach { party ->
                add(party.name)
                party.notes?.let(::add)
            }
            transactions.mapNotNullTo(this) { it.note }
        }
        val translated = translateValues(values, options)
        fun value(original: String?): String? = original?.let { translated[it.trim()] ?: it }
        return Triple(
            ledger.copy(
                name = value(ledger.name) ?: ledger.name,
                address = value(ledger.address),
                footerNote = value(ledger.footerNote)
            ),
            parties.map { it.copy(name = value(it.name) ?: it.name, notes = value(it.notes)) },
            transactions.map { it.copy(note = value(it.note)) }
        )
    }

    private fun render(original: String, translated: String, mode: ReportContentMode): String = when (mode) {
        ReportContentMode.ORIGINAL -> original
        ReportContentMode.TRANSLATED -> translated
        ReportContentMode.BILINGUAL -> if (translated.equals(original, ignoreCase = true)) original else "$original • $translated"
    }

    private fun translateChunk(values: List<String>, target: String): List<String> {
        val url = URL("https://translation.googleapis.com/language/translate/v2?key=${BuildConfig.TRANSLATION_API_KEY}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Android-Package", appContext.packageName)
            signingCertificateSha1()?.let { setRequestProperty("X-Android-Cert", it) }
        }
        val body = JSONObject().apply {
            put("q", JSONArray(values))
            put("target", target)
            put("format", "text")
        }.toString()
        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) throw ReportTranslationException(ReportExportResult.TRANSLATION_FAILED)
        val translations = JSONObject(response).getJSONObject("data").getJSONArray("translations")
        if (translations.length() != values.size) throw ReportTranslationException(ReportExportResult.TRANSLATION_FAILED)
        return (0 until translations.length()).map { index ->
            decodeHtml(translations.getJSONObject(index).getString("translatedText"))
        }
    }

    private fun hasInternetConnection(): Boolean {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Suppress("DEPRECATION")
    private fun signingCertificateSha1(): String? = try {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            info.signingInfo?.apkContentsSigners
        } else {
            appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val bytes = signatures?.firstOrNull()?.toByteArray() ?: return null
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString(":") { "%02X".format(it) }
    } catch (_: Exception) {
        null
    }

    private fun cacheKey(target: String, text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$target\u0000$text".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun decodeHtml(value: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        Html.fromHtml(value).toString()
    }
}
