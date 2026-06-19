## Context

The action surface is split today: `EntryActionDrawer` for loose entries, `GroupActionSheet` for groups. They overlap on pin/status/rename/share/multi-select but diverge: the entry drawer adds comment/tags/event/move-to-group/create-group/move-out-of-group/move-to-space/trash-delete; the group sheet adds dissolve. Groups have no comment thread, tag set, event schedule, or "group of their own" today, so those entry actions are genuinely N/A for a group.

The drawer is a big composable keyed on a `MindDumpEntry` plus a dozen callbacks. Forking it per target type would duplicate ~250 lines; the goal is one drawer that adapts.

## Goals / Non-Goals

**Goals:**
- One long-press drawer for entries and groups; delete `GroupActionSheet`.
- Hide actions that don't apply to a group; keep pin/status/rename/share/multi-select and a destructive "dissolve" action.
- No data-layer change.

**Non-Goals:**
- Group-level tags / events / comments (a real feature, out of scope — these stay entry-only).
- Cross-space group move.
- Trashing a whole group as a unit (today `deleteGroup` soft-deletes the tree; this change surfaces *dissolve*, not trash, for groups, to match "members are preserved"). Existing `deleteGroup` stays available to other callers.

## Decisions

**D1 — Discriminated target, not a fork.**
Add a sealed/generic "drawer target" concept: either `Entry(MindDumpEntry)` or `Group(groupDir: File)`. The drawer takes the target plus the existing callbacks; each action row checks the target kind (and existing `isComment`) to decide visibility. Rejected: a separate `GroupEntryActionDrawer` — pure duplication; rejected: pretending a group is a `MindDumpEntry` — groups have no `tags`/`events`/`type`/`file` content row.

**D2 — Inapplicable rows hidden by target kind.**
- Add comment, edit tags, add event: shown only for `Entry` (already hidden for comments).
- Move to group / create group / move out of group / move to space: shown only for `Entry`.
- Pin / status / rename / share / multi-select: shown for both, bound to group ops when target is `Group`.
- Delete: shown for both. For `Entry` → existing trash path. For `Group` → `deleteGroup` (whole group tree soft-deleted to `.trash/`, restorable as a unit).
- Dissolve: shown ONLY for `Group`. Calls `dissolveGroup` (members moved to parent, empty dir removed; members preserved). Entry target hides it.

**D3 — Header text.**
Entry target: `entry.file.name` (unchanged). Group target: display name = `groupDir.name.substringAfter("-g", groupDir.name)`, falling back to `R.string.group_unnamed` when blank.

**D4 — Confirmation copy differs by target and action.**
- Entry Delete: existing "moved to trash, restorable" copy.
- Group Delete: a confirmation stating the group and its members will be moved to the trash and can be restored.
- Group Dissolve: a confirmation stating members will be moved out (parent location), not deleted.
Add two zh-CN + en strings (group-delete-confirm, group-dissolve-confirm).

**D5 — ViewModel surface.**
Reuse the existing `groupMenuFor: File?` channel (already used to surface the group menu). `MainScreen` reads `groupMenuFor`; when non-null, it opens `EntryActionDrawer` with a `Group` target bound to `toggleGroupPinned` / `setGroupStatus` / `renameGroup` / `shareGroup` / `dissolveGroup` / `enterMultiSelectModeWithGroup`. The entry-action channel (`selectedEntryForAction`) stays for entries. Only one is non-null at a time.

## Risks / Trade-offs

- [Drawer grows a target-kind branch on each row → verbosity] → Acceptable; one branch per row is cheaper than a fork. Encapsulate the "is this row applicable" check in a small helper if it repeats.
- [Dissolve-vs-trash confusion: users may expect delete to trash the group] → The confirmation copy explicitly states members are preserved; dissolve is the safe default. Trashing whole groups can be added later behind the same drawer.
- [Hidden rows change muscle memory] → Only for groups; entry drawer is unchanged.

## Migration Plan

Code-only. Roll back by reverting; repository layer untouched. Verify with `./gradlew detekt ktlintCheck` and `assembleDebug`, then device-smoke both entry and group long-press.
