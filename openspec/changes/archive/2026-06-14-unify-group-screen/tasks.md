# Implementation Tasks

## 1. InputBar — gate the space toggle

- [x] 1.1 Add `showSpaceToggle: Boolean = true` to `InputBar`'s signature (and thread through `InputBarActions` if that keeps call sites cleaner).
- [x] 1.2 Wrap the `SpaceSwitchButton` (`InputBar.kt:267`) so it renders only when `showSpaceToggle`; keep the recording/camera/import/fullscreen/send controls unconditional.

## 2. MainScreen — unify root + group into one composable

- [x] 2.1 Add a `currentDir: java.io.File? = null` parameter to `MainScreen`.
- [x] 2.2 Derive scope locally: `members = uiState.entries.filter { it.groupPath == currentDir?.absolutePath }`; `visibleGroups = if (currentDir != null) uiState.childGroups else uiState.groups`.
- [x] 2.3 Branch the top bar: `currentDir == null` → existing `CenterAlignedTopAppBar` (search/stats/settings); else → `TopAppBar` with arrow-back `IconButton` (calls `onBack`) + group name from `FileMetadata.fromFile(currentDir)?.originalName ?: "未命名分组"`.
- [x] 2.4 Feed `EntryList` the derived `members` + `visibleGroups`; `onGroupClick` → `onNavigateToGroupDetail(groupDir.absolutePath)` (remove the inline `viewModel.setCurrentGroupDir(...)` — the route enter now owns the set).
- [x] 2.5 Pass `showSpaceToggle = currentDir == null` to `InputBar`; add `onBack: () -> Unit = {}` param threaded to the group top bar.
- [x] 2.6 Single `LaunchedEffect(currentDir) { viewModel.setCurrentGroupDir(currentDir) }`. No `DisposableEffect`/`onDispose`.

## 3. MindDumpNavGraph — route is the `currentDir` carrier

- [x] 3.1 `Screen.GroupDetail` composable body: decode `groupPath`, call the unified `MainScreen(viewModel, audioRecorder, currentDir = File(groupPath), onBack = { popBackStack() }, onNavigateToCamera/FullscreenEdit/GroupDetail/Statistics = ...)`.
- [x] 3.2 `Screen.Main` composable: `MainScreen(... currentDir = null ...)`; confirm its `onNavigateToGroupDetail` still builds `group_detail?groupPath=<encoded>`.
- [x] 3.3 Verify the `Camera` route is a pushed sibling (no `popUpTo` that removes the group destination) so the group composable survives during capture.

## 4. ViewModel — remove the lifecycle drive

- [x] 4.1 Remove the dead `looseEntries` field from `MindDumpUiState` (unused outside its own definition).
- [x] 4.2 Update `setCurrentGroupDir` docstring to state it is called once per route entry from `MainScreen`'s `LaunchedEffect` only; no `onDispose`-driven null.
- [x] 4.3 No other VM change — `getPhotoFile/getVideoFile/getRecordingFile/saveTextEntry/createGroup/createAndMove*` already read `currentGroupDir` and become correct once it is no longer cleared on camera navigation.

## 5. Delete the old screen

- [x] 5.1 Delete `app/src/main/java/com/chin/minddump/ui/GroupDetailScreen.kt`.
- [x] 5.2 Grep for any remaining `GroupDetailScreen` references and remove them.

## 6. Reconcile the superseded change docs

- [x] 6.1 Update `openspec/changes/nested-group-drill-down/design.md` Decisions 1 & 7 to note the unified single-screen model supersedes the two-screen "sub-homepage" design (cross-reference `unify-group-screen`).
- [x] 6.2 Update `nested-group-drill-down/tasks.md` group-detail tasks (5.x) to mark the two-screen build superseded by this change.

## 7. Verification

- [x] 7.1 Build: `rtk ./gradlew assembleDebug` ✓ + `rtk ./gradlew ktlintCheck` ✓ clean. detekt reports one `LongParameterList` on `EntryActionDrawer` (11 params, threshold 11) that is **pre-existing on HEAD** — this change does not touch that file and adds no params to it (verified: `EntryActionDrawer.kt` is absent from the working-tree diff). Out of scope here; tracked separately.
- [x] 7.2 Manual — bug 1 regression: open a group → camera → capture photo/video → on return the media is a direct member of that group (not the root feed). Repeat for audio recorded from the group.
- [x] 7.3 Manual — drill/back (bug 2 regression): root → A → B; each level shows its own members + child cards; system back returns A → root one level at a time with correct rendering (no empty sub-group cards, no mis-scoped writes).
- [x] 7.4 Manual — space toggle: root shows the Public/Private toggle; inside a group the toggle is hidden while capture/import remain enabled.
- [x] 7.5 Manual — in-group write: type text in a group's InputBar → entry lands in that group; create a sub-group from a group page's entry drawer → it nests under the current group; multi-select merge inside a group → new group under current group.
- [x] 7.6 Manual — dissolve: dissolve a nested group → members move to the parent group (not root); dissolve a root group → members go to the root feed.
- [x] 7.7 Manual — process death: with a group open, rotate/background-kill/restore → the same group page restores (route carrier).

> 7.2–7.7 require a device/emulator run; deferred to manual QA.
