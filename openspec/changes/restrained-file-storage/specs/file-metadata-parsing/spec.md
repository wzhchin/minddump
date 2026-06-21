## MODIFIED Requirements

### Requirement: FileMetadata value class
The system SHALL provide a `FileMetadata` data class in the `storage` package that encapsulates all metadata extracted from a single file: `entryType`, `timestamp`, `isPinned`, `todoState`, `isEncrypted`, and `rawFileName`. A metadata sidecar file (named `{ownerFileName}.meta[.enc]`) SHALL be recognized as a sidecar and SHALL NOT be returned as an owner entry. There is no separate `role` enum value for comments (removed) or groups (a group is a *directory* detected via `File.isDirectory`, not a filename role). The legacy `文字_*` / `录音_*` / `文件_*` forms remain unparseable and SHALL be skipped.

#### Scenario: Parse a plaintext text entry
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `2506-13-143022-f.md`
- **THEN** the result has `entryType == TEXT`, `timestamp == "2506-13-143022"`, `isPinned == false`, `isEncrypted == false`, `rawFileName == "2506-13-143022-f.md"`

#### Scenario: Parse an encrypted recording
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `2506-13-143045.m4a.enc`
- **THEN** the result has `entryType == RECORDING`, `isEncrypted == true`

#### Scenario: Parse an imported file with original name
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `2506-13-143912-f-report.pdf.enc`
- **THEN** the result has `entryType == FILE`, `isEncrypted == true`, original name "report"

#### Scenario: Parse a pinned, statused, encrypted file
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `9999-2506-13-143022-DONE-f-report.pdf.enc`
- **THEN** the result has `isPinned == true`, `todoState == DONE`, original name "report", extension "pdf", `isEncrypted == true`

#### Scenario: A directory is a group, not a filename role
- **WHEN** the scanner encounters a directory named `2506-13-143912-f-travel/`
- **THEN** it SHALL be treated as a group because `File.isDirectory == true`; no `g` role is parsed from the name, and the directory's members SHALL be collected without recursing for nested sub-groups

#### Scenario: Recognize a metadata sidecar
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `2506-13-143022-f.md.meta`
- **THEN** it SHALL be recognized as a sidecar (suffix `.meta`, optionally `.meta.enc`) and SHALL NOT be returned as an owner entry; it pairs to owner `2506-13-143022-f.md` by stripping the `.meta` suffix

#### Scenario: Status-like text in the original name is not misread
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-f-TODO-list.pdf`
- **THEN** result has `todoState == NONE`, original name "TODO-list" (the word TODO in the name slot is not parsed as a status)

### Requirement: Single parsing entry point
The system SHALL eliminate duplicate timestamp extraction and `.enc` suffix handling. `FileStorageEngine.scanEntries()` and `MindDumpRepository` SHALL use `FileMetadata.fromFile()` as the sole mechanism for extracting metadata from filenames. No other code SHALL manually call `removeSuffix(".enc")` or split on `"_"` to extract timestamps. Sidecar suffix detection (`.meta` / `.meta.enc`) SHALL likewise be centralized in `FileMetadata` / a single `metaFile()` helper, not duplicated.

#### Scenario: FileStorageEngine.scanEntries uses FileMetadata
- **WHEN** `scanEntries(space)` scans a directory containing `2506-13-143022-f.md`
- **THEN** it creates a `MindDumpEntry` using `FileMetadata.fromFile()` to populate `type` and `timestamp`, with no inline `.split("_")` or `.removeSuffix(".enc")` calls

#### Scenario: MindDumpRepository has no extractTimestamp method
- **WHEN** the refactored `MindDumpRepository` is compiled
- **THEN** it contains no private `extractTimestamp()` method — all timestamp data comes from `FileMetadata`

### Requirement: Backward-compatible filename format
The system SHALL NOT migrate old-format files. Files SHALL continue to be named `[9999-]{yymm-dd-HHMMSS}-[STATUS-]f[-{name}].{ext}[.enc]` (the `f` role, pin prefix, status token, and optional original name are unchanged). The only naming changes from the prior spec are the removal of the `g` group role and the `n` comment role, and the sidecar suffix changing from `-m.yaml` to `.meta`.

#### Scenario: New entries use existing naming convention
- **WHEN** a text entry is saved via the refactored code
- **THEN** the file on disk is named `2506-13-143022-f.md` (or `.md.enc` if encrypted), identical in role/pin/status grammar to the current format

### Requirement: Pin and status parsing
The `FileMetadata` parser SHALL additionally extract `isPinned` and `todoState` from a filename. It SHALL accept an optional leading `9999-` pin prefix and an optional status token (`TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`) sitting between the timestamp and the role character. Files lacking both SHALL parse as `isPinned == false` and `todoState == NONE`.

#### Scenario: Parse a pinned file
- **WHEN** `FileMetadata.fromFile(file)` is called on `9999-2506-13-143022-f.md`
- **THEN** result has `timestamp == "2506-13-143022"`, `isPinned == true`, `todoState == NONE`

#### Scenario: Parse a statused file
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-DOING-f.md`
- **THEN** result has `timestamp == "2506-13-143022"`, `isPinned == false`, `todoState == DOING`

#### Scenario: Parse a pinned, statused, encrypted file
- **WHEN** `FileMetadata.fromFile(file)` is called on `9999-2506-13-143022-DONE-f-report.pdf.enc`
- **THEN** result has `isPinned == true`, `todoState == DONE`, original name "report", extension "pdf", `isEncrypted == true`
