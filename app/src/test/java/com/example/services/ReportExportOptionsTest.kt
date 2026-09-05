package com.example.services

import com.example.core.localization.AppLanguage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExportOptionsTest {
    @Test
    fun originalContentNeverRequiresInternetTranslation() {
        val options = ReportExportOptions(
            targetLanguage = AppLanguage.ARABIC,
            contentMode = ReportContentMode.ORIGINAL
        )

        assertFalse(options.needsOnlineTranslation)
    }

    @Test
    fun translatedAndBilingualContentRequireInternetTranslation() {
        assertTrue(
            ReportExportOptions(AppLanguage.FRENCH, ReportContentMode.TRANSLATED)
                .needsOnlineTranslation
        )
        assertTrue(
            ReportExportOptions(AppLanguage.AMHARIC, ReportContentMode.BILINGUAL)
                .needsOnlineTranslation
        )
    }
}
