## MODIFIED Requirements

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

#### Scenario: Rebuild excludes trashed items
- **WHEN** the rebuild re-scans a space whose `.trash/` sibling contains entries
- **THEN** none of the trashed entries SHALL be inserted into the index or shown in the feed
