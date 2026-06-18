## Requirement: Reminder authoring uses a WeChat-style sheet

The reminder authoring surface SHALL be a modal bottom sheet titled with the localized word for "reminder", presenting (top-to-bottom): the entry's existing reminders as removable rows, a row of quick-selection chips, and a "custom…" affordance that opens the existing date+time picker. Each existing-reminder row SHALL display the due time (friendly local format) and a localized state suffix, with a remove affordance. Quick chips SHALL schedule a sensible near-future local time (today / tomorrow / the day after / next Monday) without requiring the user to pick a time.

#### Scenario: Sheet opens when the user taps "add reminder"
- **WHEN** the user taps the "add reminder" action in the entry drawer
- **THEN** a modal bottom sheet titled "提醒 / Reminders" SHALL open
- **AND** the sheet SHALL list the entry's existing reminders, each removable

#### Scenario: Quick chip schedules today
- **WHEN** the sheet is open and the user taps the "今天 HH:mm" chip
- **THEN** a pending event SHALL be created for a time today that is not in the past
- **AND** the sheet SHALL dismiss

#### Scenario: Custom picker still reachable
- **WHEN** the user taps "自定义…" in the reminder sheet
- **THEN** the existing date+time picker SHALL open
- **AND** confirming it SHALL schedule an event and dismiss the sheet

#### Scenario: Removing an existing reminder cancels its alarm
- **WHEN** the user taps the remove affordance on an existing-reminder row
- **THEN** that event SHALL be removed from the sidecar
- **AND** its registered alarm SHALL be cancelled
- **AND** the row SHALL disappear from the sheet

#### Scenario: Empty entry shows only the quick options
- **WHEN** the sheet is open for an entry with no events
- **THEN** no existing-reminder rows SHALL be shown
- **AND** the quick-selection chips and "自定义…" SHALL still be offered
