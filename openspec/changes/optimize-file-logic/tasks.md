## 1. FileMetadata & Naming Format

- [x] 1.1 Create `FileMetadata` data class in `storage/` with fields: `timestamp` (yymm-dd-HHMMSS), `role` (enum: FILE, COMMENT, GROUP), `originalName` (nullable), `extension`, `isEncrypted`, derived `entryType` (from extension). Factory: `FileMetadata.fromFile(file: File): FileMetadata?` returning null for non-MindDump files
- [x] 1.2 Create `EntryRole` enum: `F` (file), `N` (comment), `G` (group directory)
- [x] 1.3 Refactor `EntryType` companion — replace `fromFileName()` with extension-based detection: `.md`→TEXT, `.m4a/.aac`→RECORDING, `.jpg/.jpeg/.png`→PHOTO, `.mp4`→VIDEO, else→FILE. Remove all prefix-based logic
- [x] 1.4 Update `FileStorageEngine` timestamp generation from `HHmmssSSS` to `yymm-dd-HHMMSS`
- [x] 1.5 Update `FileStorageEngine.saveTextEntry()` to produce `{ts}-f.md`
- [x] 1.6 Update `FileStorageEngine.getRecordingFile()` to produce `{ts}-f.m4a`
- [x] 1.7 Update `FileStorageEngine.getPhotoFile()` to produce `{ts}-f.jpg`
- [x] 1.8 Update `FileStorageEngine.getVideoFile()` to produce `{ts}-f.mp4`
- [x] 1.9 Update `FileStorageEngine.importFile()` to produce `{ts}-f-{originalName}.{ext}`
- [x] 1.10 Update directory generation from `YYYY-MM-DD` to `YYYY-MM` (monthly grouping)
- [x] 1.11 Refactor `FileStorageEngine.scanEntries()` to use `FileMetadata.fromFile()`, skip non-MindDump files

## 2. Room Schema Update

- [x] 2.1 Add `role` column (String, non-null, default "f") to `EntryEntity`
- [x] 2.2 Add `targetTimestamp` column (String, nullable) to `EntryEntity` — for comments, stores the target's timestamp prefix
- [x] 2.3 Add `groupPath` column (String, nullable) to `EntryEntity` — stores the group directory path for files inside groups
- [x] 2.4 Update `EntryFts` to include `role` if needed for search filtering
- [x] 2.5 Bump `MindDumpDatabase` version to 3 with `fallbackToDestructiveMigration()` (old data discarded per spec)
- [x] 2.6 Update `EntryEntity.toEntry()` and `MindDumpEntry.toEntity()` mappers for new fields
- [x] 2.7 Update `MindDumpEntry` data class — add `role: EntryRole`, `targetTimestamp: String?`, `groupPath: String?`, rename `dateFolder` to `monthFolder` reflecting `YYYY-MM`
- [x] 2.8 Update `EntryDao` queries — replace `dateFolder` references with `monthFolder`, add `role`-based queries for comments/groups

## 3. Group & Comment File Operations

- [x] 3.1 Add `FileStorageEngine.createGroup(space: Space, name: String?): File` — creates `{ts}-g[-{name}]/` directory under current month
- [x] 3.2 Add `FileStorageEngine.moveToGroup(file: File, groupDir: File): File` — physically moves file into group directory
- [x] 3.3 Add `FileStorageEngine.moveOutOfGroup(file: File, space: Space): File` — moves file from group directory back to parent month directory
- [x] 3.4 Add `FileStorageEngine.saveComment(space: Space, targetTimestamp: String, content: String): File` — creates `{targetTs}-n-{ts}.md` in same directory as target
- [x] 3.5 Add `FileStorageEngine.scanGroups(space: Space): List<File>` — scans for `-g` directories in all month folders
- [x] 3.6 Update `FileStorageEngine.scanEntries()` to recursively scan group directories and populate `groupPath` on entries inside them

## 4. Repository Layer Refactor

