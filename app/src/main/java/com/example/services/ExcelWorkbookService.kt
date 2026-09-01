package com.example.services

import com.example.core.formatting.AmountFormatter
import com.example.core.formatting.DateFormatter
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.localization.V12StringsProvider
import com.example.data.local.entities.LedgerEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.models.PartyType
import com.example.data.models.TransactionType
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Lightweight Office Open XML writer/reader with no heavy Apache POI dependency. */
object ExcelWorkbookService {

    data class ImportRow(
        val name: String,
        val phone: String?,
        val partyType: PartyType,
        val amountText: String?,
        val currencyCode: String?,
        val direction: String?,
        val note: String?,
        val dueDateText: String?
    )

    data class ParsedImport(val rows: List<ImportRow>, val warnings: List<String>)

    fun writeTransactionsWorkbook(
        output: OutputStream,
        ledger: LedgerEntity,
        parties: List<PartyEntity>,
        transactions: List<LedgerTransactionEntity>,
        language: AppLanguage
    ) {
        val strings = AppStrings.get(language)
        val v12 = V12StringsProvider.get(language)
        val partyMap = parties.associateBy { it.id }
        val headers = listOf(
            "ID", strings.dateLabel, strings.partyNameLabel, strings.partyPhoneLabel,
            strings.partyTypeLabel, "${strings.addTransaction} / Type", strings.amountLabel,
            strings.currencyLabel, strings.noteLabel, "${v12.dateRange} / Due"
        )
        val rows = transactions.sortedBy { it.occurredAt }.map { tx ->
            val p = partyMap[tx.partyId]
            listOf(
                tx.id,
                formatIsoLocal(tx.occurredAt),
                p?.name.orEmpty(),
                p?.phone.orEmpty(),
                localizedPartyType(p?.partyType, language),
                localizedTransactionType(tx.transactionType, language),
                AmountFormatter.formatToInputString(tx.amountMinor, tx.currencyDecimalPlaces),
                tx.currencyCode,
                tx.note.orEmpty(),
                tx.dueAt?.let(::formatIsoDate).orEmpty()
            )
        }
        writeWorkbook(output, safeSheetName(ledger.name), headers, rows)
    }

    fun writeImportTemplate(output: OutputStream, language: AppLanguage) {
        val s = AppStrings.get(language)
        val headers = listOf(
            s.partyNameLabel,
            s.partyPhoneLabel,
            s.partyTypeLabel,
            s.openingBalanceAmount,
            s.currencyLabel,
            "Direction",
            s.notesLabel,
            "DueDate (YYYY-MM-DD)"
        )
        val customer = when (language) {
            AppLanguage.ARABIC -> listOf("أحمد", "00253...", s.typeCustomer, "15000", "DJF", "CUSTOMER_DEBT", "بضاعة", "2026-09-15")
            AppLanguage.FRENCH -> listOf("Ahmed", "+253...", s.typeCustomer, "15000", "DJF", "CUSTOMER_DEBT", "Marchandise", "2026-09-15")
            else -> listOf("Ahmed", "+253...", s.typeCustomer, "15000", "DJF", "CUSTOMER_DEBT", "Goods", "2026-09-15")
        }
        writeWorkbook(output, "DeynBook Import", headers, listOf(customer))
    }

