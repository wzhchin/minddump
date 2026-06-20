package com.chin.minddump.ui

import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.TodoState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The active feed filter: three dimensions combined under AND.
 *
 * - [time]: an inclusive day window (preset bucket or custom range), or [TimeFilter.None].
 * - [todoStates]: entry must have one of these todo statuses; empty = no constraint.
 * - [tags]: **intersection** — entry must carry ALL selected tags; empty = no constraint.
 *
 * A dimension at its empty/default value imposes no constraint, so an all-default
 * [FeedFilter] leaves the feed unfiltered.
 */
data class FeedFilter(
    val time: TimeFilter = TimeFilter.None,
    val todoStates: Set<TodoState> = emptySet(),
    val tags: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = time is TimeFilter.None && todoStates.isEmpty() && tags.isEmpty()

    /** Whether the tag dimension has any selection (drives the tag-button indicator). */
    val tagsActive: Boolean get() = tags.isNotEmpty()

    /** Whether the todo dimension has any selection. */
    val todoActive: Boolean get() = todoStates.isNotEmpty()

    /** Whether the time dimension has any selection. */
    val timeActive: Boolean get() = time !is TimeFilter.None

    /**
     * True when [entry] satisfies every active dimension (AND). Inactive dimensions pass.
     * Tag matching is case-insensitive (consistent with tag authoring/dedup).
     */
    fun matches(entry: MindDumpEntry): Boolean {
        if (time !is TimeFilter.None && !time.contains(entry.date())) return false
        if (todoStates.isNotEmpty() && entry.todoState !in todoStates) return false
        if (tags.isNotEmpty()) {
            val entryTagsLower = entry.tags.map { it.lowercase() }.toSet()
            if (!tags.all { it.lowercase() in entryTagsLower }) return false
        }
        return true
    }
}

/** Inclusive day window for the time dimension. */
sealed interface TimeFilter {
    /** No time constraint. */
    data object None : TimeFilter

    /** A preset relative bucket. */
    data class Bucket(
        val kind: BucketKind
    ) : TimeFilter

    /** A custom inclusive `[start, end]` range. */
    data class Range(
        val start: LocalDate,
        val end: LocalDate
    ) : TimeFilter

    fun contains(date: LocalDate): Boolean = when (this) {
        None -> true
        is Bucket -> kind.rangeFor(date.today()).let { date in it }
        is Range -> date in start..end
    }
}

/**
 * Preset time buckets. [rangeFor] returns the inclusive day window for the bucket
 * anchored at [today] (the device's current date). Forward buckets reach into the
 * future (to surface entries carrying upcoming scheduled events); the past bucket
 * looks backward.
 */
enum class BucketKind {
    TODAY,
    NEXT_3_DAYS,
    NEXT_7_DAYS,
    PAST_7_DAYS,
    ;

    /** Inclusive `[start, end]` day range for this bucket anchored at [today]. */
    fun rangeFor(today: LocalDate): ClosedRange<LocalDate> = when (this) {
        TODAY -> today..today
        NEXT_3_DAYS -> today..today.plusDays(2)
        NEXT_7_DAYS -> today..today.plusDays(6)
        PAST_7_DAYS -> today.minusDays(6)..today
    }
}

private fun LocalDate.today(): LocalDate = LocalDate.now()

/** Day-granularity date of an entry, derived from its rebuild-stable [MindDumpEntry.tid]. */
fun MindDumpEntry.date(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(tid).atZone(zone).toLocalDate()

/**
 * Compute the faceted tag list for the inline tag filter. [qualifying] is the set
 * of entries already narrowed by the time + todo dimensions (and search); the tag
 * dimension's own narrowing is intentionally NOT applied here, so selecting tags
 * doesn't immediately hide the other still-available tags. The result lists, with
 * stable display casing:
 * - every currently-selected tag (so it stays deselectable), then
 * - every tag that co-occurs with the selected tags among [qualifying]
 *   (i.e. appears on an entry that also carries all selected tags).
 *
 * A tag no qualifying entry carries is omitted. This is the intersection-narrowing
 * faceted behaviour: picking tag1 keeps tag2/tag3 visible (they co-occur on some
 * remaining entry) and hides tag4 (no remaining entry has it).
 */
fun computeFacetTags(
    qualifying: List<MindDumpEntry>,
    selectedTags: Set<String>,
): List<String> {
    val selectedLower = selectedTags.map { it.lowercase() }.toSet()
    val coOccurring = qualifying
        .filter { e -> selectedLower.all { sel -> e.tags.any { it.lowercase() == sel } } }
        .flatMap { it.tags }
        .map { it.lowercase() }
        .distinct()
        .sorted()
    val casingByLower = qualifying.flatMap { it.tags }.associateBy { it.lowercase() }
    return (selectedTags + coOccurring.filter { it !in selectedLower })
        .mapNotNull { casingByLower[it.lowercase()] }
        .distinct()
}
