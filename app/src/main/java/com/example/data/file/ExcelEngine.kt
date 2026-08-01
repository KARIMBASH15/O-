package com.example.data.file

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ExcelCell(
    var value: String = "",
    var formula: String = ""
)

data class ExcelRow(
    val rowIndex: Int,
    val cells: MutableList<ExcelCell> = mutableListOf()
)

data class ExcelData(
    val sheetName: String = "Sheet1",
    val headers: MutableList<String> = mutableListOf(),
    val rows: MutableList<ExcelRow> = mutableListOf()
) {
    fun evaluateFormula(formulaStr: String): String {
        if (!formulaStr.startsWith("=")) return formulaStr
        val clean = formulaStr.substring(1).trim().uppercase()
        try {
            if (clean.startsWith("SUM(") && clean.endsWith(")")) {
                val range = clean.substring(4, clean.length - 1)
                val values = getValuesInRange(range)
                val sum = values.mapNotNull { it.toDoubleOrNull() }.sum()
                return if (sum % 1 == 0.0) sum.toLong().toString() else String.format("%.2f", sum)
            } else if (clean.startsWith("AVERAGE(") && clean.endsWith(")")) {
                val range = clean.substring(8, clean.length - 1)
                val values = getValuesInRange(range)
                val nums = values.mapNotNull { it.toDoubleOrNull() }
                if (nums.isEmpty()) return "0"
                val avg = nums.average()
                return if (avg % 1 == 0.0) avg.toLong().toString() else String.format("%.2f", avg)
            }
        } catch (e: Exception) {
            return "#ERR"
        }
        return formulaStr
    }

    private fun getValuesInRange(range: String): List<String> {
        val parts = range.split(":")
        val list = mutableListOf<String>()
        if (parts.size == 2) {
            val startCol = parts[0].filter { it.isLetter() }
            val startRow = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 1
            val endCol = parts[1].filter { it.isLetter() }
            val endRow = parts[1].filter { it.isDigit() }.toIntOrNull() ?: rows.size

            val startColIdx = colNameToIndex(startCol)
            val endColIdx = colNameToIndex(endCol)

            for (r in (startRow - 1) until minOf(endRow, rows.size)) {
                val rowObj = rows[r]
                for (c in startColIdx..endColIdx) {
                    if (c < rowObj.cells.size) {
                        list.add(rowObj.cells[c].value)
                    }
                }
            }
        }
        return list
    }

    private fun colNameToIndex(colName: String): Int {
        var result = 0
        for (ch in colName.uppercase()) {
            result = result * 26 + (ch - 'A' + 1)
        }
        return (result - 1).coerceAtLeast(0)
    }
}

object ExcelEngine {

