## 1. Storage Layer — Group Scanning

- [x] 1.1 Add `scanGroups(space: Space): List<File>` to `FileStorageEngine` — scan the current month directory for subdirectories matching the `*-g*` naming pattern (reuse `FileMetadata.fromFile` to verify `EntryRole.GROUP`).

## 2. Repository Layer — Combined Create+Move

- [x] 2.1 Add `createAndMoveToGroup(entry: MindDumpEntry, space: Space, name: String?)` to `MindDumpRepository` — call `createGroup(space, name)` then `moveToGroup(entry, groupDir)` in sequence.

## 3. ViewModel — Group State

- [x] 3.1 Add a `groups` field to the `UiState` data class in `MindDumpViewModel.kt` (implemented as `List<GroupSummary>` — each carries `groupDir` + `memberEntries` so cards can show counts/type chips; the drawer receives `uiState.groups.map { it.groupDir }` as `List<File>`).
- [x] 3.2 Populate groups via a dedicated `refreshGroups()` (called from `entriesFlow.collect` and when the picker opens via `LaunchedEffect`), rather than only inside `refreshEntries()` — this fixes the "暂无分组" bug where groups were never loaded on launch or space switch.
- [x] 3.3 Add `createAndMoveToGroup(entry: MindDumpEntry, name: String?)` method to the ViewModel — call `repository.createAndMoveToGroup(entry, currentSpace, name)` then `refreshEntries()`.

## 4. UI — Wire Group Picker

- [x] 4.1 In `MainScreen.kt`, replace `groups = emptyList()` with `groups = uiState.groups` in the `EntryActionDrawer` call.
- [x] 4.2 Update `onCreateGroup` callback to call `viewModel.createAndMoveToGroup(entry, name)` instead of `viewModel.createGroup(name)`.

## 5. Verification

- [x] 5.1 Build and verify: create an entry, open action drawer, tap "移动到组", create a new group — entry file should physically move into the new group directory. *(Verified at build level: `./gradlew :app:assembleDebug` succeeds; unit tests pass. Manual on-device confirmation pending.)*
- [x] 5.2 Verify: existing group directories appear in the picker when re-opening it after group creation. *(Verified at build level; manual on-device confirmation pending.)*

## Note: Scope expansion during implementation

This change's planned scope (wire the picker + create-and-move) was implemented, but the same session also expanded the group feature well beyond the original proposal to address additional bugs the user reported ("group 工作不正常，包括渲染等"):

- **Group summary cards in the main list** (`GroupSummaryCard`): group renders as 📁 card with name + count + type chips; members no longer show flat. Time-ordered feed mixing group cards and loose entries.
- **`GroupDetail` nav route**: tapping a group card opens a screen showing only that group's members.
- **Group rename / dissolve** (`renameGroup`, `dissolveGroup` in repository + `renameGroupDir`/`dissolveGroup` in `FileStorageEngine`): long-press a group card → action sheet. Dissolve = move all members back to month dir + delete empty dir.
- **Multi-select merge**: multi-select hides the bottom input bar, adds a top bar with "合并为分组" (reuses `GroupPickerSheet` for new-or-existing group); `moveEntriesToGroup` / `createAndMoveGroupForEntries`.
- **Move out of group**: detail view's entry drawer gains a "移出该组" action.

These additions were not part of this change's proposal/spec and should be reflected in updated specs (e.g. an expanded `group-list-loading` or a new spec) before archival, or tracked as a follow-up change.
