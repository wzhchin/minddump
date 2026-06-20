## MODIFIED Requirements

### Requirement: Rebuild wipes and repopulates the database from disk

Upon confirmation, the system SHALL clear every row in the `entries`, `comments`, `tags`, and `events` tables, re-scan both Public and Private spaces from disk, insert all discovered entries into the relation appropriate to their kind, and rebuild the FTS full-text index. Files on disk SHALL NOT be read for modification, moved, renamed, or deleted. The re-scan SHALL exclude the `.trash/` directory so that trashed items are never re-indexed.

The index SHALL assign each entry a `tid` (its identity) derived deterministically from the file's second-resolution timestamp plus its same-second collision suffix, so that an entry's `tid` is identical across rebuilds. The rebuild SHALL insert in two passes per space: first all group rows (ancestor-first, so each group's `parentId` can resolve to an already-inserted ancestor), then all member rows whose `parentId` resolves to the group row whose `filePath` equals the member's parent directory. Tags and events SHALL be read from each owner's paired metadata sidecar into the `tags` and `events` tables respectively.

#### Scenario: Confirm triggers full rebuild
- **WHEN** the user confirms the rebuild
- **THEN** the system SHALL delete all rows from the `entries`, `comments`, `tags`, and `events` tables
- **AND** the system SHALL re-scan Public and Private spaces from disk
- **AND** the system SHALL rebuild the FTS index to match the content table
- **AND** no file on disk SHALL be modified in any way

#### Scenario: Rebuild derives entry metadata from disk
- **WHEN** the rebuild re-scans a space
- **THEN** each entry's `type`, `parentId`, `monthFolder`, and `isEncrypted` SHALL be derived from the file/disk state via `FileMetadata`
- **AND** Private-space entries SHALL be rebuilt without requiring an unlocked session password
- **AND** group directories SHALL be indexed as their own `entries` rows with `type == GROUP`
- **AND** each owner entry's tags and events SHALL be read from its paired metadata sidecar into the `tags` and `events` tables when present, and SHALL be empty when no sidecar exists
- **AND** a Private owner entry whose sidecar is encrypted SHALL have `metaEncrypted` set and its tags and events left empty (lazy-decrypted on unlock) rather than read during the cold rebuild

#### Scenario: Rebuild assigns a rebuild-stable tid
- **WHEN** the rebuild re-scans a file `2506-13-143022-f.md`
- **THEN** the entry SHALL receive a `tid` equal to the epoch-millis of `2026-06-13 14:30:22` (padded to millis)
- **AND** re-running the rebuild SHALL assign the same `tid` to that file again

#### Scenario: Rebuild distinguishes same-second entries by collision suffix
- **WHEN** the rebuild re-scans `2506-13-143022-f.md` and `2506-13-143022-f_1.md` created in the same second
- **THEN** the first SHALL receive the base epoch-millis `tid` and the second SHALL receive that value plus one
- **AND** both tids SHALL be unique within the space

#### Scenario: Rebuild resolves parentId for group members in two passes
- **WHEN** the rebuild re-scans a space containing a group `2506-13-150000-g-项目A/` with a member file inside it
- **THEN** the group SHALL be inserted first as a row with `type == GROUP`
- **AND** the member file SHALL be inserted afterward with `parentId` equal to the group's `tid`
- **AND** a nested sub-group's `parentId` SHALL equal its parent group's `tid`

#### Scenario: Rebuild indexes group directories as entries
- **WHEN** the rebuild re-scans a space containing a group directory `2506-13-150000-g-项目A/`
- **THEN** the group SHALL be inserted as an entry row with `type == GROUP`

#### Scenario: Rebuild reads tags and events into their own tables
- **WHEN** the rebuild re-scans an owner entry that has a sidecar with tags and events
- **THEN** the `tags` table SHALL contain one row per tag, keyed by the owner's `tid`
- **AND** the `events` table SHALL contain one row per event, keyed by the owner's `tid`

#### Scenario: Rebuild leaves tags empty when no sidecar exists
- **WHEN** the rebuild re-scans an owner entry with no sidecar
- **THEN** no `tags` and no `events` rows SHALL exist for that owner's `tid`

#### Scenario: Rebuild marks Private sidecars as unread until unlock
- **WHEN** the rebuild re-scans a Private owner entry whose sidecar is encrypted
- **AND** the Private space is not unlocked
- **THEN** the entry's `metaEncrypted` SHALL be set and no `tags`/`events` rows SHALL be created until unlock

#### Scenario: Rebuild detects sidecar-only edits
- **WHEN** an owner file is unchanged but its sidecar was edited since the last rebuild
- **THEN** the rebuild SHALL treat the entry as stale and refresh its `tags` and `events` rows, using `max(owner.lastModified, sidecar.lastModified)` as the staleness comparison

#### Scenario: Rebuild excludes trashed items
- **WHEN** the rebuild re-scans a space whose `.trash/` sibling contains entries
- **THEN** none of the trashed entries SHALL be inserted into any index table or shown in the feed

#### Scenario: Rebuild populates comments into their own table
- **WHEN** the rebuild re-scans an owner entry that has one or more comment files
- **THEN** each comment SHALL be inserted as a row in the `comments` table with its own `tid` and a `targetTid` equal to the owner's `tid`
