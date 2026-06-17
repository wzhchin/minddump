package com.chin.minddump.storage

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import timber.log.Timber

/**
 * Serializes/parses an [EntryMeta] to/from a constrained YAML document.
 *
 * Supported document shapes only:
 *   tags: [idea, 项目A]
 *   events:
 *     - due: 2026-06-20T09:00
 *       state: pending
 *       trigger: once
 *
 * This is NOT a general YAML parser. It recognizes exactly these two top-level
 * keys. Any unrecognized structure, malformed line, or parse error makes the
 * whole document fail closed to [EntryMeta.EMPTY] (logged, never thrown) — a
 * corrupted sidecar must not crash the app.
 *
 * The serializer always emits tags on one inline-flow line and each event as a
 * 3-field block, which round-trips through this parser.
 */
@Suppress("TooManyFunctions", "NestedBlockDepth")
object MetaYamlCodec {
    private const val TAGS_KEY = "tags"
    private const val EVENTS_KEY = "events"
    private const val DUE_KEY = "due"
    private const val STATE_KEY = "state"
    private const val TRIGGER_KEY = "trigger"

    private val DUE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    private val EVENT_FIELD_KEYS = setOf(DUE_KEY, STATE_KEY, TRIGGER_KEY)

    fun encode(meta: EntryMeta): String {
        val sb = StringBuilder()
        if (meta.tags.isNotEmpty()) {
            sb.append(TAGS_KEY).append(": [")
            sb.append(meta.tags.joinToString(", ") { escapeTag(it) })
            sb.append("]\n")
        }
        if (meta.events.isNotEmpty()) {
            sb.append(EVENTS_KEY).append(":\n")
            meta.events.forEach { ev ->
                sb.append("  - $DUE_KEY: ").append(ev.due.format(DUE_FORMAT)).append('\n')
                sb.append("    $STATE_KEY: ").append(ev.state.name.lowercase()).append('\n')
                sb.append("    $TRIGGER_KEY: ").append(ev.trigger.name.lowercase()).append('\n')
            }
        }
        return sb.toString()
    }

    fun decode(text: String): EntryMeta {
        if (text.isBlank()) return EntryMeta.EMPTY
        return runCatching { decodeInternal(text) }
            .onFailure { Timber.w(it, "Malformed metadata sidecar, treating as empty") }
            .getOrDefault(EntryMeta.EMPTY)
    }

    private fun decodeInternal(text: String): EntryMeta {
        val lines = text.lines().map { it }.filter { it.isNotBlank() }
        var i = 0
        val tags = mutableListOf<String>()
        val events = mutableListOf<EntryEvent>()

        while (i < lines.size) {
            val line = lines[i]
            if (!line.startsWith(' ') && !line.startsWith('-')) {
                // top-level key
                val colon = line.indexOf(':')
                require(colon >= 0) { "Malformed top-level line: $line" }
                val key = line.substring(0, colon).trim()
                val rest = line.substring(colon + 1).trim()
                when (key) {
                    TAGS_KEY -> tags.addAll(parseTagList(rest))
                    EVENTS_KEY -> {
                        require(rest.isEmpty()) { "events: must be a block list" }
                        val (parsed, next) = parseEventBlock(lines, i + 1)
                        events.addAll(parsed)
                        i = next
                        continue
                    }
                    else -> { /* ignore unknown top-level keys */ }
                }
            }
            i++
        }
        return EntryMeta(tags = tags.distinctBy { it.lowercase() }, events = events)
    }

    // Inline list form: [a, b, c]
    private fun parseTagList(rest: String): List<String> {
        val trimmed = rest.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        require(trimmed.startsWith("[") && trimmed.endsWith("]")) {
            "tags: must be an inline list [..]: $rest"
        }
        val inner = trimmed.substring(1, trimmed.length - 1)
        if (inner.isBlank()) return emptyList()
        return inner
            .split(",")
            .map { unescapeTag(it.trim()) }
            .filter { it.isNotEmpty() }
    }

    // Parses consecutive "  - due: ..." / "    state: ..." blocks starting at [start].
    // Returns parsed events and the index of the first line that is NOT part of the block.
    private fun parseEventBlock(lines: List<String>, start: Int): Pair<List<EntryEvent>, Int> {
        val events = mutableListOf<EntryEvent>()
        var i = start
        var current: MutableMap<String, String>? = null

        fun flush() {
            val map = current ?: return
            val dueStr = requireNotNull(map[DUE_KEY]) { "Event missing due" }
            val state = map[STATE_KEY]?.let { parseState(it) } ?: EventState.PENDING
            val trigger = map[TRIGGER_KEY]?.let { parseTrigger(it) } ?: EventTrigger.ONCE
            val due = parseDue(dueStr)
            events.add(EntryEvent(due = due, state = state, trigger = trigger))
            current = null
        }

        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("  - ")) {
                // new event starts
                flush()
                current = mutableMapOf()
                val kv = parseField(line.substring(4))
                kv?.let { (k, v) -> current!![k] = v }
            } else if (line.startsWith("    ")) {
                // continuation field of current event
                requireNotNull(current) { "Event field without event start: $line" }
                val kv = parseField(line.trim())
                kv?.let { (k, v) ->
                    require(k in EVENT_FIELD_KEYS) { "Unknown event field: $k" }
                    current!![k] = v
                }
            } else if (line.startsWith(' ') || line.startsWith('\t')) {
                // indented but unexpected — tolerate by ignoring
            } else {
                // back to top level
                break
            }
            i++
        }
        flush()
        return events to i
    }

    private fun parseField(s: String): Pair<String, String>? {
        val colon = s.indexOf(':')
        if (colon < 0) return null
        val k = s.substring(0, colon).trim()
        val v = s.substring(colon + 1).trim()
        return k to v
    }

    private fun parseDue(s: String): LocalDateTime =
        try {
            LocalDateTime.parse(s.trim(), DUE_FORMAT)
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(s.trim()) // tolerate full ISO if present
            } catch (_: DateTimeParseException) {
                throw IllegalArgumentException("Unparseable due time: $s")
            }
        }

    private fun parseState(s: String): EventState =
        EventState.entries.firstOrNull { it.name.equals(s.trim(), ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown event state: $s")

    private fun parseTrigger(s: String): EventTrigger =
        EventTrigger.entries.firstOrNull { it.name.equals(s.trim(), ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown event trigger: $s")

    // A tag value in YAML inline list may be bare or single-quoted. Strip quotes
    // and colons that the inline form can't legally contain per the tag rules.
    private fun escapeTag(tag: String): String {
        val needsQuoting = tag.any { it == ',' || it == ':' || it == '[' || it == ']' }
        return if (needsQuoting) "\"${tag.replace("\"", "\\\"")}\"" else tag
    }

    private fun unescapeTag(raw: String): String {
        var s = raw.trim()
        val isDoubleQuoted = s.startsWith("\"") && s.endsWith("\"")
        val isSingleQuoted = s.startsWith("'") && s.endsWith("'")
        if (isDoubleQuoted || isSingleQuoted) {
            s = s.substring(1, s.length - 1).replace("\\\"", "\"")
        }
        return s
    }
}
