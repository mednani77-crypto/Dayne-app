package com.example.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.localization.AppLanguage
import com.example.core.localization.FeatureStringsProvider
import com.example.services.BiometricLock

@Composable
fun BiometricGate(
    enabled: Boolean,
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = FeatureStringsProvider.get(language)
    var unlocked by remember(enabled) { mutableStateOf(!enabled) }
    var promptInFlight by remember { mutableStateOf(false) }

    fun requestUnlock() {
        if (!enabled || unlocked || promptInFlight) return
        if (activity == null || !BiometricLock.isAvailable(activity)) return
        promptInFlight = true
        BiometricLock.authenticate(
            activity = activity,
            language = language,
            onSuccess = {
                promptInFlight = false
                unlocked = true
            },
            onFailure = { promptInFlight = false }
        )
    }

    LaunchedEffect(enabled, activity) {
        if (enabled) requestUnlock() else unlocked = true
    }

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (enabled && event == Lifecycle.Event.ON_STOP) {
                unlocked = false
                promptInFlight = false
            }
            if (enabled && event == Lifecycle.Event.ON_RESUME && !unlocked) {
                requestUnlock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!enabled || unlocked || activity == null || !BiometricLock.isAvailable(activity)) {
        content()
    } else {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(strings.unlockTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 18.dp))
                Text(strings.unlockSubtitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
                Button(onClick = { requestUnlock() }) { Text(strings.unlock) }
            }
        }
    }
}
