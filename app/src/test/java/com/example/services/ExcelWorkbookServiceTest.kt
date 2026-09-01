package com.example.services

import com.example.core.localization.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ExcelWorkbookServiceTest {
    @Test
    fun importTemplateIsRealXlsxAndCanBeParsedBack() {
        val output = ByteArrayOutputStream()
        ExcelWorkbookService.writeImportTemplate(output, AppLanguage.ARABIC)
        val bytes = output.toByteArray()

        assertTrue(bytes.size > 500)
        assertEquals('P'.code.toByte(), bytes[0])
        assertEquals('K'.code.toByte(), bytes[1])

        val entries = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries += entry.name
                zip.closeEntry()
            }
        }
        assertTrue("xl/workbook.xml" in entries)
        assertTrue("xl/worksheets/sheet1.xml" in entries)

        val parsed = ExcelWorkbookService.parseImportWorkbook(ByteArrayInputStream(bytes))
        assertEquals(1, parsed.rows.size)
        assertEquals("أحمد", parsed.rows.single().name)
        assertEquals("DJF", parsed.rows.single().currencyCode)
        assertEquals("15000", parsed.rows.single().amountText)
    }

    @Test
    fun infersCustomerAndSupplierDebtSafely() {
        assertEquals(
            com.example.data.models.TransactionType.CUSTOMER_DEBT,
            ExcelWorkbookService.inferTransactionType(null, com.example.data.models.PartyType.CUSTOMER, true)
        )
        assertEquals(
            com.example.data.models.TransactionType.SUPPLIER_DEBT,
            ExcelWorkbookService.inferTransactionType(null, com.example.data.models.PartyType.SUPPLIER, true)
        )
        assertEquals(
            null,
            ExcelWorkbookService.inferTransactionType(null, com.example.data.models.PartyType.BOTH, true)
        )
    }
}
