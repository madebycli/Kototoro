package org.skepsun.kototoro.details.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class TrackingScoreFormatTest {

    @Test
    fun `whole scores drop the decimal and others keep one`() {
        assertEquals("8", formatTrackingScore(8f, Locale.ENGLISH))
        assertEquals("8.5", formatTrackingScore(8.5f, Locale.ENGLISH))
        assertEquals("8.2", formatTrackingScore(8.21f, Locale.ENGLISH))
        assertEquals("10", formatTrackingScore(9.96f, Locale.ENGLISH))
    }

    @Test
    fun `the decimal separator follows the locale`() {
        assertEquals("8,4", formatTrackingScore(8.4f, Locale.GERMAN))
    }
}
