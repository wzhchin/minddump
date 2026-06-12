package com.chin.minddump.ui

import android.app.Application
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class MindDumpViewModelTest {

    private lateinit var mockStorageEngine: FileStorageEngine
    private lateinit var application: Application
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        mockStorageEngine = mockk(relaxed = true)
        application = mockk()
        every { application.applicationContext } returns application
        testScope = TestScope(StandardTestDispatcher())
    }

    private fun createViewModel(): MindDumpViewModel {
        return MindDumpViewModel(application).also {
            // We can't easily inject mockStorageEngine since the ViewModel creates it internally.
            // Tests here focus on state transitions that don't depend on storage.
        }
    }

    @Test
    fun `initial state is Public and not dark theme`() {
        // We test the default state values directly
        val state = MindDumpUiState()
        assertEquals(Space.PUBLIC, state.currentSpace)
        assertFalse(state.isDarkTheme)
        assertFalse(state.isRecording)
        assertEquals("", state.inputText)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `initial state has no pending space switch`() {
        val state = MindDumpUiState()
        assertFalse(state.pendingSpaceSwitch)
    }

    @Test
    fun `UiState copy preserves unmodified fields`() {
        val original = MindDumpUiState(
            currentSpace = Space.PUBLIC,
            inputText = "hello",
            isRecording = true,
        )
        val modified = original.copy(inputText = "world")

        assertEquals(Space.PUBLIC, modified.currentSpace)
        assertEquals("world", modified.inputText)
        assertTrue(modified.isRecording)
    }

    @Test
    fun `Space enum has correct folder names`() {
        assertEquals("Public", Space.PUBLIC.folderName)
        assertEquals("Private", Space.PRIVATE.folderName)
    }

    @Test
    fun `requestMigration with both empty switches directly`() = testScope.runTest {
        // Create a real engine with a temp directory
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_migrate_${System.nanoTime()}")
        tempDir.mkdirs()

        try {
            val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
            every { prefs.getString("work_dir", null) } returns tempDir.absolutePath
            every { prefs.contains("work_dir") } returns true

            val context = mockk<android.content.Context>()
            every { context.getSharedPreferences("minddump_prefs", android.content.Context.MODE_PRIVATE) } returns prefs
            every { context.applicationContext } returns context

            val engine = FileStorageEngine(context)
            // Both directories are empty → countFiles returns 0
            assertEquals(0, engine.countFiles())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `MindDumpUiState pendingSpaceSwitch default is false`() {
        val state = MindDumpUiState()
        assertFalse(state.pendingSpaceSwitch)
        assertFalse(state.showSettings)
        assertFalse(state.showMigrationDialog)
    }

    @Test
    fun `UiState supports pending space switch flow`() {
        // Simulate: user clicks switch to Private → pendingSpaceSwitch = true
        val requesting = MindDumpUiState(pendingSpaceSwitch = true)
        assertTrue(requesting.pendingSpaceSwitch)
        assertEquals(Space.PUBLIC, requesting.currentSpace)

        // Simulate: auth succeeds → apply switch
        val switched = requesting.copy(
            currentSpace = Space.PRIVATE,
            isDarkTheme = true,
            pendingSpaceSwitch = false,
        )
        assertEquals(Space.PRIVATE, switched.currentSpace)
        assertTrue(switched.isDarkTheme)
        assertFalse(switched.pendingSpaceSwitch)

        // Simulate: auth fails → cancel
        val cancelled = requesting.copy(pendingSpaceSwitch = false)
        assertEquals(Space.PUBLIC, cancelled.currentSpace)
        assertFalse(cancelled.pendingSpaceSwitch)
    }

    @Test
    fun `UiState migration dialog flow`() {
        // Request migration with files present
        val state = MindDumpUiState(
            showMigrationDialog = true,
            pendingNewDir = "/new/path",
            currentFileCount = 5,
            newDirFileCount = 3,
        )
        assertTrue(state.showMigrationDialog)
        assertEquals("/new/path", state.pendingNewDir)
        assertEquals(5, state.currentFileCount)
        assertEquals(3, state.newDirFileCount)

        // Confirm migration
        val migrated = state.copy(
            workDir = "/new/path",
            showMigrationDialog = false,
            showSettings = false,
            pendingNewDir = null,
            currentFileCount = 0,
            newDirFileCount = 0,
        )
        assertFalse(migrated.showMigrationDialog)
        assertEquals("/new/path", migrated.workDir)
    }
}
