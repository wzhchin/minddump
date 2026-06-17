## Why

MindDump's delete is destructive and irreversible. `FileStorageEngine.deleteEntry()` calls `file.delete()` directly and the repository drops the Room row; multi-select batch delete (`deleteSelectedEntries`) repeats this per entry. A single mis-tap — or a multi-select slip — permanently destroys a record with no recovery, and the ROADMAP explicitly names this as the one safety-net the app itself must provide (backup and edit-history are delegated to git, but git cannot rescue an uncommitted delete). This change converts delete into a recoverable operation by moving entries into a `.trash/` holding area with a retention window before permanent deletion.

## What Changes

- **Soft delete replaces hard delete.** Deleting a file entry or group moves it from its live location to `.trash/{Public,Private}/...` instead of calling `file.delete()`. The original relative path (space / month / group nesting / filename) is preserved so the entry can be restored verbatim.
- **`.trash/` is invisible to the live feed.** `scanEntries` / `scanChildGroups` / `reconcileWithDisk` skip the `.trash` directory, so trashed entries disappear from the UI and leave the Room index the moment they are trashed. The filesystem remains the single source of truth: trashed files are real files in a real directory, recoverable without any side store.
- **Restore.** A trashed entry can be moved back to its original path, reappearing in the feed.
- **Retention + purge.** Trashed entries older than a fixed retention window (30 days) are permanently deleted. Purging runs opportunistically during the existing disk reconcile/startup path in this batch (no `WorkManager` dependency yet — that arrives with the notifications batch #7/#8). Manual "empty trash" and per-entry "delete forever" are also exposed.
- **Trash UI.** An entry point (from Settings) opens a Trash list showing trashed items with their trashed date, with per-item Restore and Delete-forever, plus Empty-trash. Because trashed content may be encrypted, the trash list shows metadata only (name, type, date) and never decrypts for display.
- **Batch delete is safe too.** `deleteSelectedEntries` routes through the same trash path.

## Capabilities

### New Capabilities
- `recycle-bin`: soft delete (move to `.trash/`), restore, retention-based purge, manual empty/forever-delete, and the trash list UI.

### Modified Capabilities
- `entry-actions-drawer`: the delete action now trashes instead of destroying; copy/labels adjust to the recoverable semantics.
- `database-rebuild`: the rebuild scan continues to ignore `.trash/`; trashed files are explicitly excluded, never re-indexed.

## Impact

- **Storage**: `FileStorageEngine.kt` — `deleteEntry` becomes `trashEntry` (rename into `.trash/` preserving relative path); new `restoreEntry`, `purgeExpired`, `emptyTrash`, `deleteEntryForever`, and `listTrashed`. The scan/reconcile roots must skip `.trash`.
- **Repository**: `MindDumpRepository.kt` — `deleteEntry` delegates to trashing; new restore/purge/empty/forever methods; reconciling trashed items out of Room.
- **Data model**: no new columns needed for the file's own identity; the original location is recoverable from the preserved relative path. Retention age is derived from file last-modified in `.trash` (set at trash time), so no schema change is required. No Room migration.
- **ViewModel/UI**: `MindDumpViewModel` gains trash state + actions; a new `TrashScreen`/list; Settings entry point. New strings (zh-CN + en).
- **Existing data**: none touched; `.trash/` simply does not exist yet and is created lazily.
