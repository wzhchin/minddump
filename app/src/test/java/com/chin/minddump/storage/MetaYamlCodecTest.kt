package com.chin.minddump.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MetaYamlCodecTest {
    @Test
    fun roundTripsTagsAndEvents() {
        val meta = EntryMeta(
            tags = listOf("idea", "项目A"),
            events = listOf(
                EntryEvent(
                    due = LocalDateTime.of(2026, 6, 20, 9, 0),
                    state = EventState.PENDING,
                    trigger = EventTrigger.ONCE,
                ),
            ),
        )
        val decoded = MetaYamlCodec.decode(MetaYamlCodec.encode(meta))
        assertEquals(listOf("idea", "项目A"), decoded.tags)
        assertEquals(1, decoded.events.size)
        assertEquals(LocalDateTime.of(2026, 6, 20, 9, 0), decoded.events[0].due)
        assertEquals(EventState.PENDING, decoded.events[0].state)
        assertEquals(EventTrigger.ONCE, decoded.events[0].trigger)
    }

    @Test
    fun roundTripsTagsOnly() {
        val meta = EntryMeta(tags = listOf("solo"))
        val decoded = MetaYamlCodec.decode(MetaYamlCodec.encode(meta))
        assertEquals(listOf("solo"), decoded.tags)
        assertTrue(decoded.events.isEmpty())
    }

    @Test
    fun roundTripsEventsOnly() {
        val meta = EntryMeta(
            events = listOf(
                EntryEvent(due = LocalDateTime.of(2026, 6, 22, 9, 0)),
            ),
        )
        val decoded = MetaYamlCodec.decode(MetaYamlCodec.encode(meta))
        assertTrue(decoded.tags.isEmpty())
        assertEquals(1, decoded.events.size)
    }

    @Test
    fun emptyDocumentYieldsEmptyMeta() {
        assertEquals(EntryMeta.EMPTY, MetaYamlCodec.decode(""))
        assertEquals(EntryMeta.EMPTY, MetaYamlCodec.decode("   \n  "))
    }

    @Test
    fun malformedDocumentFailsClosedToEmpty() {
        // Missing closing bracket, unparseable events block — must not throw.
        val decoded = MetaYamlCodec.decode("tags: [a, b\nwhatever: : :")
        assertEquals(EntryMeta.EMPTY, decoded)
    }

    @Test
    fun unknownTopLevelKeysAreIgnored() {
        val decoded = MetaYamlCodec.decode("color: red\ntags: [idea]")
        assertEquals(listOf("idea"), decoded.tags)
    }
}