    fun loadExcelOrCsv(file: File): ExcelData {
        if (!file.exists()) {
            return createDefaultSheet(file.nameWithoutExtension)
        }
        val ext = file.extension.lowercase()
        return try {
            if (ext == "csv") {
                parseCsv(file)
            } else if (ext == "xlsx") {
                parseXlsx(file)
            } else {
                parseCsv(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text lines as CSV
            parseCsv(file)
        }
    }

    fun saveExcelOrCsv(file: File, data: ExcelData): Boolean {
        return try {
            val ext = file.extension.lowercase()
            if (ext == "csv") {
                saveCsv(file, data)
            } else {
                // Save both formatted CSV and text sheet format
                saveCsv(file, data)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseCsv(file: File): ExcelData {
        val lines = file.readLines(StandardCharsets.UTF_8)
        if (lines.isEmpty()) return createDefaultSheet(file.nameWithoutExtension)

        // Detect delimiter: comma, semicolon, or tab
        val firstLine = lines.firstOrNull() ?: ""
        val delimiter = when {
            firstLine.contains(";") -> ';'
            firstLine.contains("\t") -> '\t'
            else -> ','
        }

        val rows = mutableListOf<ExcelRow>()
        var maxCols = 0

        lines.forEachIndexed { rIdx, line ->
            if (line.isNotBlank()) {
                val tokens = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
                maxCols = maxOf(maxCols, tokens.size)
                val cells = tokens.map { ExcelCell(value = it) }.toMutableList()
                rows.add(ExcelRow(rowIndex = rIdx, cells = cells))
            }
        }

        val headers = MutableList(maxCols) { idx -> getColName(idx) }

        // Pad row cells
        rows.forEach { row ->
            while (row.cells.size < maxCols) {
                row.cells.add(ExcelCell())
            }
        }

        return ExcelData(sheetName = file.nameWithoutExtension, headers = headers, rows = rows)
    }

    private fun saveCsv(file: File, data: ExcelData) {
        val sb = StringBuilder()
        data.rows.forEach { row ->
            val line = row.cells.joinToString(",") { cell ->
                val v = cell.value
                if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                    "\"${v.replace("\"", "\"\"")}\""
                } else v
            }
            sb.append(line).append("\n")
        }
        file.writeText(sb.toString(), StandardCharsets.UTF_8)
    }

    private fun parseXlsx(file: File): ExcelData {
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(file)
            val sharedStrings = mutableListOf<String>()

            val sharedEntry = zipFile.getEntry("xl/sharedStrings.xml")
            if (sharedEntry != null) {
                val isInputStream = zipFile.getInputStream(sharedEntry)
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(isInputStream, "UTF-8")

                var eventType = parser.eventType
                var text = ""
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "t") {
                                text = parser.nextText()
                                sharedStrings.add(text)
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }

            val sheetEntry = zipFile.getEntry("xl/worksheets/sheet1.xml")
                ?: zipFile.entries().asSequence().firstOrNull { it.name.contains("worksheets/sheet") }

            if (sheetEntry != null) {
                val isInputStream = zipFile.getInputStream(sheetEntry)
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(isInputStream, "UTF-8")

                val rows = mutableListOf<ExcelRow>()
                var currentCellType = ""
                var currentCellValue = ""
                var currentFormula = ""
                var currentCellCol = 0
                var currentRowIndex = 0
                var currentCells = mutableListOf<ExcelCell>()
                var maxCols = 0

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tagName = parser.name
                            if (tagName == "row") {
                                currentCells = mutableListOf()
                                currentRowIndex = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: (rows.size + 1)
                            } else if (tagName == "c") {
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                val rRef = parser.getAttributeValue(null, "r") ?: ""
                                currentCellCol = extractColIndex(rRef)
                            } else if (tagName == "f") {
                                currentFormula = parser.nextText()
                            } else if (tagName == "v") {
                                currentCellValue = parser.nextText()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tagName = parser.name
                            if (tagName == "c") {
                                var valStr = currentCellValue
                                if (currentCellType == "s" && valStr.isNotEmpty()) {
                                    val idx = valStr.toIntOrNull()
                                    if (idx != null && idx < sharedStrings.size) {
                                        valStr = sharedStrings[idx]
                                    }
                                }
                                while (currentCells.size < currentCellCol) {
                                    currentCells.add(ExcelCell())
                                }
                                val formulaStr = if (currentFormula.isNotEmpty()) "=$currentFormula" else ""
                                currentCells.add(ExcelCell(value = valStr, formula = formulaStr))
                                maxCols = maxOf(maxCols, currentCells.size)
                                currentCellValue = ""
                                currentFormula = ""
                                currentCellType = ""
                            } else if (tagName == "row") {
                                rows.add(ExcelRow(rowIndex = currentRowIndex - 1, cells = currentCells))
                            }
                        }
                    }
                    eventType = parser.next()
                }

                val headers = MutableList(maxCols) { idx -> getColName(idx) }
                return ExcelData(sheetName = file.nameWithoutExtension, headers = headers, rows = rows)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { zipFile?.close() } catch (ignored: Exception) {}
        }
        return parseCsv(file)
    }

    fun createDefaultSheet(title: String): ExcelData {
        val headers = mutableListOf("A", "B", "C", "D", "E")
        val rows = mutableListOf(
            ExcelRow(0, mutableListOf(ExcelCell("المادة/البند"), ExcelCell("الكمية"), ExcelCell("السعر"), ExcelCell("الإجمالي"), ExcelCell("الحالة"))),
            ExcelRow(1, mutableListOf(ExcelCell("كمبيوتر محمول"), ExcelCell("2"), ExcelCell("1500"), ExcelCell("3000"), ExcelCell("مكتمل"))),
            ExcelRow(2, mutableListOf(ExcelCell("شاشة 4K"), ExcelCell("4"), ExcelCell("400"), ExcelCell("1600"), ExcelCell("مكتمل"))),
            ExcelRow(3, mutableListOf(ExcelCell("لوحة مفاتيح"), ExcelCell("10"), ExcelCell("50"), ExcelCell("500"), ExcelCell("قيد الشحن"))),
            ExcelRow(4, mutableListOf(ExcelCell("المجموع الكلي"), ExcelCell("16"), ExcelCell("-"), ExcelCell("5100"), ExcelCell("معتمد")))
        )
        return ExcelData(sheetName = title, headers = headers, rows = rows)
    }

    private fun extractColIndex(ref: String): Int {
        val colLetters = ref.takeWhile { it.isLetter() }
        if (colLetters.isEmpty()) return 0
        var res = 0
        for (ch in colLetters.uppercase()) {
            res = res * 26 + (ch - 'A' + 1)
        }
        return res - 1
    }

    fun getColName(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.append(('A' + (i % 26)))
            i = (i / 26) - 1
        }
        return sb.reverse().toString()
    }
}
