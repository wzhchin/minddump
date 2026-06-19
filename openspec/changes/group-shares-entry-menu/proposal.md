## Why

Today a long-press on a loose entry opens the rich `EntryActionDrawer` (pin, status, rename, share, multi-select, move-to-group, create-group, move-out-of-group, move-to-space, add comment, edit tags, add event, delete), but a long-press on a group opens a far thinner `GroupActionSheet` (pin, status, rename, share, multi-select, dissolve). Groups are conceptually "a note that bundles other notes," yet they get a second-class menu. Users have to remember two different action surfaces for the same kind of object.

## What Changes

- **One action sheet for entries and groups.** The long-press action surface SHALL be the `EntryActionDrawer` for both loose entries and groups. The separate `GroupActionSheet` is removed.
- **Group-targeted drawer reuses entry-shaped callbacks.** When the drawer is opened for a group, it is driven by the same callback shape (pin, status, rename, share, multi-select, delete) bound to group operations. Group name replaces the file-name header.
- **Inapplicable actions are hidden for groups, not shown disabled.** The drawer SHALL omit actions that have no group analogue: add comment, edit tags, add event, move-to-group, create-group, move-out-of-group. These are entry-specific (a group is not a single file and has no comment thread / tag set / event schedule / group-of-its-own today).
- **Groups keep a separate "解散" (dissolve) action.** A group has one extra semantics an entry does not: dissolving. The shared drawer SHALL show a distinct "解散" action for group targets that moves the group's members to the parent location (month dir at root, parent group when nested) and removes the now-empty group directory — the existing `dissolveGroup` semantics, members preserved. The confirmation copy SHALL make clear members are moved out, not deleted.
- **"删除" (delete) for a group soft-deletes the whole group tree** via the existing `deleteGroup` (members travel with the group into `.trash/` and restore together), paralleling how entry delete trashes the entry. Delete and dissolve are two separate actions: delete removes the group AND its members to trash; dissolve scatters the members back out and removes only the empty folder.
- **Move-to-space stays omitted for groups** (a group lives in one space; cross-space group move is out of scope). This keeps the drawer consistent with "hide what doesn't apply."
- **Group header in the drawer shows the group's display name** (the post-`-g` segment, falling back to the unnamed-group placeholder), not the raw directory filename.

**Non-breaking at the data layer:** no schema, filename, or Room change. `dissolveGroup`, `renameGroup`, `setGroupPinned`, `setGroupStatus`, `shareGroup` already exist in the repository and are reused. The `GroupActionSheet` composable is deleted.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `entry-actions-drawer`: the action drawer SHALL also be the long-press surface for groups; document which actions are hidden when the target is a group, and add the group-only "解散" (dissolve) action alongside the shared destructive "删除" action.
- `group-card`: the long-press interaction opens the shared entry action drawer (not a separate group sheet); pin/status/rename/share/multi-select operate on the group as before; the group gains a dissolve action in the drawer.
- `entry-card`: no requirement change (loose-entry drawer behavior is unchanged), referenced only to confirm parity.

## Impact

- **Code:** `ui/components/EntryActionDrawer.kt` (accept a group target — either a discriminated parameter or a thin wrapper that adapts group callbacks to the drawer's entry-shaped callbacks, hiding inapplicable rows and adding the group-only "解散" action), `ui/MainScreen.kt` (route `onGroupLongClick` to open the drawer with a group target; delete the `GroupActionSheet` call site), delete `GroupActionSheet`. `ui/MindDumpViewModel.kt` may gain a single `selectedGroupForAction: File?` state mirroring `selectedEntryForAction`, or reuse the existing group-menu path to surface a group in the drawer; `deleteGroup` (whole-tree soft delete) and `dissolveGroup` are both reused from the repository.
- **Resources:** `res/values/strings.xml` + `values-en/strings.xml` — a "解散" action label and a dissolve-confirm copy (e.g. "解散分组？组内成员将移出，不会被删除。"); reuse existing `group_unnamed` for the header placeholder. Reuse the existing entry-delete confirmation copy for the group delete action.
- **Data:** None. No on-disk schema/filename change; repository operations reused as-is. No Room migration.
- **Build/verify:** `./gradlew detekt ktlintCheck` and `assembleDebug` after implementation; device smoke: long-press a group opens the same drawer as an entry; hidden rows are absent; delete dissolves the group and preserves members.
- **Risk:** Medium. The drawer currently assumes a `MindDumpEntry`; adapting it to also accept a `File` group target without duplicating the whole composable is the main design point (see design.md — prefer a small adapter rather than forking the drawer).
