package io.github.amibhai.liftlog.core.domain.csv

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CsvTest {

    @Test
    fun `rows map the columns to their values`() {
        val table = parseCsv("Date,Name\n2024-01-01,Push\n2024-01-03,Pull\n")

        assertThat(table.columns).containsExactly("Date", "Name").inOrder()
        assertThat(table.rows.map { it["Name"] }).containsExactly("Push", "Pull").inOrder()
    }

    @Test
    fun `quoted values may hold delimiters, quotes and line breaks`() {
        val table = parseCsv("Name,Notes\r\n\"Bench, flat\",\"He said \"\"more\"\"\r\nline two\"\r\nRow,\r\n")

        assertThat(table.rows[0]["Name"]).isEqualTo("Bench, flat")
        assertThat(table.rows[0]["Notes"]).isEqualTo("He said \"more\"\r\nline two")
        assertThat(table.rows[1]["Notes"]).isEmpty()
    }

    @Test
    fun `the header tells the delimiter`() {
        assertThat(parseCsv("A;B;C\n1;2,5;3").rows.single()["B"]).isEqualTo("2,5")
        assertThat(parseCsv("A\tB\n1\t2").rows.single()["B"]).isEqualTo("2")
    }

    @Test
    fun `byte order marks, blank lines and short rows do no harm`() {
        val table = parseCsv("﻿A,B\n\n1\n1,2,3\n")

        assertThat(table.columns).containsExactly("A", "B").inOrder()
        assertThat(table.rows.map { it["A"] }).containsExactly("1", "1")
        assertThat(table.rows[0]["B"]).isNull()
        assertThat(parseCsv("").rows).isEmpty()
    }

    @Test
    fun `what is written can be read again`() {
        val values = listOf("plain", "with, comma", "with \"quotes\"", "two\nlines", "")

        val table = parseCsv("a,b,c,d,e\n" + csvLine(values))

        assertThat(table.rows.single().values.toList()).isEqualTo(values)
    }
}
