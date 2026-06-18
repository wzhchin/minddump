## Why

The entry action drawer now carries 12 actions as full-width rows, making it tall
and crowded: every action competes for the same vertical space regardless of
whether it needs a text label. Quick, label-free actions (pin, comment, share,
rename, multi-select, move) waste space as full rows, while the actions that
actually carry useful state — the current todo status, existing tags, and the
scheduled reminder time — are buried among them. The drawer should surface that
state at a glance instead of forcing the user to scan a wall of identical rows.

## What Changes

- Reorganize the drawer into **two tiers**: a compact icon-only quick-action bar
  at the top, and full-width detail rows below for actions that summarize entry
  state.
- **Quick-action bar** (icon buttons that wrap to a second row as needed, no
  visible text labels): pin/unpin, add comment, share, rename, multi-select,
  move to group, move out of group, move to opposite space.
- **Full-width rows retained** (each can show a trailing state summary): todo
  status (current status), add tags (existing tags), add reminder (the scheduled
  time), delete.
- The **reminder row's trailing summary now shows the formatted scheduled time**
  instead of a bare "pending/fired" word, so the user sees *when* the reminder
  fires without opening the picker.
- A **pinned entry gets a filled-tint visual cue** on the pin icon button.
- Move the hardcoded zh-CN action labels (rename, multi-select, move to group,
  move out of group, move to private/public) into `strings.xml` with English
  counterparts; these now serve as the icon buttons' accessibility labels.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `entry-actions-drawer`: presentation reorganized into a quick-action icon bar
  plus detail rows; the reminder row shows the scheduled time; the pin icon
  reflects pinned state visually. New "Tag action" and "Scheduled reminder
  action" requirements fill existing spec gaps for two actions that were added
  in code (metadata sidecar) but never specified at the drawer surface.

## Impact

- `app/src/main/java/com/chin/minddump/ui/components/EntryActionDrawer.kt` —
  layout refactor (icon bar + retained detail rows), reminder time formatting,
  pin visual cue.
- `app/src/main/res/values/strings.xml` (zh-CN) and
  `app/src/main/res/values-en/strings.xml` (en) — new action-label strings.
- **No** data, schema, or migration changes. Callback signatures, conditional
  visibility, and all sub-dialogs (rename, group picker, delete confirmation,
  todo-status chooser, comment dialog) are unchanged.
