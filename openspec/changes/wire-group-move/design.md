## Context

The storage layer (`FileStorageEngine`) and repository (`MindDumpRepository`) already implement all physical operations for groups: `createGroup`, `moveToGroup`, `moveOutOfGroup`. The ViewModel also exposes `moveToGroup()` and `createGroup()` methods. However, the UI layer is disconnected — `MainScreen` passes `groups = emptyList()` to `EntryActionDrawer`, and `createGroup` only creates an empty directory without moving the selected entry into it.

The ViewModel's `UiState` has no `groups` field, so existing group directories on disk are invisible to the picker UI.

## Goals / Non-Goals

**Goals:**
- Expose existing group directories as ViewModel state so the group picker shows them.
- Combine group creation and entry move into a single atomic operation from the user's perspective.
- Keep group list in sync with disk state via the existing `refreshEntries()` cycle.

**Non-Goals:**
- Cross-month group moves (groups are scoped to a single month directory).
- Group renaming or deletion (separate concern).
- Nested groups (groups are flat directories inside a month folder).

## Decisions

### 1. Scan groups from `FileStorageEngine` alongside `refreshEntries()`

**Decision**: Add a `scanGroups(space: Space): List<File>` method to `FileStorageEngine` that scans the current month directory for subdirectories matching the `*-g*` naming pattern. Call it from the ViewModel during `refreshEntries()` and store the result in `UiState.groups`.

**Rationale**: Groups are physical directories, not database records. Scanning the filesystem is the authoritative source. Doing it alongside `refreshEntries()` keeps the state consistent without adding a separate refresh cycle.

**Alternative considered**: Query the database for distinct `groupPath` values. Rejected because the database may be stale or missing groups created externally.

### 2. Combined `createAndMoveToGroup` method

**Decision**: Add `createAndMoveToGroup(entry, space, name)` to the Repository that calls `createGroup` then `moveToGroup` in sequence. The ViewModel exposes this as a single method.

**Rationale**: The user's mental model is "create a group and put this entry in it" — one action, not two. Having two separate calls risks the group being created but the entry not moved (e.g., if the app backgrounds mid-operation).

**Alternative considered**: Keep separate calls and chain them in the ViewModel. Rejected because error handling is simpler in the Repository layer, and the two operations are always paired in the UI.

### 3. Group list scoped to current month

**Decision**: Only show groups from the current space's current month directory. This matches the file listing scope already in `refreshEntries()`.

**Rationale**: Users interact with entries in a month-scoped view. Showing groups from other months would be confusing and require cross-month file moves.

## Risks / Trade-offs

- **[Filesystem scan cost]** → Negligible: scanning one month directory for subdirectories is fast (<10ms for typical usage).
- **[Race condition: group deleted between scan and move]** → `FileStorageEngine.moveToGroup` already checks via `renameTo()` failure; the exception propagates to the ViewModel and the entry stays in place.
- **[Empty groups visible in picker]** → Acceptable for now; filtering out empty groups is a minor enhancement for later.
