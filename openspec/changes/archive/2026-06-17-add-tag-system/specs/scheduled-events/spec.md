## ADDED Requirements

### Requirement: Event authoring

The user SHALL be able to schedule a time-based event on any owner entry (text, file, or group), regardless of whether the entry carries a todo status. An event SHALL be stored in the owner's metadata sidecar as a list item under `events`, with a `due` local time, a `state`, and a `trigger` of `once`. Events SHALL be decoupled from the filename's todo-state token: an entry MAY have events without any todo status, and an entry with a todo status MAY have no events.

#### Scenario: Schedule an event on an entry without a todo status
- **WHEN** the user sets an event due "2026-06-20T09:00" on a plain entry with no todo status
- **THEN** the sidecar SHALL contain an event with `due: 2026-06-20T09:00`, `state: pending`, `trigger: once`
- **AND** the entry's filename todo-state token SHALL remain absent

#### Scenario: Done entry may still carry a future event
- **WHEN** an entry marked DONE has a pending event for next week
- **THEN** the event SHALL remain scheduled and SHALL fire at its due time

### Requirement: Event fires at the precise due time

A pending event SHALL fire via an exact alarm at its local `due` time, even when the device is in Doze. When an event fires, the system SHALL post a user-visible notification on the high-priority Reminders channel and SHALL transition the event's `state` from `pending` to `fired`. A fired event SHALL NOT fire again.

#### Scenario: Event fires on time under Doze
- **WHEN** a pending event is due now and the device is in Doze
- **THEN** the notification SHALL be posted at the due time
- **AND** the event's `state` SHALL become `fired`

#### Scenario: Fired event does not refire
- **WHEN** an event whose `state` is `fired` is encountered during scheduling
- **THEN** no alarm SHALL be registered for it

### Requirement: Events survive device restart

Because exact alarms are cleared on reboot, the system SHALL re-register every `pending` Public-space event from the index after a `BOOT_COMPLETED` broadcast.

#### Scenario: Restart re-registers public events
- **WHEN** the device reboots
- **THEN** every Public-space event with `state: pending` SHALL be re-registered as an exact alarm

### Requirement: Private events are silent while locked

A Private-space event SHALL NOT fire while the Private space is locked. When the Private space is unlocked, all Private sidecars SHALL be decrypted and every Private event with a future `due` SHALL be registered; an event whose `due` already passed while the space was locked SHALL NOT retroactively fire.

#### Scenario: Private event does not fire while locked
- **WHEN** a Private event's `due` time arrives while the Private space is locked
- **THEN** no notification SHALL be posted

#### Scenario: Unlocking registers future private events only
- **WHEN** the user unlocks Private after one event's `due` already passed and another's is in the future
- **THEN** the future event SHALL be registered as an alarm
- **AND** the past event SHALL NOT retroactively fire

### Requirement: Notification deep-links to the owning entry

Tapping an event notification SHALL open the app and navigate to the owning entry's detail screen.

#### Scenario: Tap notification opens the entry
- **WHEN** the user taps an event's notification
- **THEN** the app SHALL open to the detail screen of the entry that owns the event

### Requirement: Notification permissions and channels

The system SHALL declare the `USE_EXACT_ALARM` permission and SHALL request `POST_NOTIFICATIONS` at runtime before the user first schedules an event. The system SHALL provide two notification channels: a high-priority "提醒 / Reminders" channel for event firings and a low-priority "汇总 / Digest" channel reserved for future summary/background jobs. Both channels SHALL be created at app startup.

#### Scenario: Notification permission requested before scheduling
- **WHEN** the user attempts to schedule their first event and has not yet granted notification permission
- **THEN** the system SHALL request the `POST_NOTIFICATIONS` permission

#### Scenario: Two channels created at startup
- **WHEN** the application starts
- **THEN** a high-importance Reminders channel and a low-importance Digest channel SHALL both exist

### Requirement: Localization of event and notification strings

All event-authoring and notification user-facing strings SHALL be provided in both the default zh-CN locale (`values/strings.xml`) and the en locale (`values-en/strings.xml`).

#### Scenario: English locale for event strings
- **WHEN** the device locale is English
- **THEN** the Reminders and Digest channel names, event authoring UI labels, and notification text SHALL render in English
