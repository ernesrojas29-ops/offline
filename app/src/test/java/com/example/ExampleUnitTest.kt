package com.example

import com.example.data.scraper.ScraperService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val scraperService = ScraperService()

    @Test
    fun `extractDurationMinutes parses various duration formats with regex`() {
        // Formato real de movil.todorelatos.com
        assertEquals(15, scraperService.extractDurationMinutes("Tiempo estimado de lectura: [ 15 min. ]"))
        assertEquals(8, scraperService.extractDurationMinutes("[ 8 min. ]"))
        assertEquals(22, scraperService.extractDurationMinutes("[22 min.]"))

        // Formatos generales
        assertEquals(12, scraperService.extractDurationMinutes("Lectura: 12 min"))
        assertEquals(5, scraperService.extractDurationMinutes("5 min"))
        assertEquals(18, scraperService.extractDurationMinutes("Lectura aproximada: 18 minutos"))
        assertEquals(25, scraperService.extractDurationMinutes("Duración: 25 mins"))
        assertEquals(40, scraperService.extractDurationMinutes("Lectura: 40 min"))
    }

    @Test
    fun `critical business rule - only stories with 25 minutes or less are eligible`() {
        val maxAllowed = ScraperService.MAX_ALLOWED_DURATION_MINUTES
        assertEquals(25, maxAllowed)

        val storyDuration1 = 12
        val storyDuration2 = 25
        val storyDuration3 = 26
        val storyDuration4 = 45

        assertTrue("Stories with 12 min must be accepted", storyDuration1 <= maxAllowed)
        assertTrue("Stories with 25 min must be accepted (<= 25)", storyDuration2 <= maxAllowed)
        assertFalse("Stories with 26 min must be omitted (> 25)", storyDuration3 <= maxAllowed)
        assertFalse("Stories with 45 min must be omitted (> 25)", storyDuration4 <= maxAllowed)
    }

    @Test
    fun `calculateDurationFromWordCount uses 190 words per minute rule`() {
        // 380 palabras deberían equivaler a 2 minutos
        val text380Words = (1..380).joinToString(" ") { "palabra" }
        assertEquals(2, scraperService.calculateDurationFromWordCount(text380Words))

        // 100 palabras deberían ser redondeadas al mínimo de 1 minuto
        val text100Words = (1..100).joinToString(" ") { "palabra" }
        assertEquals(1, scraperService.calculateDurationFromWordCount(text100Words))

        // 5700 palabras = 30 minutos (superaría el límite de 25)
        val text5700Words = (1..5700).joinToString(" ") { "palabra" }
        val calculated = scraperService.calculateDurationFromWordCount(text5700Words)
        assertEquals(30, calculated)
        assertTrue("Calculated duration should exceed 25 minutes limit", calculated > ScraperService.MAX_ALLOWED_DURATION_MINUTES)
    }
}
