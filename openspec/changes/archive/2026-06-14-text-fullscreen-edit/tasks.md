# Implementation Tasks

## 1. Storage & Repository — save-back

- [x] 1.1 Add `FileStorageEngine.overwriteText(file: File, content: String)` that writes plaintext to `file` (handles the plaintext path directly).
- [x] 1.2 Add `MindDumpRepository.saveEntryEdit(entry: MindDumpEntry, newText: String)`: off-main-thread; for encrypted entries decrypt is already cached for display — write plaintext to working file, re-encrypt to original `.enc` path, delete working file; for plaintext write in place; then `dao.deleteByPath` + `insert` refreshed entity (FTS content + new lastModified).
- [x] 1.3 Handle missing `sessionPassword` on the encrypted path: surface a sentinel/error result instead of throwing, so the UI can toast and keep the editor open.
- [x] 1.4 Add `MindDumpRepository.loadEntryText(entry): String` that returns plaintext (decrypting `.enc` to cache first) for editor pre-fill, off main thread.

## 2. ViewModel — edit wiring

- [x] 2.1 Add `entryToEdit: File?` to `MindDumpUiState` and `openEntryForEdit(file: File)` / `clearEntryToEdit()` accessors.
- [x] 2.2 Add `ViewModel.saveEntryEdit(entry, newText)` that calls the repository, then triggers the normal post-save refresh (entries + groups) so the feed reorders.
- [x] 2.3 Add `ViewModel.loadEntryText(entry): String` passthrough for the editor's initial load.

## 3. Navigation — edit-mode route

- [x] 3.1 Extend `Screen.FullscreenEdit` route to accept an optional `entryPath` query arg (`fullscreen_edit?entryPath={...}`); add the `argument` to the `composable` and Uri-decode it.
- [x] 3.2 In `MindDumpNavGraph`, resolve the entry from `entryPath` against `uiState.entries` (fall back to a `File` if not in current entries) and pass it (or null) into `FullscreenEditScreen` as the edit target.
- [x] 3.3 Add `onNavigateToFullscreenEdit(entryPath: String?)` variant so the dispatch can navigate into edit mode; keep the existing no-arg navigation for new-entry mode.

## 4. Unified dispatch — `onEntryOpen`

- [x] 4.1 Add `onEntryOpen(context, entry, onTextEdit: (File) -> Unit)` helper: `TEXT` or `COMMENT` → `onTextEdit(entry.file)` (navigate edit mode); else → `openFile(context, entry.file)`.
- [x] 4.2 Replace `MainScreen` `onEntryClick` body to call `onEntryOpen`, routing text/comments to edit-mode navigation and others to `openFile`.
- [x] 4.3 Replace `GroupDetailScreen` member `onClick` to call `onEntryOpen`.
- [x] 4.4 Confirm `CommentBubble` `onCommentClick` reaches `onEntryOpen` (it flows through `onEntryClick`/`onEntryOpen`) and that comments open in edit mode.

## 5. Fullscreen editor — edit mode

- [x] 5.1 Refactor `FullscreenEditScreen` to take a sealed mode param (new-entry vs. edit-existing with target file) while keeping the shared top-bar + text-field layout.
- [x] 5.2 In edit mode, hold text in screen-local `remember { mutableStateOf("") }`; load initial content via `LaunchedEffect`/`produceState` on `Dispatchers.IO` using `ViewModel.loadEntryText`, showing the filename as placeholder while loading.
- [x] 5.3 Wire the top-bar send button in edit mode to `onSave` (calls `ViewModel.saveEntryEdit` then closes).
- [x] 5.4 Track dirty state (`current != initial`); on close (button) show the unsaved-changes sheet only when dirty.
- [x] 5.5 Add `BackHandler` in edit mode that runs the same dirty-check + sheet as the close button.

## 6. Unsaved-changes drawer

- [x] 6.1 Create a `ModalBottomSheet` "保留这次编辑吗？" with three actions: **保存**, **丢弃**, **继续编辑**, styled to match `EntryActionDrawer`/`DeleteConfirmDialog`.
- [x] 6.2 Wire **保存** → `onSave` then close; **丢弃** → close without saving; **继续编辑** → dismiss sheet.
- [x] 6.3 On save failure (locked encrypted entry), show a toast "需要解锁才能保存", keep the editor open, and dismiss the sheet.

## 7. Verification

- [x] 7.1 Build passes: `assembleDebug` ✓, `ktlintCheck` ✓, `detekt` clean for this change (sole remaining detekt failure — `EntryActionDrawer` LongParameterList — is pre-existing on HEAD in an untouched file, out of scope).
- [ ] 7.2 Manual: tap a plaintext TEXT entry → editor pre-fills → edit → save → entry overwrites and floats to feed top; content searchable.
- [ ] 7.3 Manual: tap an encrypted TEXT entry → decrypts → edit → save → re-encrypted; relaunch Private space still decrypts.
- [ ] 7.4 Manual: edit then close → "保留这次编辑吗？" sheet → each of 保存/丢弃/继续编辑 behaves per spec; back button triggers the same sheet.
- [ ] 7.5 Manual: tap photo/audio/video entry → still opens external viewer; tap a comment → opens editor.
- [ ] 7.6 Manual: save an encrypted entry after locking the session → error toast, editor stays open, edits retained.

> 7.2–7.6 require a device/emulator run; deferred to manual QA.
