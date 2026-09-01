package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object AttachmentService {
    private const val MAX_BYTES = 15L * 1024L * 1024L

    fun copyToPrivateStorage(context: Context, source: Uri): String? {
        val resolver = context.contentResolver
        val mime = resolver.getType(source).orEmpty()
        val extension = when {
            mime == "application/pdf" -> "pdf"
            mime == "image/png" -> "png"
            mime == "image/webp" -> "webp"
            mime == "image/gif" -> "gif"
            mime.startsWith("image/") -> "jpg"
            else -> "bin"
        }
        val dir = File(context.filesDir, "transaction_attachments").apply { mkdirs() }
        val destination = File(dir, "${UUID.randomUUID()}.$extension")
        return try {
            resolver.openInputStream(source)?.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) throw IllegalArgumentException("Attachment too large")
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return null
            destination.absolutePath
        } catch (_: Exception) {
            destination.delete()
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
        try {
            File(context.filesDir, "transaction_attachments").deleteRecursively()
        } catch (_: Exception) {
        }
    }

    fun open(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val type = when (file.extension.lowercase()) {
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
