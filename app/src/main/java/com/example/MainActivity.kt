package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.ui.DeynBookAppV12

class MainActivity : FragmentActivity() {
    private var sharedUri by mutableStateOf<Uri?>(null)
    private var sharedMimeType by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureSharedFile(intent)
        setContent {
            DeynBookAppV12(
                sharedUri = sharedUri,
                sharedMimeType = sharedMimeType,
                onSharedConsumed = {
                    sharedUri = null
                    sharedMimeType = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureSharedFile(intent)
    }

    private fun captureSharedFile(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val mime = intent.type.orEmpty()
        if (!(mime.startsWith("image/") || mime == "application/pdf")) return
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
        sharedUri = uri
        sharedMimeType = mime
    }
}
