## Why

Two gaps in the tag/reminder/todo UX surface today:

1. **Reminder authoring is bare-bones.** The current "添加提醒" flow is a raw
   Material `DatePickerDialog` followed by a `TimePicker` `AlertDialog`
   (`EventDateTimePicker`). There is no quick-selection ("今天 / 明天 / 后天"),
   no view of the entry's already-set reminders, and no way to delete one. The
   scheduled-events spec calls for an authoring surface, but the current one
   feels nothing like a modern reminder UX.
2. **Cards hide tags and reminders.** The entry-card spec requires at-a-glance
   identification, yet cards today (`EntryCardHeader`) only surface pin and todo
   status. Tags and reminders — the two pieces of sidecar metadata most worth
   seeing while scanning the feed — are invisible unless the user opens the
   action drawer.

## What Changes

- **WeChat-style reminder sheet.** Replace the two-step Material dialogs with a
  `ModalBottomSheet` ("提醒") that mirrors WeChat: a header, the entry's current
  reminders listed as removable rows, a block of quick-selection chips
  ("今天 HH:mm", "明天 HH:mm", "后天", "下周一"), and a "自定义…" entry that opens
  the existing date+time picker. Quick chips default the time to the next
  sensible slot (e.g. 09:00) and never produce a past time.
- **Card footer chips.** Below the card body, a wrap `FlowRow` of chips shows
  the entry's tags (`#idea`) and its soonest pending (or, if all fired,
  most-recent fired) reminder as `🔔 今天 14:30` / `🔔 已提醒`. The row is
  omitted entirely when the entry has no tags and no events, and is omitted
  during multi-select.
- **Remove-a-reminder plumbing.** `MindDumpRepository` gains `removeEvent(entry,
  eventKey)` (drop the event, persist the sidecar, cancel the alarm); the ViewModel
  exposes `removeEntryEvent`; the sheet's reminder rows use it.
- **No storage-format change.** Tags and events are already on the sidecar; this
  change only reshapes the authoring UI and adds a read-only card presentation.
  No migration, no schema bump.

## Impact

- Affected specs: `scheduled-events` (authoring UI delta), `entry-card` (new
  card-surface requirement for tags + reminders).
- Affected code: `EntryActionDrawer.kt` (new `ReminderSheet`), `MainScreen.kt`
  (wire sheet + remove callback), `EntryItem.kt` (card footer), `MindDumpViewModel.kt`
  and `MindDumpRepository.kt` (remove path), strings (zh + en).
- No breaking changes.
