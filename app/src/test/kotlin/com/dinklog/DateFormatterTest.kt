package com.dinklog

import com.dinklog.utils.formatMatchDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateFormatterTest {

    private val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

    @Test
    fun formatMatchDate_returnsCorrectDateString() {
        val millis = 1_704_153_600_000L // Jan 02, 2024 00:00:00 UTC
        val expected = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        assertEquals(expected, millis.formatMatchDate())
    }

    @Test
    fun formatMatchDate_matchesExpectedPattern() {
        val result = System.currentTimeMillis().formatMatchDate()
        // Must match "MMM dd, yyyy" e.g. "Apr 17, 2026"
        assert(result.matches(Regex("[A-Za-z]{3} \\d{2}, \\d{4}"))) {
            "Unexpected format: $result"
        }
    }

    @Test
    fun formatMatchDate_differentTimestampsProduceDifferentDates() {
        val jan1 = 1_704_067_200_000L  // Jan 01, 2024 UTC
        val dec31 = 1_703_980_800_000L // Dec 31, 2023 UTC
        assertNotEquals(jan1.formatMatchDate(), dec31.formatMatchDate())
    }

    private fun assertNotEquals(a: String, b: String) {
        assert(a != b) { "Expected different dates but got: $a" }
    }
}
