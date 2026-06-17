# entry-metadata-sidecar Specification

## Purpose
TBD - created by archiving change add-tag-system. Update Purpose after archive.

## Requirements
### Requirement: Metadata sidecar role and pairing

The system SHALL support a metadata sidecar file identified by the `m` filename role. A sidecar SHALL be named `{ts}-m.yaml` in Public space and `{ts}-m.yaml.enc` in Private space, and SHALL be paired to its owner entry by **timestamp alignment**: the sidecar carries the same timestamp as its owner and lives as a sibling file in the same directory. One timestamp within a single directory SHALL correspond to exactly one owner entry plus at most one sidecar. A sidecar SHALL NOT be inherited by a group's child entries.

#### Scenario: Sidecar is a sibling of its owner
- **WHEN** an owner entry `2506-13-143022-f.md` exists with tags
- **THEN** its sidecar SHALL be stored as `2506-13-143022-m.yaml` in the same directory as the owner

#### Scenario: Sidecar encrypts in Private space
- **WHEN** an owner entry lives in Private space and carries metadata
- **THEN** its sidecar SHALL be named `{ts}-m.yaml.enc` and SHALL be encrypted with the same mechanism as the owner content

#### Scenario: Sidecar is optional
- **WHEN** an owner entry has no tags and no events
- **THEN** no sidecar file SHALL exist for that entry, and the entry SHALL display and function identically to a tagged entry except for having no tags/events

#### Scenario: Group owns a sidecar
- **WHEN** a group directory `2506-13-150000-g-项目A/` carries metadata
- **THEN** its sidecar SHALL be stored as `2506-13-150000-m.yaml` as a sibling of the group directory (in the parent directory), not nested inside the group

#### Scenario: Tags on a group do not propagate to children
- **WHEN** a group owns the tag `项目A`
- **AND** a child entry inside the group has no tags
- **THEN** filtering by the tag `项目A` SHALL surface the group entry but SHALL NOT surface the child entry unless the child independently carries the tag

### Requirement: Orphan sidecar handling

A sidecar whose timestamp has no matching owner entry in its directory SHALL be treated as an orphan. The system SHALL skip orphan sidecars during indexing without raising an error, and SHALL NOT surface them as entries.

#### Scenario: Sidecar without an owner is ignored
- **WHEN** a directory contains `2506-13-143022-m.yaml` but no `2506-13-143022` owner entry
- **THEN** the sidecar SHALL be ignored during scanning
- **AND** no error SHALL be raised

#### Scenario: Sidecar with two candidate owners is ignored
- **WHEN** a directory contains two owner entries sharing the same timestamp (an anomalous state) and one sidecar at that timestamp
- **THEN** the sidecar SHALL be treated as an orphan and skipped, because pairing is ambiguous

### Requirement: Private sidecar lazy decryption

In Private space, sidecar contents SHALL NOT be decrypted until the Private space is unlocked with the session credential. While Private is locked, a Private owner entry SHALL present empty tags and no events, and SHALL be marked as having an encrypted (unread) sidecar; Private tags and events SHALL NOT be queryable or schedulable while locked.

#### Scenario: Locked Private entry shows no tags
- **WHEN** the Private space is locked
- **AND** an owner entry has an encrypted sidecar carrying tags
- **THEN** the entry SHALL display with no tags
- **AND** searching or filtering by those tags SHALL NOT surface the entry

#### Scenario: Unlocking Private populates tags and events
- **WHEN** the user unlocks the Private space
- **THEN** all Private sidecars SHALL be decrypted
- **AND** each owner entry's tags and events SHALL become available for display, filtering, and scheduling

### Requirement: Sidecar schema

A sidecar SHALL be a YAML document with two optional top-level fields: `tags` (a flat list of strings) and `events` (a list of event objects). Either field MAY be absent or empty. The system SHALL treat a malformed sidecar as empty metadata rather than crashing, and SHALL log the parse failure.

#### Scenario: Sidecar with tags only
- **WHEN** a sidecar contains `tags: [idea, 项目A]` and no `events`
- **THEN** the owner entry SHALL have tags `idea` and `项目A` and no events

#### Scenario: Sidecar with events only
- **WHEN** a sidecar contains an `events` list and no `tags`
- **THEN** the owner entry SHALL have those events and no tags

#### Scenario: Malformed sidecar is ignored
- **WHEN** a sidecar contains unparseable content
- **THEN** the owner entry SHALL be treated as having no tags and no events
- **AND** the application SHALL NOT crash
- **AND** the failure SHALL be logged
