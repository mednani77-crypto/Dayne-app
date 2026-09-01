package com.example.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLanguage
import com.example.core.localization.V12StringsProvider
import com.example.data.local.entities.TransactionAttachmentEntity

@Composable
fun AttachmentManagerDialog(
    attachments: List<TransactionAttachmentEntity>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAdd: (Uri) -> Unit,
    onOpen: (TransactionAttachmentEntity) -> Unit,
    onDelete: (TransactionAttachmentEntity) -> Unit
) {
    val t = V12StringsProvider.get(language)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onAdd) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.attachments, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (attachments.isEmpty()) {
                    Text(t.multipleAttachments, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                attachments.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(item.displayName, maxLines = 1)
                            if (item.sizeBytes > 0) Text(formatBytes(item.sizeBytes), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onOpen(item) }) { Icon(Icons.Default.OpenInNew, contentDescription = t.open) }
                        IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = t.delete, tint = MaterialTheme.colorScheme.error) }
                    }
                }
                Button(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(t.addAttachment)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(t.cancel) } }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes.toDouble() / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes.toDouble() / 1024.0)
    else -> "$bytes B"
}
