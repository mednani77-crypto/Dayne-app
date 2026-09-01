package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object AttachmentService {
    private const val MAX_BYTES = 15L * 1024L * 1024L
    private const val MAX_LOGO_BYTES = 5L * 1024L * 1024L

    data class StoredAttachment(
        val path: String,
        val displayName: String,
        val mimeType: String,
        val sizeBytes: Long
    )

    /** Backward-compatible helper used by the v1.1 single-attachment flow. */
    fun copyToPrivateStorage(context: Context, source: Uri): String? =
        copyWithMetadata(context, source)?.path

    fun copyWithMetadata(context: Context, source: Uri): StoredAttachment? =
        copyInternal(context, source, "transaction_attachments", MAX_BYTES, allowPdf = true)

    fun copyLogo(context: Context, source: Uri): String? =
        copyInternal(context, source, "ledger_logos", MAX_LOGO_BYTES, allowPdf = false)?.path

    private fun copyInternal(
        context: Context,
        source: Uri,
        directoryName: String,
        maxBytes: Long,
        allowPdf: Boolean
    ): StoredAttachment? {
        val resolver = context.contentResolver
        val mime = resolver.getType(source).orEmpty().ifBlank { "application/octet-stream" }
        if (!allowPdf && !mime.startsWith("image/")) return null
        if (allowPdf && !(mime.startsWith("image/") || mime == "application/pdf" || mime == "application/octet-stream")) return null

        val extension = when {
            mime == "application/pdf" -> "pdf"
            mime == "image/png" -> "png"
            mime == "image/webp" -> "webp"
            mime == "image/gif" -> "gif"
            mime == "image/jpeg" -> "jpg"
            mime.startsWith("image/") -> "jpg"
            else -> "bin"
        }
        val displayName = queryDisplayName(context, source) ?: "attachment.$extension"
        val dir = File(context.filesDir, directoryName).apply { mkdirs() }
        val destination = File(dir, "${UUID.randomUUID()}.$extension")
        return try {
            var total = 0L
            resolver.openInputStream(source)?.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) throw IllegalArgumentException("Attachment too large")
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return null
            StoredAttachment(destination.absolutePath, displayName, mime, total)
        } catch (_: Exception) {
            destination.delete()
            null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deletePath(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (_: Exception) {
        }
    }

    fun clearAll(context: Context) {
        try { File(context.filesDir, "transaction_attachments").deleteRecursively() } catch (_: Exception) {}
        try { File(context.filesDir, "ledger_logos").deleteRecursively() } catch (_: Exception) {}
    }

    fun open(context: Context, path: String?, explicitMime: String? = null) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val type = explicitMime ?: when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val share = Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(share, "DeynBook").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
