## ADDED Requirements

### Requirement: Tag is a filter dimension

The entry list SHALL support filtering by tag as a composable dimension alongside existing search. Tag filtering SHALL operate on the index populated from sidecars (see database-rebuild). Tag filtering SHALL respect the current space boundary exactly as search does, and SHALL NOT surface Private entries whose sidecars are still encrypted.

#### Scenario: Filter list by a tag
- **WHEN** the user applies a tag filter for "idea" in the current space
- **THEN** only entries in the current space carrying the "idea" tag SHALL be shown

#### Scenario: Locked Private entries are not surfaced by tag
- **WHEN** the user filters by a tag while in the Private space but the space is locked
- **THEN** no Private entries SHALL be surfaced, because their sidecars are not yet decrypted
