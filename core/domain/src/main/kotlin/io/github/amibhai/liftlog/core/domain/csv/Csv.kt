package io.github.amibhai.liftlog.core.domain.csv

private const val QUOTE = '"'
private const val BYTE_ORDER_MARK = "﻿"

/** A value with one of these has to be quoted. */
private const val SPECIAL = ",\"\n\r"
private val Delimiters = listOf(',', ';', '\t')

/** A table with a header; every row maps the column names to its values. */
class CsvTable(val columns: List<String>, val rows: List<Map<String, String>>)

/** One line of a CSV file (RFC 4180), without its line break. */
fun csvLine(values: List<String>): String = values.joinToString(",") { value ->
    if (value.any { it in SPECIAL }) {
        QUOTE + value.replace("$QUOTE", "$QUOTE$QUOTE") + QUOTE
    } else {
        value
    }
}

/** Reads a CSV file with a header. Commas, semicolons and tabs are all in use; the header tells which. */
fun parseCsv(text: String): CsvTable {
    val content = text.removePrefix(BYTE_ORDER_MARK)
    val header = content.lineSequence().firstOrNull().orEmpty()
    val delimiter = Delimiters.maxBy { candidate -> header.count { it == candidate } }
    val records = CsvReader(content, delimiter).records()
    val columns = records.firstOrNull().orEmpty().map { it.trim() }
    val rows = records.drop(1)
        .filter { record -> record.any { it.isNotBlank() } }
        .map { record -> columns.zip(record).toMap() }
    return CsvTable(columns, rows)
}

/** Splits CSV text into records: quoted values may contain delimiters, line breaks and doubled quotes. */
private class CsvReader(private val text: String, private val delimiter: Char) {
    private val records = mutableListOf<List<String>>()
    private var record = mutableListOf<String>()
    private val value = StringBuilder()
    private var isQuoted = false
    private var position = 0

    fun records(): List<List<String>> {
        while (position < text.length) {
            read(text[position])
            position++
        }
        if (value.isNotEmpty() || record.isNotEmpty()) endRecord()
        return records
    }

    private fun read(char: Char) {
        when {
            isQuoted && char == QUOTE -> readQuote()
            isQuoted -> value.append(char)
            char == QUOTE && value.isEmpty() -> isQuoted = true
            char == delimiter -> endValue()
            char == '\n' -> endRecord()
            char != '\r' -> value.append(char)
        }
    }

    private fun readQuote() {
        if (text.getOrNull(position + 1) == QUOTE) {
            value.append(QUOTE)
            position++
        } else {
            isQuoted = false
        }
    }

    private fun endValue() {
        record += value.toString()
        value.clear()
    }

    private fun endRecord() {
        endValue()
        records += record
        record = mutableListOf()
    }
}
