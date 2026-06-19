## Context

Multi-select (`isMultiSelectMode` in `MindDumpViewModel.UiState`) currently tracks only a `selectedEntries: Set<MindDumpEntry>`. Two degradations stem from this:

1. `EntryItem` switches its entire body into a header-only `Row` when `isMultiSelectMode` is true (`ui/EntryItem.kt` ~L604), hiding photos/text/video behind a bare timestamp row. Users batch blind.
2. `GroupSummaryCard` has no multi-select branch — taps always drill into the group, and there is no `selectedGroups` set, so groups can never be batched/re-clustered.

The repository already exposes `moveEntriesToGroup(entries, groupDir)` and `createAndMoveGroupForEntries(entries, space, name, parent)` (`data/MindDumpRepository.kt:580,595`) plus a group dissolve path used by `dissolveGroup`. These reuse as-is.

## Goals / Non-Goals

**Goals:**
- Render entry card bodies in multi-select (identical per-type content to non-multi-select, plus a checkbox).
- Make group cards selectable; add `selectedGroups: Set<File>` to `UiState`.
- Extend the merge action to expand selected groups into their members and dissolve emptied sources.
- Report a count label that distinguishes entries vs groups.

**Non-Goals:**
- Multi-select delete of groups (delete stays entry-only; groups are merged, not deleted). Selected groups are silently ignored by the Delete action, or the Delete action is disabled when groups are selected — see Decisions.
- Re-clustering across spaces or across months; merge stays within the current scope/space (same as today's loose-entry merge).
- Persisting selection across process death.

## Decisions

**D1 — Selection model: two parallel sets, not a unified type.**
Add `selectedGroups: Set<File>` alongside `selectedEntries: Set<MindDumpEntry>`. Rejected: a single `Set<Selectable>` sealed type — entries and groups have no common useful supertype, and the render path already dispatches by `FeedItem` subtype. Two sets keep `toggleEntrySelection` / `toggleGroupSelection` disjoint and the merge logic simply `entries + groups.flatMap { it.members }`. Multi-select exits only when both sets are empty.

**D2 — Entering group multi-select.**
Add a "多选" action to `GroupActionSheet` (`ui/components/EntryActionDrawer.kt`) that calls a new `enterMultiSelectModeWithGroup(groupDir)`. Also, a plain tap on a group card while `isMultiSelectMode` is already active toggles that group (mirrors entry long-press-then-toggle). Long-press on a group in multi-select toggles selection (does not open the sheet), consistent with entry behavior.

**D3 — Merge expands members and dissolves emptied sources.**
In `mergeSelectedIntoGroup` / `mergeSelectedIntoNewGroup`, compute `val all = selectedEntries + selectedGroups.flatMap { g -> membersOf(g) }` where `membersOf` resolves the group's direct member entries (the repository/group summary already enumerates members). Move `all` via the existing repo functions. Then, for each source group except the target, if it has no remaining members on disk, dissolve it (reuse the `dissolveGroup` path). Rationale: avoids leaving empty `-g` folders that confuse the feed's group enumeration.

**D4 — Target-is-a-source edge case.**
If the chosen target equals one of the selected source groups, do not move that group's own members to themselves and do not dissolve the target. The other source groups still dissolve when emptied.

**D5 — Entry card body in multi-select.**
Remove the `if (isMultiSelectMode) headerRow else when(type)` branch in `EntryItem`. Always render the per-type body; render the checkbox as an overlay/leading affordance that toggles selection on tap. The whole-card `combinedClickable` still toggles selection. Long-press still toggles (does not open the action sheet) in multi-select, matching today's entry behavior.

**D6 — Delete action scope with groups selected.**
Keep `deleteSelectedEntries` operating on `selectedEntries` only. Disable the Delete button in `MultiSelectTopBar` when `selectedGroups.isNotEmpty()` AND `selectedEntries.isEmpty()` (nothing to delete); when both are present, delete operates on the entries and leaves groups untouched. Rationale: deleting a whole group silently is destructive and out of scope; merging is the supported group batch path.

**D7 — Count label.**
`MultiSelectTopBar` gains `selectedGroupCount`. When > 0, render "已选 N 项 · 含 M 分组"; otherwise the existing "已选 N 项". Externalize both to `strings.xml` (zh-CN) and `values-en/strings.xml`.

## Risks / Trade-offs

- [Members of a selected group are computed from the current in-memory `GroupSummary`, which may be stale if a capture just happened] → The merge re-reads members from disk via the repository at merge time (not the UI snapshot) so a just-captured member is not lost.
- [Dissolving a source group is destructive of the folder (not its members, which moved)] → Dissolve only removes the directory after confirming it is empty; if any member failed to move, the group is left intact (D4 / D3 guard).
- [Checkbox overlay over a media hero hurts legibility] → Place the checkbox on a tonal scrim/leading slot consistent with the existing header, mirroring the entry multi-select checkbox placement.
- [Two selection sets complicate "select all" if added later] → Acceptable; a future select-all can populate both sets.

## Migration Plan

No data migration. Code-only change. Roll back by reverting the patch; the repository layer is untouched. Verify with `./gradlew detekt ktlintCheck` and `assembleDebug`, then device-smoke: enter multi-select from a group, toggle groups, merge groups into a new group, confirm source groups dissolve and members relocate.
