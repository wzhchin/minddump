## MODIFIED Requirements

### Requirement: Rebuild wipes and repopulates the database from disk
Upon confirmation, the system SHALL clear every row in the `entries` table (and the related `tags`, `events`, and FTS tables; the `comments` table no longer exists), re-scan both Public and Private spaces from disk, insert all discovered entries, and rebuild the FTS full-text index. Files on disk SHALL NOT be read for modification, moved, renamed, or deleted. The re-scan SHALL exclude the `.trash/` directory so that trashed items are never re-indexed. Comment files (`{targetTs}-n-…`) SHALL NOT be indexed (comments are removed).

#### Scenario: Confirm triggers full rebuild
- **WHEN** the user confirms the rebuild
- **THEN** the system SHALL delete all rows from the `entries`, `tags`, and `events` tables
- **AND** the system SHALL re-scan Public and Private spaces from disk
- **AND** the system SHALL rebuild the FTS index to match the content table
- **AND** no file on disk SHALL be modified in any way

#### Scenario: Rebuild derives entry identity from path
- **WHEN** the rebuild re-scans a space
- **THEN** each entry's identity SHALL be its file path relative to `workDir` (the `entries` primary key), derived directly from disk — no synthesized `tid`
- **AND** a note (file) and a group (directory) SHALL be distinguished by whether the path points at a file or a directory

#### Scenario: Rebuild derives entry metadata from disk
- **WHEN** the rebuild re-scans a space
- **THEN** each entry's `groupPath`, `monthFolder`, pin/status (from filename tokens), and `isEncrypted` SHALL be derived from the file/disk state via `FileMetadata` and directory position
- **AND** Private-space entries SHALL be rebuilt without requiring an unlocked session password
- **AND** group directories SHALL be indexed as their own entries
- **AND** each owner entry's tags and events SHALL be read from its paired metadata sidecar (named `{ownerFileName}.meta`) when present, and SHALL be empty when no sidecar exists
- **AND** a Private owner entry whose sidecar is encrypted SHALL have its tags and events marked as encrypted-unread (lazy-decrypted on unlock) rather than read during the cold rebuild

#### Scenario: Rebuild indexes group directories as entries
- **WHEN** the rebuild re-scans a space containing a group directory `2506-13-150000-f-项目A/`
- **THEN** the group SHALL be inserted as an entry row (a directory), with `groupPath == null` (groups live directly under the month bucket)

#### Scenario: Rebuild reads tags and events from a sidecar
- **WHEN** the rebuild re-scans an owner entry `2506-13-143022-f.md` that has a sidecar `2506-13-143022-f.md.meta`
- **THEN** the owner entry's tags and events SHALL be populated from the sidecar

#### Scenario: Rebuild leaves tags empty when no sidecar exists
- **WHEN** the rebuild re-scans an owner entry with no matching `.meta` sidecar
- **THEN** the owner entry's tags and events SHALL be empty

#### Scenario: Rebuild marks Private sidecars as unread until unlock
- **WHEN** the rebuild re-scans a Private owner entry whose sidecar is encrypted
- **AND** the Private space is not unlocked
- **THEN** the entry's tags and events SHALL be marked encrypted-unread and left empty until unlock

#### Scenario: Rebuild detects sidecar-only edits
- **WHEN** an owner file is unchanged but its sidecar was edited since the last rebuild
- **THEN** the rebuild SHALL treat the entry as stale and refresh its tags and events, using `max(owner.lastModified, sidecar.lastModified)` as the staleness comparison

#### Scenario: Rebuild excludes trashed items and comments
- **WHEN** the rebuild re-scans a space whose `.trash/` sibling contains entries, and the space contains legacy `{targetTs}-n-…` comment files
- **THEN** none of the trashed entries SHALL be inserted into the index
- **AND** none of the comment files SHALL be inserted into the index or shown in the feed

#### Scenario: Rebuild cleans orphan sidecars
- **WHEN** the rebuild re-scans a directory containing a sidecar `2506-13-143022-f.md.meta` whose owner `2506-13-143022-f.md` does not exist (e.g. left behind by an interrupted rename)
- **THEN** the orphan sidecar SHALL be ignored (and may be deleted) without error
- **AND** no entry SHALL be created for it

#### Scenario: Rebuild survives same-second entries without collision
- **WHEN** the rebuild re-scans entries created within the same second (in the same or different month buckets)
- **THEN** each SHALL receive a distinct `filePath` primary key and SHALL be inserted independently
- **AND** no entry SHALL overwrite another (there is no synthesized `tid` to collide)
