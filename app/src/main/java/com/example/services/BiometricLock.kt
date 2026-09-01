package com.example.services

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.core.localization.AppLanguage
import com.example.core.localization.FeatureStrings
import com.example.core.localization.FeatureStringsProvider

object BiometricLock {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        language: AppLanguage,
        onSuccess: () -> Unit,
        onFailure: (() -> Unit)? = null
    ) {
        val text = FeatureStringsProvider.get(language)
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFailure?.invoke()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(text.unlockTitle)
            .setSubtitle(text.unlockSubtitle)
            .setNegativeButtonText(cancelLabel(language))
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun cancelLabel(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> "إلغاء"
        AppLanguage.SOMALI -> "Jooji"
        AppLanguage.AMHARIC -> "ሰርዝ"
        AppLanguage.FRENCH -> "Annuler"
        AppLanguage.ENGLISH -> "Cancel"
    }
}
