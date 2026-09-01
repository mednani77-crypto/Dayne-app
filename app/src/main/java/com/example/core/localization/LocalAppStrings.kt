package com.example.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalStrings = staticCompositionLocalOf<Strings> { AppStrings.Arabic }
val LocalLanguage = staticCompositionLocalOf<AppLanguage> { AppLanguage.ARABIC }

@Composable
fun DeynBookLocalizationProvider(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val strings = AppStrings.get(language)
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}
