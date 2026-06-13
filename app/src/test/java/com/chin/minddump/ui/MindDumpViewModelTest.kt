package com.chin.minddump.ui

import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.storage.Space
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MindDumpViewModelTest {

    private lateinit var mockRepository: MindDumpRepository
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        every { mockRepository.getEntries(any()) } returns flowOf(emptyList())
        every { mockRepository.getRootDirPath() } returns "/sdcard/MindDump"
        every { mockRepository.hasStoragePermission() } returns true
        every { mockRepository.isWorkDirConfigured() } returns true
        every { mockRepository.hasPrivatePassword() } returns false
        every { mockRepository.isSessionUnlocked() } returns false
        coEvery { mockRepository.refreshFromDisk(any()) } returns Unit
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun createViewModel(): MindDumpViewModel = MindDumpViewModel(mockRepository)

    @Test
    fun `initial state is Public and not dark theme`() {
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
        val original =
            MindDumpUiState(
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
    fun `MindDumpUiState pendingSpaceSwitch default is false`() {
        val state = MindDumpUiState()
        assertFalse(state.pendingSpaceSwitch)
        assertFalse(state.showSettings)
        assertFalse(state.showMigrationDialog)
    }

    @Test
    fun `UiState supports pending space switch flow`() {
        val requesting = MindDumpUiState(pendingSpaceSwitch = true)
        assertTrue(requesting.pendingSpaceSwitch)
        assertEquals(Space.PUBLIC, requesting.currentSpace)

        val switched =
            requesting.copy(
                currentSpace = Space.PRIVATE,
                isDarkTheme = true,
                pendingSpaceSwitch = false,
            )
        assertEquals(Space.PRIVATE, switched.currentSpace)
        assertTrue(switched.isDarkTheme)
        assertFalse(switched.pendingSpaceSwitch)

        val cancelled = requesting.copy(pendingSpaceSwitch = false)
        assertEquals(Space.PUBLIC, cancelled.currentSpace)
        assertFalse(cancelled.pendingSpaceSwitch)
    }

    @Test
    fun `UiState migration dialog flow`() {
        val state =
            MindDumpUiState(
                showMigrationDialog = true,
                pendingNewDir = "/new/path",
                currentFileCount = 5,
                newDirFileCount = 3,
            )
        assertTrue(state.showMigrationDialog)
        assertEquals("/new/path", state.pendingNewDir)
        assertEquals(5, state.currentFileCount)
        assertEquals(3, state.newDirFileCount)

        val migrated =
            state.copy(
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

    @Test
    fun `requestSpaceSwitch to Private without password shows setup`() {
        val vm = createViewModel()
        vm.requestSpaceSwitch()
        assertTrue(vm.uiState.value.showPasswordSetup)
        assertFalse(vm.uiState.value.showPasswordInput)
        assertEquals(Space.PUBLIC, vm.uiState.value.currentSpace)
    }

    @Test
    fun `requestSpaceSwitch to Private with existing password shows input`() {
        every { mockRepository.hasPrivatePassword() } returns true
        val vm = createViewModel()
        vm.requestSpaceSwitch()
        assertFalse(vm.uiState.value.showPasswordSetup)
        assertTrue(vm.uiState.value.showPasswordInput)
        assertEquals(Space.PUBLIC, vm.uiState.value.currentSpace)
    }

    @Test
    fun `requestSpaceSwitch when already unlocked goes directly`() {
        every { mockRepository.isSessionUnlocked() } returns true
        val vm = createViewModel()
        vm.requestSpaceSwitch()
        assertEquals(Space.PRIVATE, vm.uiState.value.currentSpace)
        assertTrue(vm.uiState.value.isDarkTheme)
    }

    @Test
    fun `setPassword and switch to Private`() {
        val vm = createViewModel()
        vm.requestSpaceSwitch()
        assertTrue(vm.uiState.value.showPasswordSetup)
        vm.setPassword("test1234")
        assertEquals(Space.PRIVATE, vm.uiState.value.currentSpace)
        assertFalse(vm.uiState.value.showPasswordSetup)
    }

    @Test
    fun `cancelPasswordDialog resets state`() {
        val vm = createViewModel()
        vm.requestSpaceSwitch()
        assertTrue(vm.uiState.value.showPasswordSetup)
        vm.cancelPasswordDialog()
        assertFalse(vm.uiState.value.showPasswordSetup)
        assertEquals(Space.PUBLIC, vm.uiState.value.currentSpace)
    }
}
