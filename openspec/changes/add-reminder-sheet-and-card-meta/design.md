# Design — Reminder sheet + card meta surfacing

## Context

The reminder UX (`EventDateTimePicker`) was the minimal viable flow landed by
`add-tag-system`. It works but feels foreign to anyone who has used WeChat's
"提醒" sheet. Separately, the entry-card surface was designed to identify
entries at a glance but only wires pin + todo; the richer sidecar data (tags,
reminders) never reaches the card.

## Approach

### 1. Reminder sheet (replaces `EventDateTimePicker` as the drawer's target)

A `ModalBottomSheet` named `ReminderSheet` (new file or in
`EntryActionDrawer.kt`), with this layout top-to-bottom:

1. **Title row** — `提醒` (zh) / `Reminders` (en).
2. **Existing reminders** — one row per event. Each row shows the bell icon +
   `formatFriendlyDateTime(due)` + a localized state suffix
   (`event_pending` / `event_fired`). Trailing trailing-icon button removes it
   (`onRemove`). Empty list → this section is hidden.
3. **Quick chips** — `FlowRow`:
   - `今天 HH:mm` — today at the next of (now rounded up to next hour, clamped
     to 08:00..22:00; if today is exhausted, the chip is disabled)
   - `明天 09:00`
   - `后天 09:00`
   - `下周一 09:00`
   Each chip directly schedules via `onSchedule(localDateTime)` and dismisses.
4. **自定义…** row — opens the existing `EventDateTimePicker` (kept as a
   sub-picker; the `customDateTime` state lives in the sheet). On confirm the
   same `onSchedule` fires.

The sheet is invoked from `MainScreen.kt` wherever `EventDateTimePicker` is
today (`uiState.eventEditorFor`), and the same `requestNotificationPermission`
flag still triggers the runtime permission ask.

### 2. Remove-a-reminder plumbing

`MindDumpRepository.removeEvent(entry, eventKey)`:
- load meta, drop the matching `EntryEvent` by `key()`, `saveEntryMeta` (which
  deletes the sidecar when the meta becomes empty), then
  `eventScheduler.cancel(entry.file, entry.space, eventKey)`.
- Returns the refreshed entry.

`MindDumpViewModel.removeEntryEvent(entry, event)` mirrors `addEntryEvent` and
also refreshes `selectedEntryForAction` so the drawer/sheet re-render.

### 3. Card footer

A new private composable `EntryCardMetaFooter(entry)` rendered by `EntryItem`
between the per-type content and the comments section, gated on
`!isMultiSelectMode && (entry.tags.isNotEmpty() || entry.events.isNotEmpty())`.
Layout: `FlowRow` with 8dp spacing:

- For each tag: a tonal chip `#tag` (read-only; tapping opens the entry as the
  rest of the card does, so it's not independently clickable).
- If any event exists: one reminder chip `🔔 <friendly>` using
  `nextEventDue`-equivalent selection (soonest pending, else most-recent fired),
  with text `event_pending`/`event_fired` folded into the label via a prefix
  icon. Fired reminders use a de-emphasized alpha.

Same footer (without tags — groups don't carry tags on their *own* sidecar in
the summary card) is **not** added to `GroupSummaryCard` in this change; groups
are out of scope for the card-meta surfacing to keep the patch tight.

## Alternatives considered

- **Inline tag/reminder edit on the card** — rejected; the card is a scan/feed
  surface, edits belong in the drawer.
- **Reusing `EventDateTimePicker` as the whole sheet** — rejected; that's the
  current state and is what we're replacing.

## Open questions

- Default quick-time for "今天": 08:00–22:00 clamp, next-hour. Reasonable
  default; revisit if user feedback asks for "now+1h".
