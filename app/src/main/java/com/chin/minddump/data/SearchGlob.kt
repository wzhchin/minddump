package com.chin.minddump.data

/**
 * Builds a SQLite GLOB substring pattern from a user search query.
 *
 * Search matches entries by substring via GLOB on the raw `contentPreview`
 * column. Because GLOB treats `*`, `?`, and `[` as wildcards, any such
 * characters the user typed must be escaped to character classes (`[*]`,
 * `[?]`, `[[]`) so they match literally. The result is wrapped in `*…*`
 * to turn it into a "contains" test.
 *
 * Example: `天气` → `*天气*`, `a*b` → `*a[*]b*`, `100%` → `*100%*`.
 */
object SearchGlob {
    private val globMeta = charArrayOf('*', '?', '[')

    /**
     * Returns the GLOB pattern for [query], or `null` if [query] is blank
     * (callers should short-circuit rather than run an all-matching `**` scan).
     */
    fun toPattern(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val escaped = buildString(trimmed.length + 2) {
            append('*')
            for (ch in trimmed) {
                if (ch in globMeta) {
                    append('[')
                    append(ch)
                    append(']')
                } else {
                    append(ch)
                }
            }
            append('*')
        }
        return escaped
    }
}
