package com.chin.minddump.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchGlobTest {
    @Test
    fun `cjk phrase wrapped as substring pattern`() {
        assertEquals("*天气*", SearchGlob.toPattern("天气"))
    }

    @Test
    fun `longer cjk phrase preserved`() {
        assertEquals("*北京旅行*", SearchGlob.toPattern("北京旅行"))
    }

    @Test
    fun `percent is a literal not a wildcard`() {
        // % is not a GLOB metacharacter, so it passes through untouched.
        assertEquals("*100%*", SearchGlob.toPattern("100%"))
    }

    @Test
    fun `asterisk is escaped`() {
        assertEquals("*a[*]b*", SearchGlob.toPattern("a*b"))
    }

    @Test
    fun `question mark is escaped`() {
        assertEquals("*a[?]b*", SearchGlob.toPattern("a?b"))
    }

    @Test
    fun `open bracket is escaped`() {
        assertEquals("*a[[]b*", SearchGlob.toPattern("a[b"))
    }

    @Test
    fun `mixed cjk and latin`() {
        assertEquals("*用 Kotlin 写代码*", SearchGlob.toPattern("用 Kotlin 写代码"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("*天气*", SearchGlob.toPattern("  天气  "))
    }

    @Test
    fun `blank query yields null`() {
        assertNull(SearchGlob.toPattern(""))
        assertNull(SearchGlob.toPattern("   "))
    }
}
