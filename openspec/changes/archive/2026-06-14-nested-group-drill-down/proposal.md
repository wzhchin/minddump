## Why

A group is currently a "folder open" — the GroupDetail screen is read-only, has no input bar, and can't contain sub-groups, so there is exactly one level of grouping. Real thinking is hierarchical (a project → a sub-topic → a thread), and the storage layer already nests directories recursively, but the UI and `createGroup`/`scanGroups` are hardwired to the month-top level. This change makes a group behave like a drillable sub-homepage: enter it, keep writing into it, nest groups inside it, and descend as deep as needed.

## What Changes

- **Drill-down navigation**: the GroupDetail route becomes `group_detail/{groupPath}` (Uri-encoded absolute path). Each tap on a sub-group pushes a new instance; Compose Navigation handles per-level back. The single-value `uiState.selectedGroup` is no longer used for navigation.
- **GroupDetail = sub-homepage**: the screen gains nested sub-group cards (reusing `GroupSummaryCard`) and a bottom `InputBar`. Direct members render as today; sub-group members render only inside their own page.
- **In-group writing**: new entries created from a group's InputBar (text/photo/audio/import) land directly in that group's directory. Multi-select "merge into group" inside a group page creates the target group under the current group, not at month-top.
- **Sub-group creation**: `createGroup` accepts an optional `parentDir`. `null` keeps current month-top behavior; a non-null parent creates the group inside a group, enabling unbounded nesting. The EntryActionDrawer "新建组" path threads the current group path through.
- **Nested-aware dissolve**: dissolving a sub-group moves its members to the **parent group** (not the month directory), so members don't jump levels. The current `dissolveGroup` assumes `groupDir.parentFile` is the month dir — fixed to detect a group parent vs. month parent.
- **Child-group scanning**: `scanChildGroups(parentDir)` scans any directory (group or month) for `role == GROUP` sub-directories, powering the sub-group cards in each group page. Month-top scanning (`scanGroups`) is unchanged for the main feed and picker.

Non-changes: group directory naming format (`{ts}-g[-{name}]`), rename, the picker's month-scoped listing, and per-entry long-press actions other than the new-group parent threading.

## Capabilities

### New Capabilities
- `nested-group-drill-down`: Treating a group as a drillable sub-homepage — parametrized navigation, sub-homepage layout (InputBar + nested group cards + direct members), in-group entry writing, sub-group creation, and parent-aware dissolve.

### Modified Capabilities
- `group-list-loading`: The create-and-move atomic operation gains an optional parent group directory, so "新建组" can create a sub-group under the currently-open group instead of always at month-top. (Existing month-top behavior is preserved when no parent is given.)

## Impact

- **ui/MindDumpNavGraph.kt** — `group_detail/{groupPath}` route with `navArgument`; `onNavigateToGroupDetail(groupPath)` carries the encoded path.
- **ui/GroupDetailScreen.kt** — read `groupPath` from nav arg (drop `selectedGroup` dependency); render sub-group cards + InputBar; thread current group path into in-group create/save/move.
- **ui/MainScreen.kt** — `onGroupClick` navigates with the encoded path.
- **ui/MindDumpViewModel.kt** — group create/move/save operations accept `parentGroupDir: File?`; add child-group loading for an open group page; retire `selectedGroup` for navigation.
- **storage/FileStorageEngine.kt** — `createGroup(space, name, parentDir)`; `scanChildGroups(parentDir)`; `dissolveGroup` parent-aware (group parent vs. month parent).
- **data/MindDumpRepository.kt** — thread `parentGroupDir` through `createGroup`/`createAndMoveToGroup`/`createAndMoveGroupForEntries`/`saveTextEntry`/`saveComment`/media save; expose `scanChildGroups`.
- **ui/components/EntryActionDrawer.kt** — "新建组" passes the current open group (if any) as parent.
- **ui/EntryItem.kt** — `GroupSummaryCard` reused for sub-group cards (descent on tap).

**Open decision (captured in design.md):** dissolving a sub-group moves members to the parent group (recommended) vs. the month directory. Proposal assumes parent-group; reversible in design before implementation.
