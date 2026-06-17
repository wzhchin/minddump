## MODIFIED Requirements

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
