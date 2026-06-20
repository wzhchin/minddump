package com.chin.minddump.ui

import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class FeedFilterTest {
    private fun entry(
        tid: Long,
        tags: List<String> = emptyList(),
        todo: TodoState = TodoState.NONE,
    ) = MindDumpEntry(
        file = File("/x/$tid-f.md"),
        type = EntryType.TEXT,
        space = Space.PUBLIC,
        monthFolder = "2025-06",
        tid = tid,
        tags = tags,
        todoState = todo,
    )

    @Test
    fun `empty filter matches everything`() {
        val filter = FeedFilter()
        assertTrue(filter.isEmpty)
        assertTrue(filter.matches(entry(tid = todayMillis())))
        assertTrue(filter.matches(entry(tid = 0L, tags = listOf("a"))))
    }

    @Test
    fun `tag dimension is intersection`() {
        val filter = FeedFilter(tags = setOf("a", "b"))
        assertTrue(filter.matches(entry(tid = 0L, tags = listOf("a", "b", "c"))))
        assertFalse(filter.matches(entry(tid = 0L, tags = listOf("a")))) // missing b
        assertFalse(filter.matches(entry(tid = 0L, tags = listOf("b"))))
    }

    @Test
    fun `tag matching is case-insensitive`() {
        val filter = FeedFilter(tags = setOf("Idea"))
        assertTrue(filter.matches(entry(tid = 0L, tags = listOf("idea"))))
    }

    @Test
    fun `todo dimension matches any selected status`() {
        val filter = FeedFilter(todoStates = setOf(TodoState.TODO, TodoState.DOING))
        assertTrue(filter.matches(entry(tid = 0L, todo = TodoState.TODO)))
        assertTrue(filter.matches(entry(tid = 0L, todo = TodoState.DOING)))
        // Plain notes excluded when a status is selected.
        assertFalse(filter.matches(entry(tid = 0L, todo = TodoState.NONE)))
        assertFalse(filter.matches(entry(tid = 0L, todo = TodoState.DONE)))
    }

    @Test
    fun `time bucket contains is inclusive`() {
        val today = LocalDate.now()
        // TODAY: [today, today]
        assertTrue(TimeFilter.Bucket(BucketKind.TODAY).contains(today))
        assertFalse(TimeFilter.Bucket(BucketKind.TODAY).contains(today.minusDays(1)))
        assertFalse(TimeFilter.Bucket(BucketKind.TODAY).contains(today.plusDays(1)))
        // PAST_7_DAYS: [today-6, today]
        assertTrue(TimeFilter.Bucket(BucketKind.PAST_7_DAYS).contains(today))
        assertTrue(TimeFilter.Bucket(BucketKind.PAST_7_DAYS).contains(today.minusDays(6)))
        assertFalse(TimeFilter.Bucket(BucketKind.PAST_7_DAYS).contains(today.minusDays(7)))
        // NEXT_7_DAYS: [today, today+6]
        assertTrue(TimeFilter.Bucket(BucketKind.NEXT_7_DAYS).contains(today))
        assertTrue(TimeFilter.Bucket(BucketKind.NEXT_7_DAYS).contains(today.plusDays(6)))
        assertFalse(TimeFilter.Bucket(BucketKind.NEXT_7_DAYS).contains(today.plusDays(7)))
        // NEXT_3_DAYS: [today, today+2]
        assertTrue(TimeFilter.Bucket(BucketKind.NEXT_3_DAYS).contains(today.plusDays(2)))
        assertFalse(TimeFilter.Bucket(BucketKind.NEXT_3_DAYS).contains(today.plusDays(3)))
    }

    @Test
    fun `custom range is inclusive on both endpoints`() {
        val today = LocalDate.now()
        val range = TimeFilter.Range(today.minusDays(2), today)
        assertTrue(range.contains(today.minusDays(2)))
        assertTrue(range.contains(today))
        assertFalse(range.contains(today.minusDays(3)))
        assertFalse(range.contains(today.plusDays(1)))
    }

    @Test
    fun `dimensions AND together`() {
        val today = LocalDate.now()
        val filter = FeedFilter(
            time = TimeFilter.Bucket(BucketKind.TODAY),
            todoStates = setOf(TodoState.TODO),
            tags = setOf("x"),
        )
        // All three satisfied.
        assertTrue(filter.matches(entry(tid = today.toEpochMilli(), tags = listOf("x"), todo = TodoState.TODO)))
        // Fails time (yesterday).
        val yesterday = entry(
            tid = today.minusDays(1).toEpochMilli(),
            tags = listOf("x"),
            todo = TodoState.TODO,
        )
        assertFalse(filter.matches(yesterday))
        // Fails todo.
        assertFalse(filter.matches(entry(tid = today.toEpochMilli(), tags = listOf("x"), todo = TodoState.DONE)))
        // Fails tag (intersection: needs x).
        assertFalse(filter.matches(entry(tid = today.toEpochMilli(), tags = listOf("y"), todo = TodoState.TODO)))
    }

    @Test
    fun `entry date is derived from tid at day granularity`() {
        val today = LocalDate.now()
        val entry = entry(tid = today.toEpochMilli())
        assertEquals(today, entry.date())
    }

    private fun todayMillis(): Long = LocalDate.now().toEpochMilli()

    private fun LocalDate.toEpochMilli(): Long =
        atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ── Faceted tag list (the A/B/C example from the spec) ──

    private val a = entry(tid = 1L, tags = listOf("tag1", "tag2"))
    private val b = entry(tid = 2L, tags = listOf("tag1", "tag3"))
    private val c = entry(tid = 3L, tags = listOf("tag2", "tag4"))
    private val all = listOf(a, b, c)

    @Test
    fun `facets with no selection list all tags`() {
        assertEquals(listOf("tag1", "tag2", "tag3", "tag4"), computeFacetTags(all, emptySet()))
    }

    @Test
    fun `selecting tag1 hides tag4 and keeps tag2 tag3`() {
        // A (tag1,tag2), B (tag1,tag3) remain; tag4 only lives on C which lacks tag1.
        val facets = computeFacetTags(all, setOf("tag1"))
        assertEquals(listOf("tag1", "tag2", "tag3"), facets)
    }

    @Test
    fun `selecting tag1 and tag2 intersects to A`() {
        val facets = computeFacetTags(all, setOf("tag1", "tag2"))
        // Only A carries both; tag3 (on B, which lacks tag2) drops out.
        assertEquals(listOf("tag1", "tag2"), facets)
    }

    @Test
    fun `selected tag stays listed and leads even when no other tag co-occurs`() {
        // tag4 is only on C (tag2,tag4). Selecting tag4 alone → facets lead with the
        // selected tag, then co-occurring tags: [tag4, tag2].
        val facets = computeFacetTags(all, setOf("tag4"))
        assertEquals(listOf("tag4", "tag2"), facets)
    }
}
