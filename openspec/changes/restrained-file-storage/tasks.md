## 1. Storage layer — identity, parsing, sidecar pairing

- [x] 1.1 Delete `app/src/main/java/com/chin/minddump/storage/Tid.kt` entirely (`tidOf`, `parseStem`, `tidOfStem`, `parseCommentStem`, `CommentTimestamps`, `STEM_TIMESTAMP`, collision-offset logic).
- [x] 1.2 In `MindDumpEntry.kt`: remove the `tid` field and `commentTargetTs`/`targetTid` fields. Identity is carried by the `file` path (store the path relative to `workDir` where the entry is materialized).
- [x] 1.3 In `FileMetadata.kt`: collapse the `role` enum to drop `COMMENT` and `GROUP`. Keep `FILE`; groups are now detected by `File.isDirectory`, not by a `g` role token. Add sidecar recognition: a file whose name ends with `.meta` (optionally `.meta.enc`) parses as a sidecar, not as an owner entry.
- [x] 1.4 In `FileMetadata.kt`: remove the comment filename grammar (`{targetTs}-n-{nowTs}`) and any `COMMENT` parse branch. The `f` role token remains the sole file role.
- [x] 1.5 In `FileStorageEngine.kt`: change `sidecarFileFor(owner, encrypted)` to return `File(owner.parentFile, owner.name + if (encrypted) ".meta.enc" else ".meta")`. Remove the `{ts}-m.yaml` construction. Keep `writeSidecarText`/`readSidecarText` as thin wrappers over the new pairing.
- [x] 1.6 Add `FileStorageEngine.renameEntry(old: File, newName: String): File` — the single rename entry point that renames owner then its `.meta` sidecar together (group variant uses a temp-dir dance so members travel atomically). Validate target uniqueness in `old.parentFile` first.
- [x] 1.7 Funnel all existing `renameTo` call sites in `FileStorageEngine.kt` (pin/unpin, status change, slug edit, group rename, dissolve, trash/restore ~9 sites) through `renameEntry()` so every owner rename carries its sidecar. Delete direct `renameTo` usages for owner files.
- [x] 1.8 In `FileStorageEngine.scanEntries()`: stop parsing `n` comment files; skip them (they remain on disk, un-indexed). Stop recursing for nested sub-groups — a group directory is entered once and only its files are collected.
- [x] 1.9 In `FileStorageEngine.dissolveGroup()`: remove recursion; move members up to the month-bucket level only (single-level groups have a month bucket, never another group, as parent), then delete the directory + its sidecar.
- [x] 1.10 Remove `FileStorageEngine.saveComment()` and any comment-specific directory helpers.

## 2. Room schema — path identity, drop comments, single-level groups

- [x] 2.1 In `EntryEntity.kt`: change `@PrimaryKey val tid: Long` → `@PrimaryKey val filePath: String` (path relative to `workDir`). Drop the `tid` column.
- [x] 2.2 In `EntryEntity.kt`: replace `parentId: Long?` with `groupPath: String?` (relative path of the owning group dir, or null at the month-bucket root). Update the `parentId` index to index `groupPath`.
- [x] 2.3 Delete `CommentEntity` and its `toEntry()` mapper from `EntryEntity.kt`. Remove the `comments` table.
- [x] 2.4 In `EntryEntity.kt`: change `TagEntity` primary key from `["tid","tag"]` to `["filePath","tag"]`; change its `ForeignKey` to reference `EntryEntity.filePath` (`parentColumns = ["filePath"]`, `childColumns = ["filePath"]`). Update `toEntity`/mapper helpers.
- [x] 2.5 In `EntryEntity.kt`: change `EventEntity.tid: Long` → `filePath: String`; change its `ForeignKey` to reference `EntryEntity.filePath`. Update `toEventEntity`/`toEntryEvent` mappers.
- [x] 2.6 Update `EntryFts.kt` (FTS4 content entity) if its column set references `tid`; otherwise leave as-is (it shadows `contentPreview`/`monthFolder`).
- [x] 2.7 In `MindDumpDatabase.kt`: remove `CommentEntity` from the `@Database(entities=…)` list; bump `version` (6 → 7).
- [x] 2.8 In `di/StorageModule.kt`: confirm `fallbackToDestructiveMigration(dropAllTables = true)` remains the only migration strategy (no hand-written migration). No code change expected unless builder signature drifted.

## 3. DAO — re-key queries, drop comment queries

- [x] 3.1 In `EntryDao.kt`: re-key every entry query that filters by `tid` to filter by `filePath` (`getAll`, `getByType`, `getGroups`, `getMembers`, `search`, `deleteByPath`, `tidOfGroup`, `getByTag` JOIN, stats queries).
- [x] 3.2 In `EntryDao.kt`: change `tagsFor`/`distinctTags`/`getByTag` to key on `filePath`; change `tagsFor(tid)` → `tagsFor(filePath)`.
- [x] 3.3 In `EntryDao.kt`: change event queries (`eventsFor`, `pendingEvents`, `setEvents`) to key on `filePath`.
- [x] 3.4 In `EntryDao.kt`: delete all comment queries — `getCommentsSnapshot`, `getCommentsFor`, `insertComment`, `deleteCommentByPath`, `deleteCommentByTid`, `clearComments`. Remove `comments` references from `clearAll()`.

