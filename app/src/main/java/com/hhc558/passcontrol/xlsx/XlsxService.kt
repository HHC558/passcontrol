package com.hhc558.passcontrol.xlsx

import com.hhc558.passcontrol.data.AccountView
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxException(message: String) : Exception(message)

/**
 * 轻量 .xlsx 读写：无第三方依赖。
 * 写出标准 xlsx（zip + XML，内联字符串），读取兼容 Excel/WPS 常见格式
 * （sharedStrings / inlineStr / 数字 / 日期序列号），表头支持乱序。
 */
class XlsxService {

    fun writeRecords(records: List<AccountView>): ByteArray {
        val headers = listOf("平台名称", "账号", "密码", "邮箱", "创建时间")
        val rows = records.map {
            listOf(it.platform, it.username, it.password, it.email ?: "", formatCreatedAt(it.createdAt))
        }
        return buildXlsx(headers, rows)
    }

    fun readRows(bytes: ByteArray): List<ImportRow> {
        val entries = readZip(bytes)
        val workbookXml = findEntry(entries, "xl/workbook.xml", "/workbook.xml", "workbook.xml")
            ?: throw XlsxException("无法识别的 xlsx 文件（缺少 workbook）")
        val relsXml = findEntry(entries, "xl/_rels/workbook.xml.rels", "/workbook.xml.rels", "workbook.xml.rels")
        val sharedStrings = findEntry(entries, "xl/sharedStrings.xml", "/sharedStrings.xml", "sharedStrings.xml")
            ?.let { parseSharedStrings(it) } ?: emptyList()

        val target = firstSheetTarget(workbookXml, relsXml)
        val sheetXml = findEntry(entries, "xl/$target", "/$target", target)
            ?: throw XlsxException("无法识别的 xlsx 文件（缺少工作表）")

        val rawRows = parseSheet(sheetXml, sharedStrings)
        val header = detectHeader(rawRows)
        val result = ArrayList<ImportRow>()
        for (i in header.index + 1 until rawRows.size) {
            val row = rawRows[i]
            if (row.cells.isEmpty()) continue
            result.add(toImportRow(row, header.mapping) ?: continue)
        }
        return result
    }

    // ------------------------------------------------------------------ 写出

    private fun buildXlsx(headers: List<String>, rows: List<List<String>>): ByteArray {
        val entries = linkedMapOf<String, String>()
        entries["[Content_Types].xml"] = CONTENT_TYPES
        entries["_rels/.rels"] = ROOT_RELS
        entries["xl/workbook.xml"] = workbookXml()
        entries["xl/_rels/workbook.xml.rels"] = WORKBOOK_RELS
        entries["xl/styles.xml"] = STYLES
        entries["xl/worksheets/sheet1.xml"] = buildSheetXml(headers, rows)
        return writeZip(entries)
    }

    private fun workbookXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="账号密码备份" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun buildSheetXml(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>")
        val widths = listOf(24, 24, 26, 26, 20)
        for (i in widths.indices) {
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                .append("\" width=\"").append(widths[i]).append("\" customWidth=\"1\"/>")
        }
        sb.append("</cols><sheetData>")
        val allRows = listOf(headers) + rows
        for ((idx, row) in allRows.withIndex()) {
            val r = idx + 1
            sb.append("<row r=\"").append(r).append("\">")
            for ((ci, value) in row.withIndex()) {
                val ref = ('A'.code + ci).toChar().toString() + r
                sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                sb.append(xmlEscape(value))
                sb.append("</t></is></c>")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun writeZip(entries: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    private fun xmlEscape(s: String): String = buildString {
        for (ch in s) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch == '\'' -> append("&apos;")
                ch == '\t' || ch == '\n' || ch == '\r' || ch >= ' ' -> append(ch)
                // 丢弃 XML 1.0 非法控制字符
            }
        }
    }

    // ------------------------------------------------------------------ 读取

    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val map = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    map[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        return map
    }

    private fun findEntry(entries: Map<String, ByteArray>, vararg suffixes: String): ByteArray? {
        for ((name, data) in entries) {
            for (s in suffixes) {
                if (name.endsWith(s)) return data
            }
        }
        return null
    }

    private fun parseXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isExpandEntityReferences = false
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(bytes))
    }

    private fun firstSheetTarget(workbookXml: ByteArray, relsXml: ByteArray?): String {
        val doc = parseXml(workbookXml)
        val sheets = doc.getElementsByTagName("sheet")
        if (sheets.length == 0) return "worksheets/sheet1.xml"
        val sheet = sheets.item(0) as Element
        val rid = sheet.getAttribute("r:id").ifBlank {
            sheet.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
        }
        if (rid.isNotBlank() && relsXml != null) {
            val relDoc = parseXml(relsXml)
            val rels = relDoc.getElementsByTagName("Relationship")
            for (i in 0 until rels.length) {
                val rel = rels.item(i) as Element
                if (rel.getAttribute("Id") == rid) {
                    var target = rel.getAttribute("Target")
                    if (target.startsWith("/")) target = target.substring(1)
                    if (target.startsWith("xl/")) target = target.substring(3)
                    if (target.isNotBlank()) return target
                }
            }
        }
        return "worksheets/sheet1.xml"
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val doc = parseXml(xml)
        val siNodes = doc.getElementsByTagName("si")
        val result = ArrayList<String>(siNodes.length)
        for (i in 0 until siNodes.length) {
            val sb = StringBuilder()
            collectText(siNodes.item(i) as Element, sb)
            result.add(sb.toString())
        }
        return result
    }

