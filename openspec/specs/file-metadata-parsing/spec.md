## Purpose

Centralized file metadata parsing from filenames, eliminating duplicate timestamp extraction and `.enc` suffix handling across the codebase.
## Requirements
### Requirement: FileMetadata value class
The system SHALL provide a `FileMetadata` data class in the `storage` package that encapsulates all metadata extracted from a single file: `entryType`, `timestamp`, `role`, `isEncrypted`, and `rawFileName`. The `role` SHALL be one of `FILE`, `COMMENT`, `GROUP`, or `META`. A file with the `META` role (`{ts}-m.yaml[.enc]`) SHALL parse with `role == META` and SHALL carry no original name.

#### Scenario: Parse a plaintext text entry
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `文字_143022123.md`
- **THEN** the result has `entryType == TEXT`, `timestamp == "143022123"`, `isEncrypted == false`, `rawFileName == "文字_143022123.md"`

#### Scenario: Parse an encrypted recording
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `录音_143045456.m4a.enc`
- **THEN** the result has `entryType == RECORDING`, `timestamp == "143045456"`, `isEncrypted == true`, `rawFileName == "录音_143045456.m4a.enc"`

#### Scenario: Parse an imported file with original name
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `文件_143089012_report.pdf.enc`
- **THEN** the result has `entryType == FILE`, `timestamp == "143089012"`, `isEncrypted == true`

#### Scenario: Parse a file with no underscore separator
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `orphan.jpg`
- **THEN** the result has `entryType == UNKNOWN`, `timestamp == ""`, `isEncrypted == false`

#### Scenario: Parse a plaintext metadata sidecar
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `2506-13-143022-m.yaml`
- **THEN** the result has `role == META`, `timestamp == "2506-13-143022"`, `isEncrypted == false`

#### Scenario: Parse an encrypted metadata sidecar
- **WHEN** `FileMetadata.fromFile(file)` is called with a file named `2506-13-143022-m.yaml.enc`
- **THEN** the result has `role == META`, `timestamp == "2506-13-143022"`, `isEncrypted == true`

### Requirement: Single parsing entry point
The system SHALL eliminate duplicate timestamp extraction and `.enc` suffix handling. `FileStorageEngine.scanEntries()` and `MindDumpRepository` SHALL use `FileMetadata.fromFile()` as the sole mechanism for extracting metadata from filenames. No other code SHALL manually call `removeSuffix(".enc")` or split on `"_"` to extract timestamps.

#### Scenario: FileStorageEngine.scanEntries uses FileMetadata
- **WHEN** `scanEntries(space)` scans a directory containing `文字_143022123.md`
- **THEN** it creates a `MindDumpEntry` using `FileMetadata.fromFile()` to populate `type` and `timestamp`, with no inline `.split("_")` or `.removeSuffix(".enc")` calls

#### Scenario: MindDumpRepository has no extractTimestamp method
- **WHEN** the refactored `MindDumpRepository` is compiled
- **THEN** it contains no private `extractTimestamp()` method — all timestamp data comes from `FileMetadata`

### Requirement: Backward-compatible filename format
The system SHALL NOT change the on-disk filename format. Files SHALL continue to be named `类型_HHmmssSSS.ext` (or `类型_HHmmssSSS.ext.enc` when encrypted).

#### Scenario: New entries use existing naming convention
- **WHEN** a text entry is saved via the refactored code
- **THEN** the file on disk is named `文字_HHmmssSSS.md` (or `.md.enc` if encrypted), identical to the current format

### Requirement: Pin and status parsing
The `FileMetadata` parser SHALL additionally extract `isPinned` and `todoState` from a filename. It SHALL accept an optional leading `9999-` pin prefix and an optional status token (`TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`) sitting between the timestamp and the role character. Files lacking both SHALL parse as `isPinned == false` and `todoState == NONE`. Comments parse with `isPinned == false` and `todoState == NONE`.

#### Scenario: Parse a pinned file
- **WHEN** `FileMetadata.fromFile(file)` is called on `9999-2506-13-143022-f.md`
- **THEN** result has `timestamp="2506-13-143022"`, `isPinned=true`, `todoState=NONE`

#### Scenario: Parse a statused file
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-DOING-f.md`
- **THEN** result has `timestamp="2506-13-143022"`, `isPinned=false`, `todoState=DOING`

#### Scenario: Parse a pinned, statused, encrypted file
- **WHEN** `FileMetadata.fromFile(file)` is called on `9999-2506-13-143022-DONE-f-report.pdf.enc`
- **THEN** result has `isPinned=true`, `todoState=DONE`, `originalName="report"`, `extension="pdf"`, `isEncrypted=true`, `role=FILE`

#### Scenario: Status-like text in the original name is not misread
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-f-TODO-list.pdf`
- **THEN** result has `todoState=NONE`, `originalName="TODO-list"` (the word TODO in the name slot is not parsed as a status)

#### Scenario: Parse a comment ignores pin and status slots
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-n-2506-13-150000.md`
- **THEN** result has `role=COMMENT`, `isPinned=false`, `todoState=NONE`

