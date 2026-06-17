# Tasks — add-recycle-bin

## 1. Storage — trash primitives (`FileStorageEngine`)

- [x] 1.1 Add a `TRASH_DIR_NAME = ".trash"` constant and a `trashRoot(): File` helper returning `File(getRootDir(), ".trash")`.
- [x] 1.2 Add `relPathUnderSpace(file, space): String` — the file's path relative to its space root (`Public/...` stripped to the part under the space dir). Used to build both the trash target and the restore source.
- [x] 1.3 Implement `trashEntry(entry)`: compute the preserved relative path under the space root, create the mirror directory under `.trash/<space>/...`, `renameTo` the file there (mtime refreshes), `check` on failure. Return the trashed `File`.
- [x] 1.4 Implement `trashGroup(groupDir)`: move the whole group directory tree into `.trash/<space>/<relPath>` via `renameTo`. Return the trashed dir.
- [x] 1.5 Implement `restoreEntry(trashedFile)`: move back to `rootDir/<space>/<relPath>`; on target collision, append `_1`/`_2`… to the filename (reuse the existing collision convention). Return the restored `File`.
- [x] 1.6 Implement `deleteEntryForever(trashedFile)`: `delete()` for a file, `deleteRecursively()` for a dir. Return Boolean.
- [x] 1.7 Implement `emptyTrash()`: delete the whole `.trash/` tree.
- [x] 1.8 Implement `purgeExpired(retentionDays = 30)`: walk `.trash/`, delete any file whose `lastModified()` is older than the window; delete a group dir recursively if it (or all members) is expired. Guard `!exists()`.
- [x] 1.9 Implement `listTrashed(space): List<TrashedItem>`: scan `.trash/<space>/` recursively, map each file to a small data class (file, type from `FileMetadata`, trashedAt = `lastModified()`), newest-first. Skip un-parseable files gracefully.
- [x] 1.10 Add a `data class TrashedItem(val file: File, val type: EntryType, val trashedAt: Long, val space: Space)` (or reuse `MindDumpEntry` if it fits without forcing parse).
- [x] 1.11 Change `deleteEntry(entry)` to delegate to `trashEntry` (keep the method name for a minimal call-site diff, or rename and update callers — prefer rename for clarity).

## 2. Scan/reconcile exclusion

- [x] 2.1 Confirm `scanEntries`/`scanChildGroups` never descend into `.trash` (since `.trash` is a sibling of `Public`/`Private` under root, the per-space scans are inherently safe — verify, add an explicit guard/comment if a traversal could reach it).
- [x] 2.2 Audit `countFiles`, `migrateTo`, and any `walkTopDown`/`listFiles` over root: exclude `.trash` (and `.cache`) where the count or traversal is user-facing or copy-affecting. `migrateTo` must keep moving `.trash` (it lives in root) — verify it already does via `copyRecursively`.
- [x] 2.3 Add a comment block documenting the `.trash/` (and `.cache/`) exclusion contract at the scan entry points.

## 3. Repository wiring (`MindDumpRepository`)

- [x] 3.1 `deleteEntry` → trashes via storage engine and removes the Room row (behavior: entry leaves the feed immediately, row gone).
- [x] 3.2 Add `deleteGroup(groupDir, space)` trash path (if group delete existed as hard delete; otherwise add the trash path for multi-select/group delete).
- [x] 3.3 `restoreEntry(trashedFile)`: move back, then reconcile the space so the row returns.
- [x] 3.4 `deleteForever(trashedFile)`: storage delete-forever (no Room row to touch — already removed at trash time).
- [x] 3.5 `emptyTrash()`: storage empty.
- [x] 3.6 `purgeExpiredTrash()`: delegate to storage.
- [x] 3.7 `listTrashed(space)`: delegate to storage.
- [x] 3.8 Call `purgeExpiredTrash()` at the top of `reconcileWithDisk` (best-effort, log failures) and expose it for startup invocation.

## 4. ViewModel (`MindDumpViewModel`)

- [x] 4.1 Add UI state for the trash screen: `showTrash: Boolean`, `trashedItems: List<TrashedItem>`, `trashSpaceFilter: Space`.
- [x] 4.2 Add `openTrash()`, `closeTrash()`, `refreshTrash()`, `restoreTrashed(item)`, `deleteTrashedForever(item)`, `emptyTrash()` — all on `Dispatchers.IO`, refreshing trash list and live feed after.
- [x] 4.3 Ensure startup path triggers `purgeExpiredTrash()` (extend the existing init reconcile) so expired items are cleaned when the app opens.
- [x] 4.4 `deleteSelectedEntries` continues to call the (now soft) delete path — verify it routes through trash, not a leftover hard delete.

## 5. UI — trash screen + entry points

- [x] 5.1 New `TrashScreen` composable: list grouped/filtered by space, rows show type icon + filename + "trashed N days ago"; row actions 恢复 / 永久删除; top-bar 清空回收站 (with confirm dialog); empty state.
- [x] 5.2 Wire `TrashScreen` into the nav graph and add a Settings entry point "回收站" (string `trash`).
- [x] 5.3 Adjust `DeleteConfirmDialog`/delete copy: shift from irreversible framing to soft-delete (mention recoverable from 回收站); keep a confirm step for 永久删除 and 清空.
- [x] 5.4 After restore/empty, refresh the live feed so the change is visible on return.

## 6. Strings

- [x] 6.1 zh-CN `res/values/strings.xml`: `trash`, `trash_empty`, `trash_empty_message`, `restore`, `delete_forever`, `delete_forever_confirm`, `empty_trash`, `empty_trash_confirm`, `trashed_n_days_ago` (format), updated `delete_message`/`delete_title` framing.
- [x] 6.2 Matching en `res/values-en/strings.xml`.

## 7. Tests

- [x] 7.1 `FileStorageEngineTest`: trash moves file out of scan, restore returns it, purge removes expired (use `setLastModified` to age), empty clears all, forever deletes one, list returns expected items sorted.
- [x] 7.2 ViewModel test (mock repo): restore/empty/forever call the right repo methods and refresh.

## 8. Verify

- [x] 8.1 `./gradlew detekt ktlintCheck` clean.
- [x] 8.2 `./gradlew assembleRelease` builds.
- [x] 8.3 Manual: delete an entry → gone from feed; open 回收站 → see it; 恢复 → back in feed; 永久删除 → gone; 清空 → all gone; encrypted entry trashes/restores still encrypted.