    fun parseImportWorkbook(input: InputStream): ParsedImport {
        val entries = unzipEntries(input)
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetPath = resolveFirstWorksheetPath(entries) ?: "xl/worksheets/sheet1.xml"
        val sheetBytes = entries[sheetPath] ?: error("Excel worksheet not found")
        val table = parseSheet(sheetBytes, sharedStrings)
        if (table.isEmpty()) return ParsedImport(emptyList(), listOf("Workbook is empty"))

        val header = table.first().map(::normalizeHeader)
        fun indexOf(vararg aliases: String): Int = header.indexOfFirst { h -> aliases.any { normalizeHeader(it) == h } }

        val nameIx = indexOf("name", "party name", "الاسم", "اسم الحساب", "nom", "magaca", "ስም")
        if (nameIx < 0) error("Name column is required")
        val phoneIx = indexOf("phone", "phone number", "رقم الهاتف", "téléphone", "telephone", "telefoon", "ስልክ")
        val typeIx = indexOf("party type", "type", "نوع الحساب", "type de compte", "nooca xisaabta", "የመለያ ዓይነት")
        val amountIx = indexOf("opening amount", "amount", "مبلغ الرصيد الافتتاحي", "المبلغ", "montant", "lacag", "መጠን")
        val currencyIx = indexOf("currency", "العملة", "devise", "lacagta", "ምንዛሬ")
        val directionIx = indexOf("direction", "اتجاه", "نوع الدين", "sens")
        val noteIx = indexOf("notes", "note", "ملاحظات", "ملاحظة", "remarque", "qoraal", "ማስታወሻ")
        val dueIx = indexOf("duedate (yyyy-mm-dd)", "due date", "duedate", "تاريخ الاستحقاق", "échéance", "echeance")

        val warnings = mutableListOf<String>()
        val rows = mutableListOf<ImportRow>()
        table.drop(1).forEachIndexed { offset, cells ->
            fun cell(ix: Int): String? = if (ix >= 0) cells.getOrNull(ix)?.trim()?.takeIf(String::isNotBlank) else null
            val name = cell(nameIx) ?: return@forEachIndexed
            val typeText = cell(typeIx)
            val type = parsePartyType(typeText)
            if (type == null) {
                warnings += "Row ${offset + 2}: unknown account type '${typeText.orEmpty()}'"
                return@forEachIndexed
            }
            rows += ImportRow(
                name = name,
                phone = cell(phoneIx),
                partyType = type,
                amountText = cell(amountIx),
                currencyCode = cell(currencyIx)?.uppercase(Locale.US),
                direction = cell(directionIx),
                note = cell(noteIx),
                dueDateText = cell(dueIx)
            )
        }
        return ParsedImport(rows, warnings)
    }

