## Purpose

Centralized file metadata parsing from filenames, eliminating duplicate timestamp extraction and `.enc` suffix handling across the codebase.

## Requirements

### Requirement: FileMetadata value class
The system SHALL provide a `FileMetadata` data class in the `storage` package that encapsulates all metadata extracted from a single file: `entryType`, `timestamp`, `isEncrypted`, and `rawFileName`.

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
