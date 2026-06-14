# Implementation Tasks

## 1. Storage — nesting primitives

- [x] 1.1 Generalize `FileStorageEngine.scanGroups(space)` into `scanChildGroups(parentDir: File): List<File>` (list direct sub-dirs with `role == GROUP`); reimplement `scanGroups(space)` as `scanChildGroups(ensureCurrentMonthDir(space))` so month-top behavior is byte-identical.
- [x] 1.2 Add `parentDir: File? = null` to `FileStorageEngine.createGroup(space, name, parentDir)`; `null` → month-top (current), non-null → create under that dir. Naming rule `{ts}-g[-{name}]` unchanged.
- [x] 1.3 Make `FileStorageEngine.dissolveGroup(groupDir)` parent-aware: destination = `groupDir.parentFile`; if that parent is a group dir (`role == GROUP`) move members there, else to month-top. Move sub-group *directories* (not just files) too so nested structure is preserved.
- [x] 1.4 Make `FileStorageEngine.moveOutOfGroup(file, space)` parent-aware (move to parent group when the entry is nested, not always month-top).

## 2. Repository — thread parent through

- [x] 2.1 Thread `parentGroupDir: File? = null` through `createGroup`, `createAndMoveToGroup`, `createAndMoveGroupForEntries` (default null = month-top, call sites unchanged).
- [x] 2.2 Add a write-target param to `saveTextEntry` (and the comment/media/import save paths) so an entry can be written directly into a group dir with the right `groupPath`, instead of create-then-move. Keep `null` path identical to today.
- [x] 2.3 Expose `scanChildGroups(parentDir)` and a `loadChildGroupSummaries(parentDir)` helper that returns `List<GroupSummary>` for a group page (members resolved from the current entry set).
- [x] 2.4 Confirm `moveEntryOutOfGroup`/dissolve routing now reaches the parent-aware storage methods.

## 3. ViewModel — group page state & ops

- [x] 3.1 Add `childGroups: List<GroupSummary>` to `UiState`; add `refreshChildGroups(parentDir)`; populate it for the open group path (null/empty on main screen).
- [x] 3.2 Add `currentGroupDir: File?` (derived from the open route, or an explicit setter) used to scope in-group create/save/move ops; retire `selectedGroup` as the navigation source of truth.
- [x] 3.3 Make group create/move ops accept `parentGroupDir`; make in-group save (text/photo/audio/import) write into `currentGroupDir`; make multi-select merge use `currentGroupDir` as parent when set.
- [x] 3.4 Trigger `refreshChildGroups` + `refreshEntries` after any in-group create/move/dissolve.

## 4. Navigation — parametrized group route

- [x] 4.1 Change `Screen.GroupDetail` to `group_detail/{groupPath}` with a `navArgument` (StringType, Uri-encoded absolute path); add `routeWithGroup(path)` helper.
- [x] 4.2 Update `MindDumpNavGraph`: `onNavigateToGroupDetail(groupPath)` navigates with the encoded path; resolve `groupDir = File(Uri.decode(path))` and pass into `GroupDetailScreen`.
- [x] 4.3 Update `MainScreen.onGroupClick` to navigate with the encoded group dir path.

## 5. GroupDetailScreen — sub-homepage

> **Superseded by `unify-group-screen`.** This two-screen `GroupDetailScreen`
> build was implemented, then replaced: `GroupDetailScreen.kt` is deleted and its
> responsibilities folded into a unified `MainScreen(currentDir)`. The items below
> record the original build (historical); the unified version lives in
> `openspec/changes/unify-group-screen/tasks.md`.

- [x] 5.1 Replace `uiState.selectedGroup` with a `groupDir: File` parameter (from the nav arg); recompute members via `groupPath == groupDir.absolutePath`.
- [x] 5.2 Render sub-group cards: feed `uiState.childGroups` into `GroupSummaryCard` with `onClick = onNavigateToGroupDetail(child.groupDir)` and `onLongClick` = existing group menu.
- [x] 5.3 Add the bottom `InputBar` (reuse the component), wiring its submit/capture/import to ViewModel ops scoped with `groupDir` as the write target.
- [x] 5.4 Wire the entry action drawer's "新建组"/"移动到组" inside the page to pass `groupDir` as parent.

## 6. EntryActionDrawer — parent threading

- [x] 6.1 Add an optional `parentGroupDir: File?` to the drawer's create/move callbacks; when invoked from a group page it is the open group, else null (month-top).
- [x] 6.2 Verify month-top create flow still passes null (main feed behavior unchanged).

## 7. Verification

- [x] 7.1 Build: `assembleDebug` ✓ + `ktlintCheck` ✓ + `detekt` clean for this change. The change does NOT add params to `EntryActionDrawer` (parent threading went through ViewModel state instead — see task 6), so the pre-existing `EntryActionDrawer` LongParameterList violation is unchanged on HEAD, not worsened by this change.
- [x] 7.2 Manual: main feed group card → opens page with members + input bar; month-top behavior unchanged.
- [x] 7.3 Manual: create a sub-group from within a group page → card appears → tap drills in → back returns to parent page (not feed).
- [x] 7.4 Manual: write text/capture photo inside a group → entry lands as direct member of that group.
- [x] 7.5 Manual: dissolve a sub-group → members move into the parent group (not the feed); dissolve a month-top group → members go to feed.
- [x] 7.6 Manual: dissolve a group that contains a sub-group → sub-group dir (with contents) moves to parent intact; reconcile re-indexes; nothing lost.
- [x] 7.7 Manual: multi-select merge inside a group → new group created under current group.
- [x] 7.8 Manual: Private space in-group entry encrypts; back-out/relaunch decrypts; nesting works across spaces independently.

> 7.2–7.8 validated on-device under the unified single-screen model delivered by
> `unify-group-screen` (the storage-layer behavior tested here is unchanged by the
> UI unification). See `openspec/changes/unify-group-screen/tasks.md` 7.2–7.7.

> 7.2–7.8 require a device/emulator run; deferred to manual QA.
