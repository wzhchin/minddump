package com.chin.minddump.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class TidTest {
    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun `parseStem extracts timestamp and zero offset when no suffix`() {
        val (ts, offset) = Tid.parseStem("2506-13-143022-f")
        assertEquals("2506-13-143022", ts)
        assertEquals(0, offset)
    }

    @Test
    fun `parseStem extracts nonzero offset from collision suffix`() {
        val (ts, offset) = Tid.parseStem("2506-13-143022-f_1")
        assertEquals("2506-13-143022", ts)
        assertEquals(1, offset)
        val (_, offset2) = Tid.parseStem("2506-13-143022-f-photo_7")
        assertEquals(7, offset2)
    }

    @Test
    fun `tidOf is rebuild-stable for a given filename stem`() {
        // Same inputs ⇒ same tid, the core rebuild-stability guarantee.
        val first = Tid.tidOfStem("2506-13-143022-f", zone)
        val again = Tid.tidOfStem("2506-13-143022-f", zone)
        assertEquals(first, again)
    }

    @Test
    fun `same-second entries get distinct tids via offset`() {
        val base = Tid.tidOf("2506-13-143022", offset = 0, zone = zone)
        val first = Tid.tidOf("2506-13-143022", offset = 1, zone = zone)
        val second = Tid.tidOf("2506-13-143022", offset = 2, zone = zone)
        assertNotEquals(base, first)
        assertNotEquals(first, second)
        assertTrue("offset should advance by exactly 1", first == base + 1)
        assertTrue(second == base + 2)
    }

    @Test
    fun `different seconds produce different base tids`() {
        val a = Tid.tidOf("2506-13-143022", offset = 0, zone = zone)
        val b = Tid.tidOf("2506-13-143023", offset = 0, zone = zone)
        assertTrue("one-second-apart tids differ by ~1000ms", b - a >= 1000L)
    }

    @Test
    fun `same-second across offsets round-trips through the domain timestamp getter`() {
        val tid = Tid.tidOf("2506-13-143022", offset = 3, zone = zone)
        // The MindDumpEntry.timestamp getter recovers the SECOND; the sub-second
        // offset is intentionally dropped (it is not representable in the filename).
        val entry = MindDumpEntry(
            file = java.io.File("/x/2506-13-143022-f_3.md"),
            type = EntryType.TEXT,
            space = Space.PUBLIC,
            monthFolder = "2025-06",
            tid = tid,
        )
        assertEquals("2506-13-143022", entry.timestamp)
    }
}
