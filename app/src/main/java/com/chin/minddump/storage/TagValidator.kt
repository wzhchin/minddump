package com.chin.minddump.storage

/**
 * Validation and normalization for entry tags.
 *
 * A tag may contain letters (Latin and CJK), digits, and hyphens. Spaces, the
 * `#` character, and `/` are forbidden. Tags are stored without a leading `#`;
 * the UI prepends `#` only for display. Deduplication is case-insensitive —
 * `Idea` and `idea` are the same tag — but the first-added casing is preserved.
 */
object TagValidator {
    // Latin letters, digits, CJK Unified Ideographs (一-鿿), and hyphen.
    private val ALLOWED = Regex("""^[A-Za-z0-9一-鿿-]+$""")

    fun isValid(tag: String): Boolean {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return false
        return ALLOWED.matches(trimmed)
    }

    /**
     * Normalize a raw tag (trim, strip a leading `#` if the user typed one).
     * Returns null when the result is invalid.
     */
    fun normalize(raw: String): String? {
        var t = raw.trim()
        if (t.startsWith("#")) t = t.removePrefix("#").trim()
        return if (isValid(t)) t else null
    }

    /**
     * Append [tag] to [existing], deduplicating case-insensitively while keeping
     * the first-seen casing. Returns the same list if [tag] is already present
     * (by case-insensitive match) or invalid.
     */
    fun addUnique(existing: List<String>, tag: String): List<String> {
        val normalized = normalize(tag) ?: return existing
        if (existing.any { it.equals(normalized, ignoreCase = true) }) return existing
        return existing + normalized
    }

    /**
     * Remove [tag] from [existing], matching case-insensitively.
     */
    fun remove(existing: List<String>, tag: String): List<String> =
        existing.filterNot { it.equals(tag.trim(), ignoreCase = true) }
}
