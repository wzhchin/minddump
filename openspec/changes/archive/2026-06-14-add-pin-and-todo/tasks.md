# Tasks — add-pin-and-todo

## 1. Parsing core (`storage/`)

- [ ] 1.1 Add `TodoState` enum to `storage` package (`NONE, TODO, DOING, WAIT, DONE, CANCEL` with `code: String?`).
- [ ] 1.2 Rewrite `FileMetadata.FILE_PATTERN` and `DIR_PATTERN` to the new schema (capture pin prefix + status token). Update the doc comment naming convention block.
- [ ] 1.3 Add `isPinned: Boolean` and `todoState: TodoState` to `FileMetadata`; populate from regex groups in `parseFile`/`parseDirectory`. Default `false`/`NONE` when slots absent.
- [ ] 1.4 Verify (unit-test mentally or with a quick test) the non-collision cases: `9999-2506-13-143022-TODO-f.md`, `2506-13-143022-f-TODO-list.pdf` (status NONE, name "TODO-list"), `2506-13-143022-n-2506-13-150000.md` (comment: pin false, NONE), encrypted variants.

## 2. Data model

- [ ] 2.1 Add `isPinned: Boolean = false` and `todoState: TodoState = TodoState.NONE` to `MindDumpEntry`.
- [ ] 2.2 Add the same two columns to `EntryEntity`; update `toEntry`/`toEntity` mappers.
- [ ] 2.3 Bump `MindDumpDatabase` version 3 → 4. Confirm `fallbackToDestructiveMigration(dropAllTables = true)` in `StorageModule` handles it (no migration code needed).

## 3. File operations (`FileStorageEngine`)

- [ ] 3.1 Add `setPinned(file, pinned)` and `setStatus(file, state)` for files: parse via `FileMetadata`, reassemble name from fields, `renameTo`, `check`. Preserve encryption + originalName.
- [ ] 3.2 Add `setGroupPinned(dir, pinned)` and `setGroupStatus(dir, state)` for group directories (mirror `renameGroupDir`).
- [ ] 3.3 Change `scanEntries` final sort from `sortedByDescending { it.file.lastModified() }` to `sortedByDescending { it.file.name }`.
- [ ] 3.4 Change `scanChildGroups` (and any group-list ordering path) to `sortedByDescending { it.name }`.
- [ ] 3.5 Populate `isPinned`/`todoState` on the `MindDumpEntry` built in `scanEntries` from the parsed `FileMetadata`.
- [ ] 3.6 Leave all `save*`/`get*`/`createGroup`/`saveComment` builders emitting pin-less, status-less default names (verify, no change expected).

## 4. Repository wiring

- [ ] 4.1 In `MindDumpRepository` scan/reconcile, ensure `isPinned`/`todoState` flow from `MindDumpEntry` into `EntryEntity` rows.
- [ ] 4.2 Confirm comment association still joins on the parent's real timestamp (extracted post-pin-strip); add/adjust a parse helper if association currently string-matches the full filename.
- [ ] 4.3 Add repository methods to pin/unpin and set status (delegating to `FileStorageEngine`), updating the Room row's path + fields after the rename.

## 5. UI — cards

- [ ] 5.1 Entry card: render pin icon when `isPinned`, status badge when `todoState != NONE`, in the header.
- [ ] 5.2 Apply a de-emphasized treatment for `DONE`/`CANCEL` (muted chip / strikethrough).
- [ ] 5.3 Group card: same pin indicator + status badge in the header.

## 6. UI — actions

- [ ] 6.1 Entry actions drawer: add "置顶"/"取消置顶" toggle (hide for comments).
- [ ] 6.2 Entry actions drawer: add "待办状态" → status chooser (5 states + clear; hide for comments).
- [ ] 6.3 Group action affordance: pin toggle + status chooser (mirror entry drawer).
- [ ] 6.4 After any pin/status op, refresh the feed/groups so ordering and badges update.

## 7. Strings

- [ ] 7.1 Add zh-CN strings to `res/values/strings.xml`: `pin`, `unpin`, `todo_status`, `status_todo`, `status_doing`, `status_wait`, `status_done`, `status_cancel`, `status_clear`.
- [ ] 7.2 Add matching en strings to `res/values-en/strings.xml`.

## 8. Verify

- [ ] 8.1 `./gradlew detekt ktlintCheck` clean.
- [ ] 8.2 `./gradlew assembleRelease` builds.
- [ ] 8.3 Manual sanity: pin an entry → floats to top; unpin → drops back; set TODO → badge; clear → gone; comment under a pinned parent still associates.