- [x] 4.1 Update `MindDumpRepository.saveTextEntry()` — use new naming, use `FileMetadata.fromFile()` for entity creation
- [x] 4.2 Update `MindDumpRepository.registerMediaFile()` — use `FileMetadata.fromFile()`
- [x] 4.3 Update `MindDumpRepository.importFile()` — use new naming and `FileMetadata.fromFile()`
- [x] 4.4 Add `MindDumpRepository.saveComment(space, targetTimestamp, content)` — save comment file + insert Room row with role=N, targetTimestamp set
- [x] 4.5 Add `MindDumpRepository.createGroup(space, name?)` — create group dir + scan for group entry
- [x] 4.6 Add `MindDumpRepository.moveToGroup(entry, groupDir)` — move file + update Room row's groupPath
- [x] 4.7 Add `MindDumpRepository.moveOutOfGroup(entry, space)` — move file out + clear groupPath
- [x] 4.8 Add `MindDumpRepository.moveEntryToSpace(entry, targetSpace)` — move file between Public/Private, handle encrypt/decrypt
- [x] 4.9 Add `MindDumpRepository.renameEntry(entry, newName)` — rename file's originalName portion + update Room
- [x] 4.10 Update `MindDumpRepository.reconcileWithDisk()` — derive `role`, `targetTimestamp`, `groupPath` from filesystem using `FileMetadata`

## 5. UI — Entry Actions Drawer

- [x] 5.1 Create `EntryActionDrawer` composable — ModalBottomSheet with action list: Delete, Rename, Multi-select, Move to group, Move to Public/Private
- [x] 5.2 Add `RenameDialog` composable — text field for editing originalName
- [x] 5.3 Add `GroupPickerSheet` composable — list of existing groups + "Create new group" option with name input
- [x] 5.4 Update `MindDumpViewModel` — add state for `selectedEntryForAction`, methods: `renameEntry()`, `moveToGroup()`, `moveToSpace()`, `createGroup()`
- [x] 5.5 Update `MainScreen.kt` — replace `DeleteConfirmDialog` with `EntryActionDrawer` on long-press, wire up all action callbacks

## 6. UI — Multi-Select Mode

- [x] 6.1 Add `isMultiSelectMode: Boolean` and `selectedEntries: Set<MindDumpEntry>` to `MindDumpUiState`
- [x] 6.2 Add multi-select toggle/exit methods to ViewModel
- [x] 6.3 Add top action bar composable for multi-select mode (bulk delete, bulk move)
- [x] 6.4 Update `EntryItem` — show checkbox when in multi-select mode

## 7. UI — Comments & Groups Display

- [x] 7.1 Update `MindDumpUiState` / ViewModel to group entries — main entries with their comments nested
- [x] 7.2 Update entry bubble — render comments as indented sub-bubbles within the parent entry
- [x] 7.3 Add orphan comment rendering — standalone bubble with "target missing" indicator
- [x] 7.4 Update `EntryItem` click handler — open comment input for adding new comments (long-term; may defer)

## 8. Verification

- [x] 8.1 Build app — confirm no compile errors
- [x] 8.2 Run `./gradlew detekt ktlint` — fix lint issues (14 errors all pre-existing, not from this change)
- [ ] 8.3 Test: save text entry → verify file appears as `{ts}-f.md` in `YYYY-MM/` folder
- [ ] 8.4 Test: import file → verify `{ts}-f-originalName.ext` naming
- [ ] 8.5 Test: capture photo/video/audio → verify naming and playback
- [ ] 8.6 Test: create group → verify directory created, files movable in/out
- [ ] 8.7 Test: add comment → verify `{targetTs}-n-{ts}.md` created, displayed under parent
- [ ] 8.8 Test: long-press drawer → verify all actions work (delete, rename, move to group, move space)
- [ ] 8.9 Test: reconciliation → delete file from disk manually, trigger reconcile, verify Room updated
