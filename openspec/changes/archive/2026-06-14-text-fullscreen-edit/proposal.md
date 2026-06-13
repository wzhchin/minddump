## Why

Tapping a text entry or comment currently launches an external editor via `Intent.ACTION_VIEW`, throwing the user out of the app mid-thought. MindDump is built around frictionless capture, so reading/editing your own text should happen in-app. This removes the context switch and lets users refine existing thoughts without leaving the capture flow.

## What Changes

- Tapping a `TEXT` entry or a comment (`EntryRole.COMMENT`) opens an in-app fullscreen editor instead of an external app.
- Non-text entries (photos, audio, video, generic files) keep the existing `openFile()` → external viewer behavior.
- A unified `onEntryOpen(entry)` dispatch point routes the tap by type/role, replacing the three scattered `openFile` calls (main list, group detail, comment bubble).
- The fullscreen editor gains an **edit-existing** mode: initial text loaded from the entry's file, edits held in screen-local state (decoupled from the new-entry `uiState.inputText` flow, which stays unchanged).
- Saving an edited entry writes back to the original file (overwrite); `file.lastModified` updates naturally, so the entry floats to the top of the feed (feed sorts by `lastModified`).
- On back-press with unsaved changes, a "保留这次编辑吗？" drawer offers **保存** / **丢弃** / **继续编辑**. No changes → closes directly. This replaces persistent draft storage with a one-shot confirmation.
- The top-bar send button in edit mode means **save**.

## Capabilities

### New Capabilities
- `text-entry-editing`: In-app fullscreen editing of existing text entries and comments — click dispatch by type/role, edit-existing editor mode, overwrite-save-back, and the unsaved-changes confirmation drawer.

### Modified Capabilities
<!-- None. Click-to-open dispatch is not described by any existing spec; this change adds it. Existing entry-actions-drawer (long-press), group-list-loading, and file-naming-format behaviors are unaffected. -->

## Impact

- **ui/MindDumpNavGraph.kt** — add an edit-mode route (e.g. `fullscreen_edit?entryPath={...}`) carrying the target file path.
- **ui/FullscreenEditScreen.kt** — support edit-existing mode (initial text from file, `onSave`, dirty-state tracking, unsaved-changes drawer). New-entry mode unchanged.
- **ui/MainScreen.kt** — replace `onEntryClick`'s direct `openFile` with `onEntryOpen(entry)` dispatch.
- **ui/GroupDetailScreen.kt** — member `onClick` routes through `onEntryOpen`.
- **ui/EntryItem.kt** — comment-bubble `onCommentClick` flows through the same dispatch.
- **ui/MindDumpViewModel.kt** — add an edit-target state / callback (e.g. `entryToEdit: File?`) plus a save-back function.
- **storage/FileStorageEngine.kt** — add an overwrite-write method to write edited content back to the original entry file.
- **storage/CryptoEngine interaction** — when the target entry is encrypted (`.enc`), save-back must re-encrypt before writing (Private space).
- No schema/DB migration: save-back reuses the existing file + Room index row (timestamps re-indexed on the existing entry, not a new row).
