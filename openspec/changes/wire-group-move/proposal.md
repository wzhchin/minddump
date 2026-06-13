## Why

The EntryActionDrawer offers "移动到组" (Move to Group) but the UI is not wired to the storage layer. `groups` is hardcoded to `emptyList()`, the ViewModel has no group-loading state, and `createGroup` creates a directory without moving the entry into it. Users see "暂无分组" even when groups exist on disk, and creating a new group strands the entry in its original location.

## What Changes

- Add `groups: List<File>` to the ViewModel `UiState` and populate it by scanning the current space's month directory for `*-g*` subdirectories during `refreshEntries()`.
- Replace `groups = emptyList()` in `MainScreen.kt` with `groups = uiState.groups`.
- Replace the disconnected `createGroup(name)` + `moveToGroup(entry, groupDir)` two-step with a single `createAndMoveToGroup(entry, name)` flow that creates the group directory and immediately moves the entry into it.
- Add `createAndMoveToGroup(entry, name)` to ViewModel and Repository layers.

## Capabilities

### New Capabilities
- `group-list-loading`: Scanning and exposing existing group directories from disk as ViewModel state, kept in sync with `refreshEntries()`.

### Modified Capabilities
<!-- No existing specs require requirement-level changes. -->

## Impact

- `MindDumpViewModel.kt` — new `groups` state field, new `createAndMoveToGroup()` method, `refreshEntries()` gains group scanning.
- `MindDumpRepository.kt` — new `createAndMoveToGroup()` method combining `createGroup` + `moveToGroup`.
- `MainScreen.kt` — wire `groups` from state, update `onCreateGroup` callback to use combined create+move.
- `EntryActionDrawer.kt` — no changes needed (already accepts `groups` and callbacks).
