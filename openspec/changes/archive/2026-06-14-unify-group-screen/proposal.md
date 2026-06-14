## Why

The in-flight `nested-group-drill-down` change implements nesting as **two screens** (`MainScreen` for the root feed + a pushed `GroupDetailScreen` for a group), bridged by a shared mutable `currentGroupDir` in `uiState` driven by the group screen's compose lifecycle (`LaunchedEffect`/`DisposableEffect`). That bridge is fundamentally racy and ships two confirmed bugs:

1. **In-group photo/video lands at month-top, not the group.** Navigating to the `Camera` route removes `GroupDetailScreen` from composition → its `onDispose` clears `currentGroupDir` → the camera route then computes the capture file against a null target (month dir).
2. **Drill-down / back race.** Tapping a sub-group sets `currentGroupDir(B)` then navigates; the parent screen's `onDispose` may run after the child's `LaunchedEffect`, clobbering the value (wrong scope or empty sub-group cards).

The deeper issue is the mental model: **the root month directory is itself just a group** (the root group). There is no real two-screen distinction — there is one screen whose "current directory" changes. Entering a group is "moving the current directory down one level," not "opening a detail page." This change unifies the two screens into one `MainScreen` parametrized by a `currentDir`, removing the racy bridge and fixing both bugs by construction. It builds on the nesting primitives (parent-aware create/scan/dissolve, write-target params) already added by `nested-group-drill-down`, so the storage/repository layer is unchanged.

## What Changes

- **Single screen, mutable `currentDir`.** `GroupDetailScreen` is deleted; its body folds into `MainScreen`, which gains a `currentDir: File?` parameter. `null` = root (the current month dir, treated as the root group); a non-null `File` = an open group. The same `MainScreen` composable serves every level.
- **Route as `currentDir` carrier, not a separate screen.** The `group_detail?groupPath={groupPath}` route survives only as the persistent carrier of `currentDir` on the back stack (process-death/config-change safety, free system-back). Its composable body calls the unified `MainScreen` with the decoded path.
- **No lifecycle-driven `currentGroupDir` writes.** `MainScreen` calls `viewModel.setCurrentGroupDir(currentDir)` from a single `LaunchedEffect(currentDir)` with **no `onDispose` clear**. Returning to root is achieved by navigating back to `Screen.Main` (whose `currentDir` is null), not by a screen leaving composition. This eliminates both races — including the camera-capture bug, since navigating to `Camera` no longer clears the group scope.
- **In-group InputBar hides the space toggle.** The Public/Private `SpaceSwitchButton` is shown only at root (`currentDir == null`). Inside a group the InputBar keeps all capture/import controls but drops the toggle. Encryption behavior is unchanged.
- **Per-scope top bar.** Root keeps the existing search/stats/settings `CenterAlignedTopAppBar`; an open group shows a `TopAppBar` with a back button and the group name.

Non-changes: the storage primitives (`createGroup(parentDir)`, `scanChildGroups`, parent-aware `dissolveGroup`/`moveOutOfGroup`, write-target `saveTextEntry`/`getPhotoFile`/etc.), `EntryList`'s merged feed rendering, group directory naming, and the picker's month-scoped listing are all unchanged.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `nested-group-drill-down`: The "group detail is a drillable sub-homepage" requirement is rewritten to the unified single-screen model — `MainScreen` is the single composable for root and all group levels; the route is the carrier of `currentDir`, not a separate screen. The in-group-capture requirement is strengthened to guarantee capture lands in the open group even when the camera/audio route sits on the back stack above it (the bug this change fixes). A new requirement hides the space toggle inside a group.

## Impact

- **ui/GroupDetailScreen.kt** — **deleted**; body folded into `MainScreen`.
- **ui/MainScreen.kt** — add `currentDir: File?` param; branch the top bar (root vs group), derive members + child-group cards from `currentDir`, single `LaunchedEffect(currentDir) → setCurrentGroupDir(currentDir)` (no dispose clear), drop the inline `setCurrentGroupDir` on group click.
- **ui/MindDumpNavGraph.kt** — `Screen.GroupDetail` composable body becomes a `MainScreen(currentDir = File(decodedPath), onBack = popBackStack)` call; `Screen.Main` keeps `currentDir = null`.
- **ui/InputBar.kt** — add `showSpaceToggle: Boolean = true`; gate `SpaceSwitchButton` on it.
- **ui/MindDumpViewModel.kt** — minor: remove the dead `looseEntries` field. No structural change (the `currentGroupDir` semantics are fixed by removing the lifecycle-driven writes upstream).
- **openspec/changes/nested-group-drill-down/** — its UI/navigation tasks and design decisions are superseded by this change; reconcile on archive.
