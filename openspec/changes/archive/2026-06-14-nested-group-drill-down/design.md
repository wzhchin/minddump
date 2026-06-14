## Context

Today a group is reached via a single-value `uiState.selectedGroup: File?`, and `GroupDetailScreen` reads that value to filter `entries.filter { it.groupPath == selectedGroup.absolutePath }`. The screen is read-only: no input bar, no sub-group cards. The storage layer is the real constraint:

- `createGroup(space, name)` always creates under `ensureCurrentMonthDir(space)` — there is **no path to create a group inside another group**, so nesting can't even originate in-app.
- `scanGroups(space)` lists only month-top group dirs — sub-groups (if they existed on disk) are invisible to the UI.
- `scanEntries` already recurses into nested group dirs and stamps each file's `groupPath` with the **deepest** containing group's absolute path — so nested members are already readable, just not separately surfaced.
- `dissolveGroup(groupDir)` assumes `groupDir.parentFile` is the month dir and moves members there. For a sub-group, the parent is a group, so members would jump past their natural parent to month-top — a level-skipping bug.

So the storage layer is *half-ready* for nesting; this change closes the create/scan/dissolve gaps and reworks the navigation + screen to expose them.

## Goals / Non-Goals

**Goals:**
- A group page is a sub-homepage: direct members + nested sub-group cards + an InputBar that writes into this group.
- Unbounded drill-down: tap a sub-group → its page; repeat; back returns one level at a time.
- Create sub-groups in-app (EntryActionDrawer "新建组" inside a group page, plus multi-select merge).
- In-group capture: text/photo/audio/import from a group page land in that group.
- Don't break month-top grouping: the main feed cards and the picker behave exactly as before.
- Parent-aware dissolve so members don't skip levels.

**Non-Goals:**
- No change to group directory naming (`{ts}-g[-{name}]`).
- No cross-month or cross-space nesting (a group still lives within one month of one space).
- No drag-to-reorder or group re-parenting UI beyond dissolve/create/move.
- No change to encryption behavior (in-group entries follow the same encrypt-if-Private rule).
- No rework of per-entry long-press actions beyond threading the parent group into "新建组".

## Decisions

> **Superseded (UI/navigation framing) by `unify-group-screen`.** The two-screen
> `GroupDetailScreen` model described below was implemented, then found to be racy
> (in-group capture landed at month-top because navigating to the Camera route
> disposed the group screen and cleared `currentGroupDir`; drill/back clobbered
> the same field). `unify-group-screen` collapses the two screens into a single
> `MainScreen(currentDir)` where the root month dir is itself the root group, and
> sets `currentGroupDir` once per route entry with no dispose clear. **The
> storage-layer decisions below (3–6: `scanChildGroups`, `createGroup(parentDir)`,
> write-target saves, parent-aware dissolve) remain valid and in force** — only
> the screen/navigation decisions (1, 7) are reframed by `unify-group-screen`. See
> `openspec/changes/unify-group-screen/design.md`.

### Decision 1: Navigation identity = encoded group path on the back stack
Route `group_detail/{groupPath}` (`navArgument` StringType, Uri-encoded absolute path). Each descent is a `navigate(group_detail/<encoded>)`; back pops one entry. `GroupDetailScreen` derives everything from the path arg.

- *Why over `selectedGroup` uiState:* a single `File?` can't represent a stack (`A → B` overwrites `A`; back returns to home, not `A`). Encoding the path in the back stack is the Compose Navigation idiom, survives config change/process death, and needs no manual stack bookkeeping.
- *`selectedGroup` disposition:* removed as the navigation source of truth. If any non-navigation consumer still needs "which group is open" (none found in audit), it derives from the current route.
- *Alternative considered:* a `List<File>` stack in uiState pushed/popped alongside the nav stack. Rejected — two stacks to keep in sync; the nav stack already encodes the truth.
- *Superseded:* the route as back-stack identity is retained by `unify-group-screen`, but `GroupDetailScreen` no longer exists — the same `MainScreen` composable serves root and all group levels, parametrized by `currentDir`. The encoded path is the *carrier* of `currentDir`, not a separate screen.

### Decision 2: Direct-members-only filtering (no descendants)
A group page shows `entries.filter { it.groupPath == groupDir.absolutePath }`. Sub-group members have a deeper `groupPath` and are excluded; they appear in their own page.

- *Why over "all descendants" (`startsWith`):* showing descendants would duplicate them across the chain and blur which level you're at. One-level-at-a-time matches the mental model of drilling in.
- *Cost:* a member buried 3 levels deep requires 3 taps to reach. Accepted — that's the point of a hierarchy.

### Decision 3: `scanChildGroups(parentDir)` — one scanner, two callers
Add `scanChildGroups(parentDir: File): List<File>` that lists `parentDir`'s direct sub-directories with `role == GROUP`. `scanGroups(space)` becomes `scanChildGroups(ensureCurrentMonthDir(space))` — same month-top behavior, single implementation.

