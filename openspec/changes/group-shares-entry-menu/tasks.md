# Tasks — group-shares-entry-menu

Implementation order: drawer target model (1) → wire group long-press (2) → hide inapplicable rows + group-only dissolve (3) → delete/dissolve semantics (4) → remove GroupActionSheet (5) → verify (6).

## 1. Drawer target model

- [x] 1.1 Introduce a drawer-target concept in `EntryActionDrawer` (e.g. a sealed interface `ActionDrawerTarget` with `Entry(MindDumpEntry)` and `Group(groupDir: File)` variants), or accept an optional `groupTarget: File?` alongside `entry`, with exactly one non-null.
- [x] 1.2 Add group-bound callbacks to the drawer signature: `onGroupTogglePin`, `onGroupSetStatus`, `onGroupRename`, `onGroupShare`, `onGroupMultiSelect`, `onGroupDissolve`, `onGroupDelete` (null-default `(() -> Unit)?` / `((TodoState) -> Unit)?`); keep entry callbacks as-is.
- [x] 1.3 Render the header from the target: entry → `entry.file.name`; group → display name (`groupDir.name.substringAfter("-g", groupDir.name)`, fallback `R.string.group_unnamed`).

## 2. Wire group long-press to the drawer

- [x] 2.1 In `MindDumpViewModel`, reuse `groupMenuFor: File?` as the channel that surfaces a group in the drawer (already exists); keep `selectedEntryForAction` for entries. Only one is non-null at a time.
- [x] 2.2 In `MainScreen`, when `uiState.groupMenuFor != null`, render `EntryActionDrawer` with a `Group` target bound to `viewModel.toggleGroupPinned` / `setGroupStatus` / `renameGroup` / `shareGroup` / `enterMultiSelectModeWithGroup` / `dissolveGroup` / `deleteGroup`. `onDismiss` calls `viewModel.dismissGroupMenu()`.
- [x] 2.3 Add a `deleteGroup(groupDir: File)` ViewModel wrapper over `repository.deleteGroup(groupDir, currentSpace)` if one does not already expose cleanly to the UI (verify and add only if needed).
- [x] 2.4 Keep `onGroupLongClick` (non-multi-select) calling `viewModel.selectGroupForMenu(groupDir)` — unchanged route, new drawer surface.

## 3. Hide inapplicable rows + group-only dissolve

- [x] 3.1 Gate add-comment, edit-tags, add-event, move-to-group, create-group, move-out-of-group, and move-to-space rows on the target being an `Entry`. Comments already hide several via `isComment`; extend the guard to "entry target AND not comment" where relevant.
- [x] 3.2 Add a "解散" action row visible ONLY for `Group` targets, bound to `onGroupDissolve`. Place it near the destructive delete row.
- [x] 3.3 Ensure pin / status / rename / share / multi-select / delete rows render for both target kinds, bound to the matching callback set.

## 4. Delete vs dissolve semantics

- [x] 4.1 For a `Group` target, "删除" calls `onGroupDelete` → `repository.deleteGroup` (whole-tree soft-delete to `.trash/`); for an `Entry` it keeps the existing trash confirmation path.
- [x] 4.2 "解散" (group only) calls `onGroupDissolve` → `repository.dissolveGroup` (members moved to parent, empty dir removed).
- [x] 4.3 Add zh-CN + en strings: `group_dissolve` ("解散"), `group_dissolve_confirm` ("解散分组？组内成员将移出，不会被删除。" / "Dissolve group? Members will be moved out, not deleted."), `group_delete_confirm` ("删除分组？分组及其成员将移入回收站，可恢复。" / "Delete group? The group and its members will be moved to the trash and can be restored.").
- [x] 4.4 Show the appropriate confirmation dialog (dissolve-confirm for 解散, group-delete-confirm for 删除 on a group) before running.

## 5. Remove GroupActionSheet

- [x] 5.1 Delete the `GroupActionSheet` composable from `EntryActionDrawer.kt` and its call site in `MainScreen.kt`.
- [x] 5.2 Remove now-unused imports/strings introduced only for `GroupActionSheet` (keep `group_unnamed`, shared by the drawer header).

## 6. Verify

- [ ] 6.1 `./gradlew detekt ktlintCheck` clean. — Blocked: Gradle cannot start in this sandbox (wildcard IP); run on host.
- [ ] 6.2 `./gradlew assembleDebug` succeeds. — Blocked: same Gradle host networking issue; run on host.
- [ ] 6.3 Smoke (device, deferred): long-press a group opens the entry drawer; entry-only rows hidden; "解散" present for group and absent for entry; 删除 on a group trashes the whole tree; 解散 scatters members and removes the folder; entry drawer unchanged.
