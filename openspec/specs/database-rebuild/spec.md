# database-rebuild Specification

## Purpose
TBD - created by archiving change add-database-rebuild. Update Purpose after archive.
## Requirements
### Requirement: Rebuild entry in Settings

The Settings dialog SHALL expose a "重建数据库" (Rebuild database) action below the work-directory control, accompanied by a short description stating that the rebuild reconstructs the SQLite index from files on disk and does not modify the files themselves.

#### Scenario: Open Settings shows rebuild action
- **WHEN** the user opens the Settings dialog
- **THEN** the dialog SHALL display a "重建数据库" button
- **AND** the dialog SHALL display a description explaining that only the index is rebuilt and files are untouched

#### Scenario: Tap rebuild shows confirmation
- **WHEN** the user taps the "重建数据库" button
- **THEN** a confirmation dialog SHALL appear titled "重建数据库？"
- **AND** the confirmation message SHALL state that the operation clears and rebuilds the index database, keeps files unchanged, and cannot be undone

#### Scenario: Cancel confirmation
- **WHEN** the user dismisses the confirmation dialog
- **THEN** no database changes SHALL occur
- **AND** the Settings dialog SHALL remain open

### Requirement: Rebuild wipes and repopulates the database from disk

Upon confirmation, the system SHALL clear every row in the `entries` table, re-scan both Public and Private spaces from disk, insert all discovered entries, and rebuild the FTS full-text index. Files on disk SHALL NOT be read for modification, moved, renamed, or deleted. The re-scan SHALL exclude the `.trash/` directory so that trashed items are never re-indexed.

#### Scenario: Confirm triggers full rebuild
- **WHEN** the user confirms the rebuild
- **THEN** the system SHALL delete all rows from the `entries` table
- **AND** the system SHALL re-scan Public and Private spaces from disk
- **AND** the system SHALL rebuild the FTS index to match the content table
- **AND** no file on disk SHALL be modified in any way

#### Scenario: Rebuild derives entry metadata from disk
- **WHEN** the rebuild re-scans a space
- **THEN** each entry's `role`, `targetTimestamp`, `groupPath`, `monthFolder`, and `isEncrypted` SHALL be derived from the file/disk state via `FileMetadata`
- **AND** Private-space entries SHALL be rebuilt without requiring an unlocked session password
- **AND** group directories (`role == GROUP`) SHALL be indexed as their own entries
- **AND** each owner entry's tags and events SHALL be read from its paired metadata sidecar when present, and SHALL be empty when no sidecar exists
- **AND** a Private owner entry whose sidecar is encrypted SHALL have its tags and events marked as encrypted-unread (lazy-decrypted on unlock) rather than read during the cold rebuild

#### Scenario: Rebuild indexes group directories as entries
- **WHEN** the rebuild re-scans a space containing a group directory `2506-13-150000-g-项目A/`
- **THEN** the group SHALL be inserted as an entry row with `role == GROUP`

#### Scenario: Rebuild reads tags and events from a sidecar
- **WHEN** the rebuild re-scans an owner entry that has a sidecar with tags and events
- **THEN** the owner entry's tags and events SHALL be populated from the sidecar

#### Scenario: Rebuild leaves tags empty when no sidecar exists
- **WHEN** the rebuild re-scans an owner entry with no sidecar
- **THEN** the owner entry's tags and events SHALL be empty

#### Scenario: Rebuild marks Private sidecars as unread until unlock
- **WHEN** the rebuild re-scans a Private owner entry whose sidecar is encrypted
- **AND** the Private space is not unlocked
- **THEN** the entry's tags and events SHALL be marked encrypted-unread and left empty until unlock

#### Scenario: Rebuild detects sidecar-only edits
- **WHEN** an owner file is unchanged but its sidecar was edited since the last rebuild
- **THEN** the rebuild SHALL treat the entry as stale and refresh its tags and events, using `max(owner.lastModified, sidecar.lastModified)` as the staleness comparison

#### Scenario: Rebuild excludes trashed items
- **WHEN** the rebuild re-scans a space whose `.trash/` sibling contains entries
- **THEN** none of the trashed entries SHALL be inserted into the index or shown in the feed

### Requirement: Rebuild progress is non-cancellable

While the rebuild is running, the dialog SHALL show an indeterminate progress indicator with a "正在重建…" message. The dialog SHALL NOT be dismissible by the user or by tapping outside until the rebuild completes.

#### Scenario: Running state blocks dismissal
- **WHEN** the rebuild is in progress
- **THEN** the dialog SHALL render an indeterminate spinner with "正在重建…"
- **AND** tapping outside or pressing back SHALL NOT dismiss the dialog

### Requirement: Rebuild reports the resulting entry count

When the rebuild completes, the system SHALL dismiss the progress dialog and show a Snackbar stating the total number of entries now in the database, formatted as "已重建数据库，共 N 条记录".

#### Scenario: Success Snackbar
- **WHEN** the rebuild finishes successfully
- **THEN** the progress dialog SHALL close
- **THEN** the current entry list and group summaries SHALL refresh to reflect the rebuilt state
- **AND** a Snackbar SHALL show "已重建数据库，共 {count} 条记录" with the total entry count

### Requirement: Localization of rebuild strings

All rebuild-related user-facing strings SHALL be provided in both the default zh-CN locale (`values/strings.xml`) and the en locale (`values-en/strings.xml`).

#### Scenario: English locale
- **WHEN** the device locale is English
- **THEN** the rebuild button, confirmation, progress message, and success Snackbar SHALL render in English ("Rebuild database", "Rebuilding…", "Database rebuilt: N entries", etc.)

