package com.chin.minddump.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ──────────────────────────────────────────────
// Friendly local date-time formatting
// ──────────────────────────────────────────────

/**
 * Format a [LocalDateTime] as a compact, user-facing local string anchored to
 * the calendar date — "今天 HH:mm", "昨天 HH:mm", "M月d日 HH:mm" (same year), or
 * "yyyy年M月d日 HH:mm" (other years).
 *
 * Unlike [EntryItem.formatRelativeTimestamp], this is calendar-date-anchored and
 * carries no "刚刚 / N分钟前" duration branches, so it is correct for arbitrary
 * future points (e.g. a reminder due time) as well as past ones. Localized to
 * zh-CN by construction; the app's primary locale is zh-CN.
 */
fun formatFriendlyDateTime(
    dateTime: LocalDateTime,
    today: LocalDate = LocalDate.now(),
): String {
    val date = dateTime.toLocalDate()
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dayMonth = dateTime.format(DateTimeFormatter.ofPattern("M月d日"))
    val fullDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
    return when {
        date == today -> "今天 $time"
        date == today.minusDays(1) -> "昨天 $time"
        date.year == today.year -> "$dayMonth $time"
        else -> "$fullDate $time"
    }
}
