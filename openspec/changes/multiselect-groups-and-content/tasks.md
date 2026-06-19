# Tasks — multiselect-groups-and-content

Implementation order: state model (1) → entry card body (2) → group selection UI (3) → merge re-cluster (4) → count label (5) → verify (6).

## 1. Selection state model

- [x] 1.1 Add `selectedGroups: Set<File> = emptySet()` to `MindDumpViewModel.UiState`.
- [x] 1.2 Add `toggleGroupSelection(groupDir: File)` that add/removes from `selectedGroups`; multi-select exits only when both `selectedEntries` and `selectedGroups` are empty (update `exitMultiSelectMode`, `toggleEntrySelection`, `toggleGroupSelection` accordingly).
- [x] 1.3 Add `enterMultiSelectModeWithGroup(groupDir: File)` that sets `isMultiSelectMode = true`, `selectedGroups = setOf(groupDir)`, clears `selectedEntryForAction`.

## 2. Entry card body in multi-select

- [x] 2.1 In `EntryItem` (`ui/EntryItem.kt`), remove the `if (isMultiSelectMode) { Row(header only) } else { when(type) }` branch. Always render the per-type body via `when (entry.type)`.
- [x] 2.2 Render the multi-select checkbox as a leading affordance (overlaid on the media hero for photo/video, or in the header row for text/recording/file) so it toggles selection without hiding the body.
- [x] 2.3 Keep the whole-card `combinedClickable` toggling selection and long-press toggling selection in multi-select (no action sheet). Preserve non-multi-select tap/long-press behavior.

## 3. Group selection UI

- [x] 3.1 Add an `isMultiSelectMode`, `isSelected`, `onToggle` params to `GroupSummaryCard`; render a checkbox affordance (checked when selected) in multi-select. Place it without displacing carousel/name/stats.
- [x] 3.2 In `EntryList`, thread `selectedGroups: Set<File>` and route group tap/long-press to toggle when `isMultiSelectMode`, else to `onGroupClick`/`onGroupLongClick`.
- [x] 3.3 In `MainScreen.kt`, pass `selectedGroups = uiState.selectedGroups` to `EntryList`; make `onGroupClick`/`onGroupLongClick` toggle via `viewModel.toggleGroupSelection` when in multi-select.
- [x] 3.4 Add a "多选" action to `GroupActionSheet` that calls `viewModel.enterMultiSelectModeWithGroup(groupDir)` then dismisses; wire it from `MainScreen`'s group menu (`groupMenuFor`).

## 4. Merge re-cluster

- [x] 4.1 In `mergeSelectedIntoGroup(groupDir)`: re-read each selected group's current members from the repository; build `all = selectedEntries + selectedGroups.flatMap { members }` excluding the target group's own members when target is a selected source; move `all` via `repository.moveEntriesToGroup`.
- [x] 4.2 After the move, dissolve each selected source group that is now empty on disk and is not the target (reuse the `dissolveGroup` repository path). Leave non-empty sources and the target intact.
- [x] 4.3 Apply the same member-expansion + source-dissolve to `mergeSelectedIntoNewGroup(name)` via `repository.createAndMoveGroupForEntries`.
- [x] 4.4 Clear both `selectedEntries` and `selectedGroups` and exit multi-select after a successful merge.

## 5. Count label + strings

- [x] 5.1 Add zh-CN keys to `res/values/strings.xml` (e.g. `multiselect_count` = "已选 %1$d 项", `multiselect_count_with_groups` = "已选 %1$d 项 · 含 %2$d 分组") and matching `values-en/strings.xml` entries.
- [x] 5.2 Add a `selectedGroupCount` param to `MultiSelectTopBar` and render the with-groups label when `selectedGroupCount > 0`, else the entries-only label.
- [x] 5.3 In `MainScreen.kt`, pass `selectedGroupCount = uiState.selectedGroups.size` to `MultiSelectTopBar`.

## 6. Verify

- [ ] 6.1 `./gradlew detekt ktlintCheck` clean. — Blocked: Gradle cannot start in this sandbox ("Could not determine a usable wildcard IP for this machine"); run on host.
- [ ] 6.2 `./gradlew assembleDebug` succeeds. — Blocked: same Gradle host networking issue; run on host.
- [ ] 6.3 Smoke (device, deferred): entry bodies visible in multi-select; group checkbox toggles; merge of mixed entries+groups moves members and dissolves emptied sources; target-is-source case leaves target intact; count label shows group count.