    fun parseDueDate(text: String?): Long? {
        val value = text?.trim()?.takeIf(String::isNotBlank) ?: return null
        val patterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy")
        patterns.forEach { pattern ->
            try {
                val date = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value) ?: return@forEach
                return DateFormatter.endOfDay(date.time)
            } catch (_: Exception) {}
        }
        return null
    }

    fun inferTransactionType(direction: String?, partyType: PartyType, hasAmount: Boolean): TransactionType? {
        if (!hasAmount) return null
        val d = direction.orEmpty().trim().lowercase(Locale.ROOT)
        if (d.isNotBlank()) {
            if (d.contains("customer_debt") || d.contains("he_owes") || d.contains("عليه") || d.contains("عميل")) return TransactionType.CUSTOMER_DEBT
            if (d.contains("supplier_debt") || d.contains("i_owe") || d.contains("علينا") || d.contains("مورد")) return TransactionType.SUPPLIER_DEBT
        }
        return when (partyType) {
            PartyType.CUSTOMER -> TransactionType.CUSTOMER_DEBT
            PartyType.SUPPLIER -> TransactionType.SUPPLIER_DEBT
            PartyType.BOTH -> null
        }
    }

    private fun writeWorkbook(output: OutputStream, sheetName: String, headers: List<String>, rows: List<List<String>>) {
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", contentTypesXml())
            entry("_rels/.rels", rootRelsXml())
            entry("xl/workbook.xml", workbookXml(sheetName))
            entry("xl/_rels/workbook.xml.rels", workbookRelsXml())
            entry("xl/styles.xml", stylesXml())
            entry("xl/worksheets/sheet1.xml", sheetXml(headers, rows))
        }
    }

    private fun sheetXml(headers: List<String>, rows: List<List<String>>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        val allRows = listOf(headers) + rows
        allRows.forEachIndexed { rIndex, row ->
            val rowNumber = rIndex + 1
            append("<row r=\"").append(rowNumber).append("\">")
            row.forEachIndexed { cIndex, value ->
                val ref = columnName(cIndex) + rowNumber
                val style = if (rIndex == 0) " s=\"1\"" else ""
                append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(style).append("><is><t xml:space=\"preserve\">")
                append(xmlEscape(value))
                append("</t></is></c>")
            }
            append("</row>")
        }
        append("</sheetData><autoFilter ref=\"A1:").append(columnName((headers.size - 1).coerceAtLeast(0))).append("1\"/>")
        append("</worksheet>")
    }

    private fun unzipEntries(input: InputStream): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    zip.copyTo(buffer)
                    out[entry.name] = buffer.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return out
    }

    private fun resolveFirstWorksheetPath(entries: Map<String, ByteArray>): String? {
        // Our writer always uses sheet1. Most standard workbooks also expose this path.
        if (entries.containsKey("xl/worksheets/sheet1.xml")) return "xl/worksheets/sheet1.xml"
        return entries.keys.filter { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }.sorted().firstOrNull()
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val doc = safeDocument(bytes)
        val nodes = doc.getElementsByTagNameNS("*", "si")
        return (0 until nodes.length).map { i ->
            val element = nodes.item(i) as Element
            val texts = element.getElementsByTagNameNS("*", "t")
            buildString { for (j in 0 until texts.length) append(texts.item(j).textContent) }
        }
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val doc = safeDocument(bytes)
        val rows = doc.getElementsByTagNameNS("*", "row")
        val result = mutableListOf<List<String>>()
        for (i in 0 until rows.length) {
            val row = rows.item(i) as Element
            val cells = row.getElementsByTagNameNS("*", "c")
            val map = mutableMapOf<Int, String>()
            var max = -1
            for (j in 0 until cells.length) {
                val cell = cells.item(j) as Element
                val ref = cell.getAttribute("r")
                val column = columnIndex(ref.takeWhile(Char::isLetter))
                max = maxOf(max, column)
                val type = cell.getAttribute("t")
                val value = when (type) {
                    "inlineStr" -> cell.getElementsByTagNameNS("*", "t").item(0)?.textContent.orEmpty()
                    "s" -> {
                        val ix = cell.getElementsByTagNameNS("*", "v").item(0)?.textContent?.toIntOrNull()
                        ix?.let(sharedStrings::getOrNull).orEmpty()
                    }
                    else -> cell.getElementsByTagNameNS("*", "v").item(0)?.textContent.orEmpty()
                }
                map[column] = value
            }
            result += if (max < 0) emptyList() else (0..max).map { map[it].orEmpty() }
        }
        return result
    }

    private fun safeDocument(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        try { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (_: Exception) {}
        try { setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Exception) {}
        try { setFeature("http://xml.org/sax/features/external-parameter-entities", false) } catch (_: Exception) {}
        isXIncludeAware = false
        isExpandEntityReferences = false
    }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun parsePartyType(text: String?): PartyType? {
        val t = text.orEmpty().trim().lowercase(Locale.ROOT)
        if (t.isBlank()) return PartyType.CUSTOMER
        return when {
            t == "both" || t.contains("عميل ومورد") || t.contains("client et fournisseur") || t.contains("customer & supplier") -> PartyType.BOTH
            t.contains("supplier") || t.contains("مورد") || t.contains("fournisseur") || t.contains("alaab") || t.contains("አቅራቢ") -> PartyType.SUPPLIER
            t.contains("customer") || t.contains("عميل") || t.contains("client") || t.contains("macaamiil") || t.contains("ደንበኛ") -> PartyType.CUSTOMER
            else -> null
        }
    }

    private fun localizedPartyType(value: String?, language: AppLanguage): String {
        val s = AppStrings.get(language)
        return when (value?.let(PartyType::from)) {
            PartyType.CUSTOMER -> s.typeCustomer
            PartyType.SUPPLIER -> s.typeSupplier
            PartyType.BOTH -> s.typeBoth
            null -> ""
        }
    }

    private fun localizedTransactionType(value: String, language: AppLanguage): String {
        val s = AppStrings.get(language)
        return when (TransactionType.from(value)) {
            TransactionType.CUSTOMER_DEBT -> s.txCustomerDebtTitle
            TransactionType.CUSTOMER_PAYMENT -> s.txCustomerPaymentTitle
            TransactionType.SUPPLIER_DEBT -> s.txSupplierDebtTitle
            TransactionType.SUPPLIER_PAYMENT -> s.txSupplierPaymentTitle
            TransactionType.OPENING_RECEIVABLE -> s.txOpeningReceivable
            TransactionType.OPENING_PAYABLE -> s.txOpeningPayable
        }
    }

    private fun formatIsoLocal(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestamp))
    private fun formatIsoDate(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
    private fun safeSheetName(name: String): String = name.replace(Regex("[\\\\/*?:\\[\\]]"), " ").take(31).ifBlank { "DeynBook" }
    private fun normalizeHeader(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    private fun columnName(index: Int): String { var n = index + 1; val s = StringBuilder(); while (n > 0) { val r = (n - 1) % 26; s.append(('A'.code + r).toChar()); n = (n - 1) / 26 }; return s.reverse().toString() }
    private fun columnIndex(name: String): Int { var n = 0; name.uppercase(Locale.US).forEach { n = n * 26 + (it.code - 'A'.code + 1) }; return (n - 1).coerceAtLeast(0) }
    private fun xmlEscape(v: String): String = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
    private fun rootRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    private fun workbookXml(name: String) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${xmlEscape(name)}" sheetId="1" r:id="rId1"/></sheets></workbook>"""
    private fun workbookRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs></styleSheet>"""
}
