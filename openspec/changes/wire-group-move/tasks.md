## 1. Storage Layer — Group Scanning

- [x] 1.1 Add `scanGroups(space: Space): List<File>` to `FileStorageEngine` — scan the current month directory for subdirectories matching the `*-g*` naming pattern (reuse `FileMetadata.fromFile` to verify `EntryRole.GROUP`).

## 2. Repository Layer — Combined Create+Move

- [x] 2.1 Add `createAndMoveToGroup(entry: MindDumpEntry, space: Space, name: String?)` to `MindDumpRepository` — call `createGroup(space, name)` then `moveToGroup(entry, groupDir)` in sequence.

## 3. ViewModel — Group State

- [x] 3.1 Add `groups: List<File> = emptyList()` field to the `UiState` data class in `MindDumpViewModel.kt`.
- [x] 3.2 In `refreshEntries()`, call `repository.scanGroups(currentSpace)` and update `_uiState` with the result.
- [x] 3.3 Add `createAndMoveToGroup(entry: MindDumpEntry, name: String?)` method to the ViewModel — call `repository.createAndMoveToGroup(entry, currentSpace, name)` then `refreshEntries()`.

## 4. UI — Wire Group Picker

- [x] 4.1 In `MainScreen.kt`, replace `groups = emptyList()` with `groups = uiState.groups` in the `EntryActionDrawer` call.
- [x] 4.2 Update `onCreateGroup` callback to call `viewModel.createAndMoveToGroup(entry, name)` instead of `viewModel.createGroup(name)`.

## 5. Verification

- [ ] 5.1 Build and verify: create an entry, open action drawer, tap "移动到组", create a new group — entry file should physically move into the new group directory.
- [ ] 5.2 Verify: existing group directories appear in the picker when re-opening it after group creation.
