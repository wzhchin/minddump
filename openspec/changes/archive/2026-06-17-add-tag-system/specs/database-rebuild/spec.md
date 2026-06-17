## MODIFIED Requirements

### Requirement: Rebuild derives entry metadata from disk
During a rebuild, each entry's `role`, `targetTimestamp`, `groupPath`, `monthFolder`, and `isEncrypted` SHALL be derived from the file/disk state via `FileMetadata`. Group directories SHALL be indexed as entries, tags and events SHALL be read from each owner's paired sidecar when present (and SHALL be empty otherwise), and Private encrypted sidecars SHALL be left unread until unlock.

#### Scenario: Rebuild derives metadata from disk
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