    private fun collectText(el: Element, sb: StringBuilder) {
        val kids = el.childNodes
        for (i in 0 until kids.length) {
            val n = kids.item(i)
            if (n is Element) {
                when (n.nodeName) {
                    "t" -> sb.append(n.textContent)
                    "r", "is" -> collectText(n, sb)
                }
            }
        }
    }

    private data class RawRow(val number: Int, val cells: Map<String, String>)

    private fun parseSheet(sheetXml: ByteArray, sharedStrings: List<String>): List<RawRow> {
        val doc = parseXml(sheetXml)
        val rowNodes = doc.getElementsByTagName("row")
        val rows = ArrayList<RawRow>(rowNodes.length)
        for (i in 0 until rowNodes.length) {
            val rowEl = rowNodes.item(i) as Element
            val rowNum = rowEl.getAttribute("r").toIntOrNull() ?: (rows.size + 1)
            val cells = LinkedHashMap<String, String>()
            val cellNodes = rowEl.childNodes
            for (j in 0 until cellNodes.length) {
                val node = cellNodes.item(j)
                if (node is Element && node.nodeName == "c") {
                    val ref = node.getAttribute("r")
                    val col = columnLetters(ref)
                    val value = cellValue(node, sharedStrings)
                    if (col.isNotEmpty() && value != null) cells[col] = value
                }
            }
            rows.add(RawRow(rowNum, cells))
        }
        return rows
    }

    private fun cellValue(c: Element, sharedStrings: List<String>): String? {
        return when (c.getAttribute("t")) {
            "s" -> {
                val idx = firstChild(c, "v")?.textContent?.trim()?.toIntOrNull() ?: return null
                if (idx in sharedStrings.indices) sharedStrings[idx] else null
            }
            "b" -> firstChild(c, "v")?.textContent?.trim()?.let { if (it == "1") "TRUE" else "FALSE" }
            "e" -> null
            "inlineStr" -> {
                val isEl = firstChild(c, "is") ?: return null
                val sb = StringBuilder()
                collectText(isEl, sb)
                sb.toString()
            }
            else -> {
                val v = firstChild(c, "v")?.textContent?.trim()
                if (v != null) {
                    v
                } else {
                    val isEl = firstChild(c, "is")
                    if (isEl != null) {
                        val sb = StringBuilder()
                        collectText(isEl, sb)
                        sb.toString()
                    } else null
                }
            }
        }
    }

    private fun firstChild(el: Element, name: String): Element? {
        val kids = el.childNodes
        for (i in 0 until kids.length) {
            val n = kids.item(i)
            if (n is Element && n.nodeName == name) return n
        }
        return null
    }

    private fun columnLetters(ref: String): String = ref.takeWhile { it.isLetter() }

    private data class HeaderInfo(val index: Int, val mapping: Map<String, String>)

    private fun detectHeader(rows: List<RawRow>): HeaderInfo {
        for ((idx, row) in rows.withIndex()) {
            val mapping = LinkedHashMap<String, String>()
            for ((col, text) in row.cells) {
                val kind = headerKind(text)
                if (kind != null && !mapping.containsKey(kind)) mapping[kind] = col
            }
            if (mapping.containsKey("platform") || mapping.containsKey("username") || mapping.containsKey("password")) {
                return HeaderInfo(idx, mapping)
            }
        }
        throw XlsxException("未找到表头行（需要：平台名称/账号/密码/邮箱/创建时间）")
    }

    private fun toImportRow(row: RawRow, mapping: Map<String, String>): ImportRow? {
        fun cell(kind: String): String? = mapping[kind]?.let { row.cells[it] }
        val platform = cell("platform")?.trim() ?: ""
        val username = cell("username")?.trim() ?: ""
        if (platform.isBlank() && username.isBlank()) return null
        val password = cell("password") ?: ""
        val email = cell("email")?.trim()?.takeIf { it.isNotEmpty() }
        val createdAtRaw = cell("createdAt")
        val createdAt = createdAtRaw?.let { parseDateCell(it) }
        return ImportRow(platform, username, password, email, createdAt, row.number)
    }

    private fun parseDateCell(raw: String): Long? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val num = t.toDoubleOrNull()
        if (num != null && num in 20000.0..60000.0) {
            return excelSerialToMillis(num)
        }
        return parseDateString(t)
    }

    private fun excelSerialToMillis(serial: Double): Long =
        ((serial - 25569.0) * 86_400_000.0).toLong()

    private fun parseDateString(s: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm"
        )
        for (f in formats) {
            try {
                val df = SimpleDateFormat(f, Locale.US)
                df.isLenient = false
                return df.parse(s)?.time
            } catch (e: Exception) {
                // 尝试下一种格式
            }
        }
        return null
    }

    private fun headerKind(text: String): String? {
        val n = text.trim().lowercase().replace(" ", "").replace("_", "")
        return when (n) {
            "平台名称", "platform" -> "platform"
            "账号", "账号名", "account", "username", "登录名" -> "username"
            "密码", "password" -> "password"
            "邮箱", "email", "mail" -> "email"
            "创建时间", "createdat", "createdtime", "time" -> "createdAt"
            else -> null
        }
    }

    private fun formatCreatedAt(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    private companion object {
        val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
    }
}