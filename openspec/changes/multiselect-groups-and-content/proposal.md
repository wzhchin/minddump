## Why

Multi-select mode today degrades each entry card to a bare header (type dot + timestamp): photos, text, recordings, and video previews all collapse to a one-line row, so the user selects blind — they cannot tell entries apart while batching. Group cards are not selectable at all, so a user who wants to combine several existing groups into one (re-cluster) has no batch path; they can only dissolve groups one by one.

## What Changes

- **Entry cards keep their content in multi-select.** In multi-select mode an entry card SHALL render the same per-type body (media hero, text, recording chip, file chip) it renders outside multi-select, plus the checkbox affordance. The card body is no longer hidden behind the header-only branch.
- **Group cards become selectable in multi-select.** Entering multi-select (from a group's long-press action sheet, or by toggling on any group card) SHALL let the user tap group cards to add/remove whole groups from the selection. A selected group's checkbox reflects its state.
- **Merge-to-group works on a selection that includes groups.** The "合并为分组" action SHALL accept a mixed selection of loose entries and groups: the merge collects every member of every selected group plus every selected loose entry and moves them into the target (existing or newly created) group. After merging, selected source groups whose members all moved out SHALL be dissolved (become empty and are removed).
- **Selected-count label counts both.** The multi-select top bar SHALL report a count that reflects both selected entries and selected groups (e.g. "已选 3 项 · 含 2 分组").

**No breaking data contract:** the underlying `moveEntriesToGroup` / `createAndMoveGroupForEntries` repository operations are reused; only the caller now expands selected groups into their members before moving. No Room migration; no filename format change.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `entry-card`: the multi-select scenario of "Single entry renders as an expressive card" is tightened — the per-type body SHALL still render in multi-select, not only the header.
- `group-card`: a new requirement that group cards are selectable in multi-select (with a selection affordance), and that selected groups participate in the merge/re-cluster action.
- `entry-actions-drawer`: the multi-select merge ("合并为分组") action is extended to accept a mixed selection of entries and groups, expanding group members into the move and dissolving emptied source groups.

## Impact

- **Code:** `ui/EntryItem.kt` (multi-select branch of `EntryItem` to render the body; `GroupSummaryCard` selection affordance + tap toggles selection in multi-select; `EntryList` passes `selectedGroups`), `ui/MindDumpViewModel.kt` (`UiState.selectedGroups: Set<File>`; `toggleGroupSelection`; `enterMultiSelectMode` overload/flag for groups; `mergeSelectedIntoGroup` / `mergeSelectedIntoNewGroup` expand selected-group members and dissolve emptied sources; `deleteSelectedEntries` unchanged scope — groups are merged, not deleted, so delete stays entry-only), `ui/MainScreen.kt` (wire `onGroupClick`/`onGroupLongClick` in multi-select, count label), `ui/components/EntryActionDrawer.kt` (`MultiSelectTopBar` count label), `GroupActionSheet` gains a "多选" entry action.
- **Resources:** `res/values/strings.xml` (+ `values-en`) — a key or two for the count label (e.g. `multiselect_count_with_groups`).
- **Data:** No on-disk schema/filename change. `FileStorageEngine.createGroup` / `moveToGroup` / group dissolve path already exist. No Room migration.
- **Build/verify:** `./gradlew detekt ktlintCheck` and `assembleDebug` after implementation.
- **Risk:** Medium. Selection state gains a second set (`selectedGroups`); merge logic must expand members and dissolve sources atomically. UI contract changes (group cards gain a checkbox in multi-select; entry cards show bodies again).
