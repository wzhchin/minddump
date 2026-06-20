package com.chin.minddump.storage

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Structured metadata stored in an `m` sidecar (tags + scheduled events) for an
 * owner entry. Serialized to/from a constrained YAML document by [MetaYamlCodec].
 */
data class EntryMeta(
    val tags: List<String> = emptyList(),
    val events: List<EntryEvent> = emptyList(),
) {
    val isEmpty: Boolean get() = tags.isEmpty() && events.isEmpty()

    companion object {
        val EMPTY = EntryMeta()
    }
}

/**
 * A single scheduled event on an entry. Decoupled from the filename todo-state:
 * any entry may carry events regardless of its todo status.
 */
data class EntryEvent(
    val due: LocalDateTime,
    val state: EventState = EventState.PENDING,
    val trigger: EventTrigger = EventTrigger.ONCE,
) {
    /**
     * A stable, human-readable key for this event within its owner sidecar.
     * Used to address alarms (owner filePath + this key). Uses the due time so
     * identical dues collapse; the scheduler treats the list index + due as the
     * identity and cancels by cancel-then-set, so collisions are tolerable.
     */
    fun key(): String = "${due.format(KEY_FORMAT)}#${state.name}"

    /** Epoch millis for the alarm, in the system default timezone. */
    fun dueMillis(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long =
        due.atZone(zone).toInstant().toEpochMilli()
}

enum class EventState { PENDING, FIRED, SNOOZED }

enum class EventTrigger { ONCE }

private val KEY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
