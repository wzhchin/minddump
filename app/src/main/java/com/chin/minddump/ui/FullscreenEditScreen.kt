package com.chin.minddump.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.components.UnsavedEditSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fullscreen editor serving two modes:
 *  - **new entry** ([editEntry] == null): bound to the ViewModel's input text flow;
 *    submit creates a new entry (unchanged behavior).
 *  - **edit existing** ([editEntry] != null): screen-local state seeded from the
 *    entry's file; save overwrites the original file.
 *
 * In edit mode, closing with unsaved changes shows [UnsavedEditSheet] so a discard
 * is always deliberate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenEditScreen(
    viewModel: MindDumpViewModel,
    editEntry: MindDumpEntry?,
    newEntryText: String,
    onNewEntryTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmitNewEntry: () -> Unit,
    onSavedEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditMode = editEntry != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Edit-mode state: loaded once from disk (decrypted if needed), held locally so
    // it never touches the new-entry inputText flow.
    var loadedText by rememberSaveable(editEntry) { mutableStateOf("") }
    var loaded by rememberSaveable(editEntry) { mutableStateOf(false) }
    var editDraft by rememberSaveable(editEntry) { mutableStateOf("") }
    var showUnsavedSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editEntry) {
        if (isEditMode && !loaded) {
            val text = withContext(Dispatchers.IO) { viewModel.loadEntryText(editEntry!!) }
            loadedText = text
            editDraft = text
            loaded = true
        }
    }

    val isDirty = isEditMode && loaded && editDraft != loadedText
    val fieldText = if (isEditMode) editDraft else newEntryText
    val fieldPlaceholder = if (isEditMode && !loaded) "加载中…" else stringResource(R.string.input_placeholder)
    val canSubmit = if (isEditMode) loaded && editDraft.isNotBlank() else newEntryText.isNotBlank()

    val closeWithGuard = {
        if (isDirty) showUnsavedSheet = true else onClose()
    }

    val doSaveEdit = saveEdit@{
        val entry = editEntry ?: return@saveEdit
        scope.launch {
            val ok = viewModel.saveEntryEdit(entry, editDraft)
            if (ok) {
                onSavedEdit()
            } else {
                // Encrypted session locked — keep the editor open so edits aren't lost.
                showUnsavedSheet = false
                Toast.makeText(context, "需要解锁才能保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = true) { closeWithGuard() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorTopBar(
                canSubmit = canSubmit,
                onClose = closeWithGuard,
                onSubmit = { if (isEditMode) doSaveEdit() else onSubmitNewEntry() },
            )
            OutlinedTextField(
                value = fieldText,
                onValueChange = if (isEditMode) ({ editDraft = it }) else onNewEntryTextChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .imePadding(),
                enabled = !isEditMode || loaded,
                placeholder = { Text(fieldPlaceholder) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
        }
    }

    // Auto-focus and show keyboard (new-entry mode; edit waits until loaded).
    LaunchedEffect(isEditMode, loaded) {
        if (!isEditMode || loaded) focusRequester.requestFocus()
    }

    if (showUnsavedSheet) {
        UnsavedEditSheet(
            onDismiss = { showUnsavedSheet = false },
            onSave = {
                showUnsavedSheet = false
                doSaveEdit()
            },
            onDiscard = {
                showUnsavedSheet = false
                onClose()
            },
            onKeepEditing = { showUnsavedSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    canSubmit: Boolean,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            FilledIconButton(
                onClick = onSubmit,
                enabled = canSubmit,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
