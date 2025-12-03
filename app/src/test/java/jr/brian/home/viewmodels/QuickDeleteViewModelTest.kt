package jr.brian.home.viewmodels

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import jr.brian.home.data.QuickDeleteManager
import jr.brian.home.model.FileExtensionInfo
import jr.brian.home.model.state.DeleteResult
import jr.brian.home.model.state.QuickDeleteUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickDeleteViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: QuickDeleteViewModel
    private lateinit var mockContext: Context
    private lateinit var mockQuickDeleteManager: QuickDeleteManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockQuickDeleteManager = mockk(relaxed = true)

        // Default behavior: return empty flow
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(emptySet())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): QuickDeleteViewModel {
        return QuickDeleteViewModel(mockContext, mockQuickDeleteManager)
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // Given/When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(emptyList<String>(), state.folderPaths)
            assertEquals(emptyList<FileExtensionInfo>(), state.fileExtensions)
            assertEquals(emptySet<String>(), state.selectedExtensions)
            assertFalse(state.isScanning)
            assertFalse(state.isDeleting)
            assertFalse(state.showDeleteConfirmation)
            assertNull(state.scanError)
            assertNull(state.deleteResult)
        }
    }

    @Test
    fun `loadFolderPaths updates state with folder paths from manager`() = runTest {
        // Given
        val testPaths = setOf("/path/one", "/path/two", "/path/three")
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(testPaths)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testPaths.toList(), state.folderPaths)
        }
    }

    @Test
    fun `addFolderPath calls manager and persists path`() = runTest {
        // Given
        viewModel = createViewModel()
        val path = "content://test/path"
        coEvery { mockQuickDeleteManager.addFolderPath(path) } returns Unit

        // When
        viewModel.addFolderPath(path)
        advanceUntilIdle()

        // Then
        coVerify { mockQuickDeleteManager.addFolderPath(path) }
    }

    @Test
    fun `addFolderPath ignores blank strings`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.addFolderPath("")
        viewModel.addFolderPath("   ")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { mockQuickDeleteManager.addFolderPath(any()) }
    }

    @Test
    fun `removeFolderPath calls manager`() = runTest {
        // Given
        val testPaths = setOf("/path/one")
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(testPaths)
        viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { mockQuickDeleteManager.removeFolderPath("/path/one") } returns Unit

        // When
        viewModel.removeFolderPath("/path/one")
        advanceUntilIdle()

        // Then
        coVerify { mockQuickDeleteManager.removeFolderPath("/path/one") }
    }

    @Test
    fun `removeFolderPath clears scan results`() = runTest {
        // Given
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(setOf("/path/one"))
        viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { mockQuickDeleteManager.removeFolderPath("/path/one") } returns Unit

        // When
        viewModel.removeFolderPath("/path/one")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.fileExtensions.isEmpty())
            assertTrue(state.selectedExtensions.isEmpty())
        }
    }

    @Test
    fun `scanFolders does nothing when no folder paths exist`() = runTest {
        // Given
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(emptySet())
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.scanFolders()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isScanning)
            assertTrue(state.fileExtensions.isEmpty())
        }
    }

    @Test
    fun `toggleExtensionSelection adds extension when not selected`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleExtensionSelection("txt")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.selectedExtensions.contains("txt"))
        }
    }

    @Test
    fun `toggleExtensionSelection removes extension when already selected`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleExtensionSelection("txt")
        advanceUntilIdle()

        // When
        viewModel.toggleExtensionSelection("txt")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.selectedExtensions.contains("txt"))
        }
    }

    @Test
    fun `deselectAllExtensions clears all selections`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleExtensionSelection("txt")
        viewModel.toggleExtensionSelection("jpg")
        advanceUntilIdle()

        // When
        viewModel.deselectAllExtensions()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.selectedExtensions.isEmpty())
        }
    }

    @Test
    fun `showDeleteConfirmation sets showDeleteConfirmation to true`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleExtensionSelection("txt")
        advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showDeleteConfirmation)
        }
    }

    @Test
    fun `showDeleteConfirmation does nothing when no extensions selected`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showDeleteConfirmation)
        }
    }

    @Test
    fun `hideDeleteConfirmation sets showDeleteConfirmation to false`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleExtensionSelection("txt")
        advanceUntilIdle()
        viewModel.showDeleteConfirmation()
        advanceUntilIdle()

        // When
        viewModel.hideDeleteConfirmation()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showDeleteConfirmation)
        }
    }

    @Test
    fun `deleteSelectedFiles does nothing when no extensions selected`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteSelectedFiles()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isDeleting)
            assertNull(state.deleteResult)
        }
    }

    @Test
    fun `clearDeleteResult sets deleteResult to null`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearDeleteResult()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.deleteResult)
        }
    }

    @Test
    fun `clearScanError sets scanError to null`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearScanError()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.scanError)
        }
    }

    @Test
    fun `QuickDeleteUIState has correct default values`() {
        // When
        val state = QuickDeleteUIState()

        // Then
        assertEquals(emptyList<String>(), state.folderPaths)
        assertEquals(emptyList<FileExtensionInfo>(), state.fileExtensions)
        assertEquals(emptySet<String>(), state.selectedExtensions)
        assertFalse(state.isScanning)
        assertFalse(state.isDeleting)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.scanError)
        assertNull(state.deleteResult)
    }

    @Test
    fun `QuickDeleteUIState can be copied with new values`() {
        // Given
        val originalState = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = false
        )

        // When
        val newState = originalState.copy(isScanning = true)

        // Then
        assertFalse(originalState.isScanning)
        assertTrue(newState.isScanning)
        assertEquals(originalState.folderPaths, newState.folderPaths)
    }

    @Test
    fun `DeleteResult Success contains correct data`() {
        // When
        val result = DeleteResult.Success(42)

        // Then
        assertEquals(42, result.deletedCount)
    }

    @Test
    fun `DeleteResult Failure contains correct message`() {
        // When
        val result = DeleteResult.Failure("Test error message")

        // Then
        assertEquals("Test error message", result.message)
    }

    @Test
    fun `multiple toggleExtensionSelection calls work correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleExtensionSelection("txt")
        viewModel.toggleExtensionSelection("jpg")
        viewModel.toggleExtensionSelection("png")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.selectedExtensions.size)
            assertTrue(state.selectedExtensions.containsAll(listOf("txt", "jpg", "png")))
        }
    }

    @Test
    fun `QuickDeleteUIState equality works correctly`() {
        // Given
        val state1 = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = false
        )
        val state2 = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = false
        )
        val state3 = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = true
        )

        // Then
        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    @Test
    fun `QuickDeleteUIState with different folder paths are not equal`() {
        // Given
        val state1 = QuickDeleteUIState(folderPaths = listOf("/path/one"))
        val state2 = QuickDeleteUIState(folderPaths = listOf("/path/two"))

        // Then
        assertTrue(state1 != state2)
    }

    @Test
    fun `QuickDeleteUIState hashCode is consistent`() {
        // Given
        val state = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = true
        )

        // When
        val hash1 = state.hashCode()
        val hash2 = state.hashCode()

        // Then
        assertEquals(hash1, hash2)
    }

    @Test
    fun `QuickDeleteUIState hashCode is consistent with equals`() {
        // Given
        val state1 = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = true
        )
        val state2 = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = true
        )

        // Then
        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun `initial value is accessible immediately`() {
        // When
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(emptySet())
        viewModel = createViewModel()
        val initialValue = viewModel.uiState.value

        // Then
        assertFalse(initialValue.isScanning)
        assertTrue(initialValue.folderPaths.isEmpty())
    }

    @Test
    fun `ViewModel can be instantiated`() {
        // Given/When
        val vm = QuickDeleteViewModel(mockContext, mockQuickDeleteManager)

        // Then
        assertFalse(vm.uiState.value.isScanning)
        assertTrue(vm.uiState.value.folderPaths.isEmpty())
    }

    @Test
    fun `ViewModel state flow emits initial state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When/Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isScanning)
            assertTrue(initialState.folderPaths.isEmpty())
            assertTrue(initialState.fileExtensions.isEmpty())
        }
    }

    @Test
    fun `multiple ViewModels can be created independently`() {
        // Given
        val mockManager1 = mockk<QuickDeleteManager>(relaxed = true)
        val mockManager2 = mockk<QuickDeleteManager>(relaxed = true)
        coEvery { mockManager1.folderPaths } returns flowOf(emptySet())
        coEvery { mockManager2.folderPaths } returns flowOf(emptySet())

        // When
        val vm1 = QuickDeleteViewModel(mockContext, mockManager1)
        val vm2 = QuickDeleteViewModel(mockContext, mockManager2)

        // Then
        assertTrue(vm1 !== vm2) // Different instances
        assertEquals(vm1.uiState.value.isScanning, vm2.uiState.value.isScanning)
    }

    @Test
    fun `QuickDeleteUIState default constructor creates valid empty state`() {
        // When
        val state = QuickDeleteUIState()

        // Then
        assertTrue(state.folderPaths.isEmpty())
        assertTrue(state.fileExtensions.isEmpty())
        assertTrue(state.selectedExtensions.isEmpty())
        assertFalse(state.isScanning)
        assertFalse(state.isDeleting)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.scanError)
        assertNull(state.deleteResult)
    }

    @Test
    fun `QuickDeleteUIState toString contains all properties`() {
        // Given
        val state = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            isScanning = true
        )

        // When
        val stringRepresentation = state.toString()

        // Then
        assertTrue(stringRepresentation.contains("folderPaths"))
        assertTrue(stringRepresentation.contains("isScanning"))
    }

    @Test
    fun `FileExtensionInfo can be created with correct properties`() {
        // When
        val fileInfo = FileExtensionInfo(
            extension = "txt",
            fileCount = 5,
            totalSize = 10240L,
            filePaths = listOf("/file1.txt", "/file2.txt")
        )

        // Then
        assertEquals("txt", fileInfo.extension)
        assertEquals(5, fileInfo.fileCount)
        assertEquals(10240L, fileInfo.totalSize)
        assertEquals(2, fileInfo.filePaths.size)
    }

    @Test
    fun `DeleteResult Success can be pattern matched`() {
        // When
        val result: DeleteResult = DeleteResult.Success(10)

        // Then
        when (result) {
            is DeleteResult.Success -> assertEquals(10, result.deletedCount)
            is DeleteResult.Failure -> throw AssertionError("Should be Success")
        }
    }

    @Test
    fun `DeleteResult Failure can be pattern matched`() {
        // When
        val result: DeleteResult = DeleteResult.Failure("Error occurred")

        // Then
        when (result) {
            is DeleteResult.Success -> throw AssertionError("Should be Failure")
            is DeleteResult.Failure -> assertEquals("Error occurred", result.message)
        }
    }

    @Test
    fun `folder paths flow is collected on init`() = runTest {
        // Given
        val paths = setOf("/path/one", "/path/two")
        coEvery { mockQuickDeleteManager.folderPaths } returns flowOf(paths)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals(paths.toList(), viewModel.uiState.value.folderPaths)
    }

    @Test
    fun `addFolderPath with valid path is processed`() = runTest {
        // Given
        viewModel = createViewModel()
        val validPath = "content://com.android.externalstorage/tree/primary"
        coEvery { mockQuickDeleteManager.addFolderPath(validPath) } returns Unit

        // When
        viewModel.addFolderPath(validPath)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { mockQuickDeleteManager.addFolderPath(validPath) }
    }

    @Test
    fun `selectAllExtensions when file extensions exist`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Manually set some file extensions in the state for testing
        // Since we can't easily mock the scanning, we'll test the selection logic
        val testExtensions = listOf(
            FileExtensionInfo("txt", 5, 1024L),
            FileExtensionInfo("jpg", 3, 2048L)
        )

        // We can't directly set the state, but we can test that selectAll works
        // with the current state (which will be empty)
        viewModel.selectAllExtensions()
        advanceUntilIdle()

        // Then - with empty fileExtensions, selectedExtensions should be empty
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(emptySet<String>(), state.selectedExtensions)
        }
    }

    @Test
    fun `QuickDeleteUIState copy preserves unchanged values`() {
        // Given
        val fileInfo = FileExtensionInfo("txt", 5, 1024L)
        val original = QuickDeleteUIState(
            folderPaths = listOf("/path/one"),
            fileExtensions = listOf(fileInfo),
            selectedExtensions = setOf("txt"),
            isScanning = false
        )

        // When
        val copied = original.copy(isScanning = true)

        // Then
        assertEquals(listOf("/path/one"), copied.folderPaths)
        assertEquals(listOf(fileInfo), copied.fileExtensions)
        assertEquals(setOf("txt"), copied.selectedExtensions)
        assertTrue(copied.isScanning)
        assertFalse(original.isScanning)
    }
}