## 4. Repository — reconcile, save/load, drop comments + tid resolution

- [x] 4.1 In `MindDumpRepository.kt`: rewrite `reconcileWithDisk(space)` to drop the comment branch entirely (no `resolveCommentTargetTid`, no comment upsert, no `ownerTidByTs`). Upsert owners + their `filePath`-keyed tags/events; single-level `groupPath` from directory position.
- [x] 4.2 In `MindDumpRepository.kt`: drop the `mergeComments` step from `getEntries`; `getEntries(space)` now maps entities to entries with `tagsFor`/`eventsFor` keyed by `filePath`.
- [x] 4.3 In `MindDumpRepository.kt`: delete `saveComment`, `updateCommentRow`, `commentPreviewOf`, and any comment-rebuild helpers.
- [x] 4.4 In `MindDumpRepository.kt`: re-plumb all save/save-meta/pin/todo/move/dissolve paths to compute identity from `filePath` (relative to `workDir`) and to route renames through `storageEngine.renameEntry()` so sidecars travel.
- [x] 4.5 In `MindDumpRepository.kt`: ensure orphans — a `.meta` sidecar whose owner path is absent — are deleted during reconcile (the crash-mid-rename self-heal path). Owner content is never deleted by orphan handling.
- [x] 4.6 In `MindDumpRepository.kt`: add a `rootRelative(path)` helper that strips the `workDir` prefix so stored `filePath`/`groupPath` are workDir-relative (migration/move-safe). Apply at every insert.

## 5. UI / ViewModel — remove comment surfaces, drop tid references

- [x] 5.1 In `ui/MindDumpViewModel.kt`: delete `addComment` and any comment grouping (`groupEntriesWithComments`). Remove `_uiState` comment fields.
- [x] 5.2 In `ui/MindDumpViewModel.kt`: remove every `entry.tid` / `targetTid` read; re-plumb entry identification to the entry's file path.
- [x] 5.3 In `ui/MainScreen.kt`: remove the comment input/dialog wiring (`viewModel.addComment(...)` at ~line 583) and any comment-affordance UI.
- [x] 5.4 In `ui/components/EntryActionDrawer.kt`: remove `CommentDialog` (~line 670) and the comment action entry.
- [x] 5.5 In `ui/EntryItem.kt`: remove `CommentPreview` (~line 458) and the comment affordance on cards; cards show notes/groups only.
- [x] 5.6 In `ui/FeedFilter.kt` and any group/multi-select screens: remove sub-group creation UI inside a group page and multi-select-into-subgroup; groups can only be created at the month-top level. Remove nested-group drill UI if present.
- [x] 5.7 Audit remaining UI for `tid`/`parentId`/`EntryRole.COMMENT|GROUP|META` references and re-plumb to `filePath`/`groupPath`/isDirectory.

## 6. Scheduler — re-arm by path

- [x] 6.1 In the event scheduler (wherever `registerAllPublicEvents`/alarms are armed): identify owning entries by `filePath` instead of `tid`; the alarm re-registration key derives from `filePath`.
- [x] 6.2 Confirm Private-event lazy registration on unlock still keys on `filePath` and that no `tid` lookup remains.

## 7. Strings & localization

- [x] 7.1 In `res/values/strings.xml` (zh-CN) and `res/values-en/strings.xml` (en): remove comment-related strings (comment affordance, orphan-comment indicator, comment dialog). Keep group strings but update any "sub-group"/nested wording to single-level.
- [x] 7.2 Add or adjust any strings for single-level group messaging if new user-facing text is needed.

## 8. Verification

- [x] 8.1 `./gradlew detekt ktlintCheck` passes.
- [x] 8.2 `./gradlew assembleDebug` builds (no `tid`/`CommentEntity`/`EntryRole.GROUP` dangling references).
- [ ] 8.3 Manual: cold-launch reconcile indexes existing notes/groups by path; legacy `{targetTs}-n-…` and `…-m.yaml` sidecars are ignored (not shown, not crashing).
- [ ] 8.4 Manual: create a text note, add tags + a reminder, pin it, toggle todo — each rename keeps the note paired with its `.meta` sidecar (tags/reminder survive).
- [ ] 8.5 Manual: create a same-second note and group in the same month bucket — both index independently with distinct `filePath` keys (no overwrite).
- [ ] 8.6 Manual: dissolve a group — members move to the month bucket, group dir + sidecar deleted.
- [ ] 8.7 Manual: a Public-space reminder fires at its due time after a rebuild; notification deep-links to the entry.
- [ ] 8.8 Manual: move `workDir` (settings) — full rebuild re-roots all `filePath` identities consistently; no orphans, no data loss.
