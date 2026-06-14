# pin-and-todo Specification

## Purpose
TBD - created by archiving change add-pin-and-todo. Update Purpose after archive.
## Requirements
### Requirement: Pin an entry or group to the top
The system SHALL allow the user to pin a file entry or a group so that it stays at the top of its feed. Pinning is a boolean property of the entry, persisted as a `9999-` prefix on its filename. Pinning SHALL be available for file entries and group directories; it SHALL NOT be available for comments.

#### Scenario: Pin a plain entry
- **WHEN** the user pins a text entry named `2506-13-143022-f.md`
- **THEN** the file is renamed to `9999-2506-13-143022-f.md`

#### Scenario: Unpin reverts the prefix
- **WHEN** the user unpins an entry named `9999-2506-13-143022-f.md`
- **THEN** the file is renamed back to `2506-13-143022-f.md`

#### Scenario: Pin an imported file
- **WHEN** the user pins `2506-13-143912-f-report.pdf`
- **THEN** the file is renamed to `9999-2506-13-143912-f-report.pdf`, preserving its original name and extension

#### Scenario: Pin a group directory
- **WHEN** the user pins a group directory named `2506-13-120000-g-travel`
- **THEN** the directory is renamed to `9999-2506-13-120000-g-travel`

#### Scenario: Comments cannot be pinned
- **WHEN** the user views a comment (`-n-`) entry
- **THEN** no pin affordance is offered

### Requirement: Pinned entries sort above all others
The system SHALL order feeds (the main entry feed and group lists) so that pinned entries appear before unpinned ones, purely as a consequence of the `9999-` filename prefix sorting after every real date. Within the pinned block and within the normal block, entries SHALL be ordered newest-first by filename timestamp.

#### Scenario: Pinned entry floats above a newer normal entry
- **WHEN** the feed contains `9999-2506-13-143022-f.md` (pinned) and `2506-14-091500-f.md` (normal, newer)
- **THEN** the pinned entry is rendered above the newer normal entry

#### Scenario: Two pinned entries sort newest-first among themselves
- **WHEN** the feed contains `9999-2506-13-143022-f.md` and `9999-2506-12-080000-f.md`
- **THEN** the June 13 entry is rendered above the June 12 entry

#### Scenario: Pinned status is reversed by unpin
- **WHEN** an entry is unpinned
- **THEN** it immediately drops back into the normal block ordered by its timestamp

### Requirement: Optional todo status on entries and groups
The system SHALL allow a file entry or group to carry an optional todo status drawn from a fixed set: `TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`. A plain entry has no status. The status is persisted as an uppercase status token between the timestamp and the role character in the filename. Comments SHALL NOT carry a status.

#### Scenario: Mark an entry as TODO
- **WHEN** the user marks `2506-13-143022-f.md` as TODO
- **THEN** the file is renamed to `2506-13-143022-TODO-f.md`

#### Scenario: Change status between states
- **WHEN** the user changes the status of `2506-13-143022-TODO-f.md` to DOING
- **THEN** the file is renamed to `2506-13-143022-DOING-f.md`

#### Scenario: Clear the status
- **WHEN** the user removes the status from `2506-13-143022-DONE-f.md`
- **THEN** the file is renamed to `2506-13-143022-f.md`, and it is no longer considered a todo

#### Scenario: Status and pin coexist
- **WHEN** the user pins `2506-13-143022-TODO-f.md`
- **THEN** the file is renamed to `9999-2506-13-143022-TODO-f.md`, preserving the status

#### Scenario: Status token in a name does not collide
- **WHEN** an imported file is named `TODO-list.pdf` and marked TODO
- **THEN** the result is `2506-13-143022-TODO-f-TODO-list.pdf` — the status token and the literal text "TODO" in the original name are parsed as distinct fields

### Requirement: Todo status does not change feed ordering
The system SHALL NOT reorder the feed based on todo status. Only the pin prefix affects ordering; status is a presentation/semantic property shown as a badge and usable for filtering, but it does not itself move an entry.

#### Scenario: A DONE entry stays in timestamp order
- **WHEN** the feed contains `2506-13-143022-DONE-f.md` and `2506-13-090000-TODO-f.md`
- **THEN** they are ordered by their timestamps regardless of status

### Requirement: Pin and status survive the disk-as-truth rebuild
Because the filesystem is the source of truth, pin and status SHALL be fully recoverable from filenames alone. A rebuild of the Room index from disk SHALL re-derive `isPinned` and `todoState` for every entry without any side store.

#### Scenario: Rebuild recovers pinned status
- **WHEN** the index is rebuilt from a directory containing `9999-2506-13-143022-TODO-f.md`
- **THEN** the resulting entry has `isPinned == true` and `todoState == TODO`

#### Scenario: Rebuild treats plain names as unpinned and statusless
- **WHEN** the index is rebuilt from a directory containing `2506-13-143022-f.md`
- **THEN** the resulting entry has `isPinned == false` and `todoState == NONE`

