package com.chin.minddump.storage

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Derivation of `tid`, the rebuild-stable primary key for entries and comments.
 *
 * Filenames are second-resolution (`yyMM-dd-HHmmss`); the millisecond part of the
 * capture instant is never stored. To produce a unique, rebuild-stable identity we
 * therefore take the epoch-millis of the parsed second-resolution timestamp and add
 * a small integer [offset] that distinguishes entries created within the same
 * second. The offset is recoverable from the filename: [uniqueFile] appends `_1`,
 * `_2`, … on collision, so "no suffix" ⇒ offset 0, `_1` ⇒ 1, and so on. Because
 * both inputs come from the filename, `tid` is identical before and after a full
 * rebuild-from-disk — making it a valid foreign-key target for `parentId`.
 *
 * Uniqueness holds **within a space** (the natural rebuild / collision unit);
 * entries with the same second-resolution timestamp in different month folders
 * share a base epoch-millis but differ by their per-directory collision offset.
 */
object Tid {
    /** Matches the filename timestamp segment `yyMM-dd-HHmmss` (e.g. `2506-13-143022`). */
    val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMM-dd-HHmmss")

    private val COLLISION_SUFFIX = Regex("""_(\d+)$""")

    /**
     * The `tid` for a second-resolution [timestamp] with [offset] same-second entries
     * before it (0 when this is the sole entry in its second). [zone] defaults to the
     * system timezone — filenames carry no zone, so local time at capture is assumed,
     * matching [FileStorageEngine.nowTimestampStr].
     */
    fun tidOf(timestamp: String, offset: Int = 0, zone: ZoneId = ZoneId.systemDefault()): Long {
        val ldt = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT)
        val secondMillis = ldt.atZone(zone).toInstant().toEpochMilli()
        return secondMillis + offset
    }

    /**
     * Split a filename stem into `(secondResolutionTimestamp, collisionOffset)`.
     * The stem is the filename minus its extension(s), e.g. `2506-13-143022-f` or
     * `2506-13-143022-f_1`. The timestamp is the leading `\d{4}-\d{2}-\d{6}`; the
     * offset is the integer after a trailing `_N`, or 0 when absent.
     */
    fun parseStem(stem: String): Pair<String, Int> {
        val timestamp = STEM_TIMESTAMP.find(stem)?.value
            ?: error("No timestamp segment in stem: $stem")
        val offset = COLLISION_SUFFIX
            .find(stem)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 0
        return timestamp to offset
    }

    /**
     * Convenience: `tid` straight from a filename stem (timestamp + parsed offset).
     * Use [parseStem] when the timestamp is needed separately.
     */
    fun tidOfStem(stem: String, zone: ZoneId = ZoneId.systemDefault()): Long {
        val (timestamp, offset) = parseStem(stem)
        return tidOf(timestamp, offset, zone)
    }

    /**
     * Parse a comment filename stem `{targetTs}-n-{nowTs}[_offset]` into its two
     * timestamps and the comment's own collision offset. The **first** timestamp
     * segment is the owner's targetTs; the **second** is the comment's own nowTs
     * (which carries the optional `_N` suffix). Returns null if the stem is not a
     * two-timestamp comment name.
     */
    fun parseCommentStem(stem: String, zone: ZoneId = ZoneId.systemDefault()): CommentTimestamps? {
        val matches = STEM_TIMESTAMP.findAll(stem).toList()
        if (matches.size < 2) return null
        val targetTs = matches[0].value
        val ownTs = matches[1].value
        val offset = COLLISION_SUFFIX
            .find(stem.substringAfter(ownTs))
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 0
        return CommentTimestamps(
            ownTid = tidOf(ownTs, offset, zone),
            targetTs = targetTs,
        )
    }

    /** A comment's own tid plus its owner's targetTs (owner offset resolved separately). */
    data class CommentTimestamps(
        val ownTid: Long,
        val targetTs: String
    )

    private val STEM_TIMESTAMP = Regex("""\d{4}-\d{2}-\d{6}""")
}
