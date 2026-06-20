package com.chin.minddump.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.ZoneId

/**
 * Reproduces the reconcile-time comment→owner tid resolution that
 * [com.chin.minddump.data.MindDumpRepository] performs, using only the pure
 * [Tid] helpers. Guards against a regression where a comment's targetTid fails
 * to match its owner's tid (which would render comments as tag-less orphan
 * cards instead of nesting them under their file).
 */
class CommentTargetResolutionTest {
    private val zone = ZoneId.systemDefault()

    @Test
    fun `comment targetTs resolves to the owner file tid`() {
        // Owner file: 2506-13-143022-f.md
        val ownerTid = Tid.tidOfStem("2506-13-143022-f", zone)
        // Comment naming: {targetTs}-n-{nowTs}.md — nowTs is later the same day.
        val commentParsed = Tid.parseCommentStem("2506-13-143022-n-2506-13-150000", zone)
        val targetTs = commentParsed?.targetTs
        val ownerTimestamp = MindDumpEntry(
            file = java.io.File("/x/2506-13-143022-f.md"),
            type = EntryType.TEXT,
            space = Space.PUBLIC,
            monthFolder = "2025-06",
            tid = ownerTid,
        ).timestamp

        // The owner's recovered timestamp must equal the comment's encoded targetTs.
        assertEquals(targetTs, ownerTimestamp)
        // So grouping by timestamp resolves the comment's targetTid to the owner tid.
        assertEquals(ownerTid, Tid.tidOf(targetTs!!, offset = 0, zone))
        assertNotEquals(ownerTid, commentParsed.ownTid)
    }

    @Test
    fun `collision-offset owner still matches a comment's base targetTs`() {
        // Owner file created same-second: 2506-13-143022-f_1.md (offset 1)
        val ownerTid = Tid.tidOfStem("2506-13-143022-f_1", zone)
        val commentParsed = Tid.parseCommentStem("2506-13-143022-n-2506-13-150000", zone)

        // Owner's timestamp (recovered from tid) drops the offset → base ts,
        // which still equals the comment's targetTs. The reconcile resolution
        // picks the first owner at that ts; this test documents that behaviour.
        val ownerTimestamp = MindDumpEntry(
            file = java.io.File("/x/2506-13-143022-f_1.md"),
            type = EntryType.TEXT,
            space = Space.PUBLIC,
            monthFolder = "2025-06",
            tid = ownerTid,
        ).timestamp
        assertEquals(commentParsed?.targetTs, ownerTimestamp)
    }
}
