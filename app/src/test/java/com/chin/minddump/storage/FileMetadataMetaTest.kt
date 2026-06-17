package com.chin.minddump.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileMetadataMetaTest {
    @Test
    fun parsesPlaintextMetaSidecar() {
        val meta = FileMetadata.fromFile(dummy("2506-13-143022-m.yaml"))
        assertEquals(EntryRole.META, meta?.role)
        assertEquals("2506-13-143022", meta?.timestamp)
        assertFalse(meta?.isEncrypted ?: true)
    }

    @Test
    fun parsesEncryptedMetaSidecar() {
        val meta = FileMetadata.fromFile(dummy("2506-13-143022-m.yaml.enc"))
        assertEquals(EntryRole.META, meta?.role)
        assertEquals("2506-13-143022", meta?.timestamp)
        assertTrue(meta?.isEncrypted ?: false)
    }

    @Test
    fun metaNeverCarriesOriginalName() {
        val meta = FileMetadata.fromFile(dummy("2506-13-143022-m.yaml"))
        assertEquals(null, meta?.originalName)
    }

    private fun dummy(name: String) =
        java.io.File(System.getProperty("java.io.tmpdir"), name)
}
