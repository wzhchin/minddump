## 1. Remove-a-reminder plumbing

- [ ] 1.1 `MindDumpRepository.removeEvent(entry, eventKey)`: load meta → drop matching `EntryEvent` by `key()` → `saveEntryMeta` (auto-deletes empty sidecar) → `eventScheduler.cancel`
- [ ] 1.2 `MindDumpViewModel.removeEntryEvent(entry, event)`: calls repo, refreshes `selectedEntryForAction` if it matches, `refreshEntries()`

## 2. Reminder bottom sheet

- [ ] 2.1 New composable `ReminderSheet(events, onSchedule, onRemove, onDismiss)` as a `ModalBottomSheet`
- [ ] 2.2 Existing-reminders section: one row per event (bell + friendly due + state suffix + remove affordance); hidden when empty
- [ ] 2.3 Quick chips `FlowRow`: 今天 / 明天 / 后天 / 下周一 — each computes a non-past local time and calls `onSchedule`, then dismisses
- [ ] 2.4 "自定义…" row toggles the existing `EventDateTimePicker` as a sub-picker; on confirm calls `onSchedule`
- [ ] 2.5 Replace `EventDateTimePicker` usage in `MainScreen.kt` (`uiState.eventEditorFor`) with `ReminderSheet`, wiring `onRemove` to `viewModel.removeEntryEvent`

## 3. Card footer

- [ ] 3.1 New private composable `EntryCardMetaFooter(entry)` rendering a `FlowRow`: tag chips (`#tag`, tonal) + one reminder chip (bell + friendly due + fired de-emphasis)
- [ ] 3.2 Render the footer in `EntryItem` between per-type content and comments, gated on `!isMultiSelectMode && (tags or events non-empty)`
- [ ] 3.3 Reminder chip text uses soonest-pending (else most-recent fired) selection; reuse the `nextEventDue` helper from the drawer

## 4. Strings + l10n

- [ ] 4.1 Add zh strings: `reminder_sheet_title`, `reminder_custom`, `reminder_today`, `reminder_tomorrow`, `reminder_day_after`, `reminder_next_monday`
- [ ] 4.2 Add en counterparts in `values-en/strings.xml`

## 5. Verify

- [ ] 5.1 `./gradlew detekt ktlintCheck`
- [ ] 5.2 Manual smoke: add via quick chip, add via custom, remove existing, footer shows/hides correctly