- *Why generalize:* the main feed (month-top) and a group page (group parent) need the identical "list child groups" operation. One function prevents drift.
- *ViewModel state:* the main feed keeps `groups: List<GroupSummary>` (month-top). Add `childGroups: List<GroupSummary>` populated for the currently-open group path (null/empty on the main screen). `refreshGroups` takes the open path into account; the group page triggers its own refresh on entry.

### Decision 4: `createGroup(space, name, parentDir: File?)`
`parentDir == null` → month-top (current behavior, all existing callers unchanged). `parentDir != null` → create under that group dir. The `{ts}-g[-{name}]` naming is unchanged; only the parent changes.

- *Why optional param over two functions:* ~every existing call site is the month-top case; an optional defaulted param keeps them call-site-identical and makes the new "inside a group" case the explicit opt-in.
- *Threading:* the group page and EntryActionDrawer pass `currentGroupDir` (the open path) as `parentDir`; everywhere else passes `null` (or omits it).

### Decision 5: In-group writing via a write-target, not post-hoc move
The group page's InputBar carries a `targetGroupDir: File?` (the open path). Repository save functions (`saveTextEntry`, `saveComment`, media save, import) accept it: when set, the file is written directly into the target group dir instead of the month dir, with `groupPath` stamped accordingly.

- *Why over "create at month-top then moveToGroup":* two filesystem ops + two index writes per capture, plus a window where the entry shows in the wrong place. Writing in place is one op and the entry lands correct immediately.
- *Comment target dir:* a comment on an entry inside a group already lands next to that entry (same dir) via `saveComment`'s existing `targetDir` param — unchanged. The new target only affects top-level captures in a group page.

### Decision 6: Parent-aware dissolve — members go to the parent group
`dissolveGroup(groupDir)`: determine the destination as `parentDir = groupDir.parentFile`. If `parentDir` itself is a group dir (`role == GROUP`), members move there (stay within the parent group). If `parentDir` is a month dir, members move there (current month-top behavior). Then delete `groupDir`.

- *Why parent-group over month-top:* dissolving a sub-group should flatten it into its immediate parent, not eject members two levels up to the feed. Ejecting to month-top would silently scatter nested members across the home feed — surprising and hard to undo.
- *Sub-group dirs inside the dissolved group:* if a sub-group contains its own sub-groups, those sub-dirs are moved as directories into the parent alongside loose files (preserving their nesting). Reconcile re-indexes them. This is the one non-trivial edge; called out in tasks/verification.
- *`moveOutOfGroup` (single entry, "移出该组"):* currently moves to month-top. For consistency it should move to the parent group when the entry is in a nested group. Same parent-detection as dissolve.

### Decision 7: Sub-group cards reuse `GroupSummaryCard`
`GroupSummaryCard` already renders name/count/type-chips and routes `onClick`/`onLongClick`. The group page feeds it `childGroups` with `onClick = navigate(child.groupDir)` and `onLongClick` = the existing group action menu (dissolve/rename), now parent-aware.

- *Superseded:* `GroupDetailScreen` is gone; `MainScreen` already renders `EntryList` (loose entries + group cards) for both root and group scopes. Sub-group cards reuse `GroupSummaryCard` inside the same unified list — there is no separate "group page" component to fold them into.

## Risks / Trade-offs

- **[Risk] `scanChildGroups` over-scans if a group dir ever holds non-group sub-dirs** (e.g. a `.cache` or temp dir). → *Mitigation:* filter strictly on `FileMetadata.fromFile(it)?.role == EntryRole.GROUP`; the `{ts}-g*` naming is the gate. `.cache` lives at root, not under groups.
- **[Risk] Deeply nested paths make the back stack long and the encoded path large.** → *Mitigation:* Uri-encoding handles any depth; practically users won't go >4–5 levels. No hard cap imposed (would be arbitrary); monitor.
- **[Risk] Reconcile/`scanEntries` interaction with moved sub-group dirs on dissolve.** → *Mitigation:* after dissolve, the page calls the normal `refreshEntries` (full-space reconcile) + `refreshGroups`; reconcile already recurses and re-stamps `groupPath`. Verified path in tasks.
- **[Trade-off] Direct-members-only means a deep member is several taps away.** Accepted — matches hierarchical intent.
- **[Trade-off] Dissolving a group that contains sub-groups flattens sub-group *dirs* (not their contents) into the parent.** Mildly surprising but preserves data; documented in spec scenarios.

## Migration Plan

- Additive at the storage layer: new optional `parentDir` params default to existing behavior; `scanChildGroups` reimplements `scanGroups` identically for the month case. No data format or schema change.
- No data migration: existing month-top groups render and behave identically. Nested structures created post-release are plain directories the existing scanner already understands.
- Rollback: revert the route/screen/storage changes; nested dirs (if any were created) remain valid on disk and simply aren't navigable in a rolled-back build — no corruption.
