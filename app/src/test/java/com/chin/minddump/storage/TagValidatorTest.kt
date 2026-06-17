package com.chin.minddump.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagValidatorTest {
    @Test
    fun acceptsLettersDigitsCjkAndHyphen() {
        assertEquals("idea", TagValidator.normalize("idea"))
        assertEquals("项目A", TagValidator.normalize("项目A"))
        assertEquals("v2-launch", TagValidator.normalize("v2-launch"))
    }

    @Test
    fun stripsLeadingHash() {
        assertEquals("idea", TagValidator.normalize("#idea"))
    }

    @Test
    fun rejectsSpacesHashAndSlash() {
        assertNull(TagValidator.normalize("two words"))
        assertNull(TagValidator.normalize("a/b"))
        assertNull(TagValidator.normalize("##double"))
    }

    @Test
    fun addUniqueDedupsCaseInsensitivelyKeepingFirstCasing() {
        val result = TagValidator.addUnique(listOf("Idea"), "idea")
        assertEquals(listOf("Idea"), result)
    }

    @Test
    fun addUniqueAppendsNewTag() {
        val result = TagValidator.addUnique(listOf("a"), "b")
        assertTrue(result.contains("b"))
        assertEquals(2, result.size)
    }

    @Test
    fun removeMatchesCaseInsensitively() {
        val result = TagValidator.remove(listOf("Idea", "b"), "idea")
        assertEquals(listOf("b"), result)
    }
}
