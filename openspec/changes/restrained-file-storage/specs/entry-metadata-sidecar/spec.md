## MODIFIED Requirements

### Requirement: Metadata sidecar role and pairing

The system SHALL support a metadata sidecar file paired to its owner by the **owner's full filename plus the `.meta` suffix**. A sidecar SHALL be named `{ownerFileName}.meta` in Public space and `{ownerFileName}.meta.enc` in Private space, where `{ownerFileName}` is the owner's complete filename (including its timestamp, status/pin tokens, role, optional name, and extension). The sidecar SHALL live as a sibling file in the same directory as its owner. Because the owner filename is unique within a directory (filesystem-enforced), the pairing is necessarily unique: a same-second note and a same-second group, having different filenames, SHALL pair to different sidecars. A sidecar SHALL NOT be inherited by a group's child entries. (The legacy `{ts}-m.yaml` naming is superseded.)

#### Scenario: Sidecar is a sibling of its owner, named after the owner
- **WHEN** an owner entry `2506-13-143022-f.md` exists with tags
- **THEN** its sidecar SHALL be stored as `2506-13-143022-f.md.meta` in the same directory as the owner

#### Scenario: Sidecar encrypts in Private space
- **WHEN** an owner entry lives in Private space and carries metadata
- **THEN** its sidecar SHALL be named `{ownerFileName}.meta.enc` and SHALL be encrypted with the same mechanism as the owner content

#### Scenario: Sidecar is optional
- **WHEN** an owner entry has no tags and no events
- **THEN** no sidecar file SHALL exist for that entry, and the entry SHALL display and function identically to a tagged entry except for having no tags/events

#### Scenario: Group owns a sidecar
- **WHEN** a group directory `2506-13-150000-f-项目A/` carries metadata
- **THEN** its sidecar SHALL be stored as `2506-13-150000-f-项目A.meta` as a sibling of the group directory (in the parent directory), not nested inside the group

#### Scenario: Same-second note and group get distinct sidecars
- **WHEN** a directory contains both a note `2506-13-150000-f.md` and a group `2506-13-150000-f-旅行/` at the same timestamp
- **THEN** the note's sidecar SHALL be `2506-13-150000-f.md.meta` and the group's sidecar SHALL be `2506-13-150000-f-旅行.meta`, distinct files, each paired unambiguously to its owner by full filename

#### Scenario: Tags on a group do not propagate to children
- **WHEN** a group owns the tag `项目A`
- **AND** a child entry inside the group has no tags
- **THEN** filtering by the tag `项目A` SHALL surface the group entry but SHALL NOT surface the child entry unless the child independently carries the tag

### Requirement: Orphan sidecar handling

A sidecar whose filename (minus `.meta` / `.meta.enc`) has no matching owner entry in its directory SHALL be treated as an orphan. The system SHALL skip/delete orphan sidecars during indexing without raising an error, and SHALL NOT surface them as entries. Because pairing is by exact full filename (not timestamp), there is no ambiguous two-candidate-owner case.

#### Scenario: Sidecar without an owner is ignored
- **WHEN** a directory contains `2506-13-143022-f.md.meta` but no `2506-13-143022-f.md` owner
- **THEN** the sidecar SHALL be ignored (and may be deleted) during scanning
- **AND** no error SHALL be raised

#### Scenario: Crash mid-rename leaves a stale sidecar
- **WHEN** an owner was renamed from `OLD.md` to `NEW.md` but its sidecar was not yet renamed (still `OLD.md.meta`)
- **THEN** the new owner `NEW.md` SHALL read as a bare entry with empty tags/events (its `NEW.md.meta` is absent)
- **AND** the stale `OLD.md.meta` SHALL be treated as an orphan and deleted on the next reconcile, since no `OLD.md` owner exists
- **AND** no owner content SHALL be lost
