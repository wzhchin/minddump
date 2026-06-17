# entry-tags Specification

## Purpose
TBD - created by archiving change add-tag-system. Update Purpose after archive.

## Requirements
### Requirement: Tag authoring

The user SHALL be able to add and remove tags on any owner entry (text, file, or group) from the entry's detail/edit surface. A tag SHALL be a short string. Tags SHALL be stored in the owner's metadata sidecar (see entry-metadata-sidecar).

#### Scenario: Add a tag to an entry
- **WHEN** the user adds the tag "idea" to an entry that had no tags
- **THEN** a sidecar SHALL be created (if absent) for that entry with `tags: [idea]`
- **AND** the entry SHALL display the "idea" tag

#### Scenario: Remove a tag from an entry
- **WHEN** the user removes a tag from an entry
- **THEN** that tag SHALL no longer appear on the entry
- **AND** the sidecar SHALL be updated to reflect the remaining tags

#### Scenario: Removing the last tag removes the tags field
- **WHEN** the user removes the last tag from an entry that also has no events
- **THEN** no sidecar SHALL remain for that entry (no empty sidecar is kept)

### Requirement: Tag character set and deduplication

A tag SHALL accept letters (Latin and CJK), digits, and hyphens. A tag SHALL NOT contain spaces, the `#` character, or `/`. Tags SHALL be stored without a leading `#`; the UI SHALL prepend `#` only for display. Duplicate detection SHALL be case-insensitive (`项目A` and `项目a` are the same tag), but the original casing of the first-added form SHALL be preserved for display.

#### Scenario: Tag with a space is rejected
- **WHEN** the user attempts to add a tag containing a space
- **THEN** the tag SHALL NOT be added
- **AND** the user SHALL be informed that spaces are not allowed

#### Scenario: Tags differing only in case are deduplicated
- **WHEN** an entry already has the tag "Idea"
- **AND** the user adds "idea"
- **THEN** the entry SHALL have a single tag displayed as "Idea"

#### Scenario: Display shows a leading hash
- **WHEN** an entry has the tag "idea"
- **THEN** the entry's tag chip SHALL display "#idea"

### Requirement: Tag filtering across entries

The user SHALL be able to filter the entry list by one tag, and the filtered list SHALL surface every owner entry in the current space that carries that tag. Tag filtering SHALL respect the current space boundary exactly as search does.

#### Scenario: Filter by a tag
- **WHEN** the user applies a tag filter for "idea"
- **THEN** only entries carrying the "idea" tag in the current space SHALL be shown

#### Scenario: Cross-space isolation
- **WHEN** the user filters by a tag while in the Public space
- **THEN** no Private-space entry SHALL appear in the filtered results
