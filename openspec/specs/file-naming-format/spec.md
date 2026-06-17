## Purpose

File naming convention, monthly directory layout, type detection by extension, and FileMetadata parsing for the new MindDump file format.
## Requirements
### Requirement: File naming format
The system SHALL use `[9999-][yymm-dd-HHMMSS]-[STATUS]-f[-{originalName}].{extension}[.enc]` for user-managed files, where the `9999-` prefix is an optional pin sentinel and `[STATUS]` is an optional todo-status token (`TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`) followed by `-`. Timestamp format is `yymm-dd-HHMMSS` (e.g., `2506-13-143022`). When neither pin nor status is present, the name collapses to the legacy `{ts}-f[-{name}].{ext}[.enc]` form. Comments use `{ts}-n-{ts}.md[.enc]` and never carry a pin prefix or status token. Metadata sidecars use `{ts}-m.yaml[.enc]` and never carry a pin prefix or status token; the sidecar's `m` role pairs it to an owner entry that shares its timestamp.

#### Scenario: Text entry
- **WHEN** a text entry is saved
- **THEN** the file is named `{ts}-f.md` (e.g., `2506-13-143022-f.md`) — no pin, no status

#### Scenario: Photo capture
- **WHEN** a photo is captured
- **THEN** the file is named `{ts}-f.jpg` (e.g., `2506-13-143645-f.jpg`)

#### Scenario: Imported file with original name
- **WHEN** a file `report.pdf` is imported from an external app
- **THEN** the file is named `{ts}-f-report.pdf` (e.g., `2506-13-143912-f-report.pdf`)

#### Scenario: Imported file preserves original extension
- **WHEN** a file `photo.png` is imported
- **THEN** the file is named `{ts}-f-photo.png` — the original extension is kept

#### Scenario: Encrypted file
- **WHEN** a file is saved to Private space with encryption enabled
- **THEN** `.enc` is appended to the full filename (e.g., `2506-13-143022-f.md.enc`)

#### Scenario: Pinned entry name
- **WHEN** an entry is pinned
- **THEN** a `9999-` prefix is prepended to its filename (e.g., `2506-13-143022-f.md` becomes `9999-2506-13-143022-f.md`)

#### Scenario: Statused entry name
- **WHEN** an entry is marked TODO
- **THEN** a `TODO-` token is inserted between its timestamp and role char (e.g., `2506-13-143022-f.md` becomes `2506-13-143022-TODO-f.md`)

#### Scenario: Pinned and statused and encrypted name
- **WHEN** an imported, encrypted file is pinned and marked DONE
- **THEN** its name is `9999-2506-13-143912-DONE-f-report.pdf.enc`

#### Scenario: Comment keeps its own format
- **WHEN** a comment is saved against an entry
- **THEN** the comment is named `{targetTs}-n-{nowTs}.md` with no pin prefix and no status token, regardless of the parent's pin/status

#### Scenario: Metadata sidecar name in Public space
- **WHEN** an owner entry gains tags or events in Public space
- **THEN** its sidecar is named `{ts}-m.yaml` (e.g., `2506-13-143022-m.yaml`), with no pin prefix and no status token

#### Scenario: Metadata sidecar name in Private space
- **WHEN** an owner entry gains tags or events in Private space
- **THEN** its sidecar is named `{ts}-m.yaml.enc` (e.g., `2506-13-143022-m.yaml.enc`)

### Requirement: Monthly directory grouping
The system SHALL organize files under `{workDir}/{Public|Private}/YYYY-MM/` instead of daily folders. For example, entries from June 2025 go into `2025-06/`.

#### Scenario: Save entry creates monthly folder
- **WHEN** an entry is saved on June 13, 2025
- **THEN** the file is stored under `{workDir}/{Public|Private}/2025-06/`

#### Scenario: Entries from same month share folder
- **WHEN** entries are saved on June 1 and June 30
- **THEN** both files are in the same `2025-06/` directory

### Requirement: Type detection by extension
The system SHALL infer `EntryType` solely from file extension, with no type prefix in the filename.

| Extension | EntryType |
|-----------|-----------|
| `.md` | TEXT |
| `.m4a`, `.aac` | RECORDING |
| `.jpg`, `.jpeg`, `.png` | PHOTO |
| `.mp4` | VIDEO |
| anything else | FILE |

#### Scenario: Detect text entry
- **WHEN** a file ends with `.md`
- **THEN** `EntryType` is `TEXT`

#### Scenario: Detect imported PDF
- **WHEN** a file ends with `.pdf`
- **THEN** `EntryType` is `FILE`

#### Scenario: Detect encrypted text entry
- **WHEN** a file ends with `.md.enc`
- **THEN** after stripping `.enc`, the `.md` extension yields `EntryType.TEXT`

### Requirement: FileMetadata parsing
The system SHALL provide a `FileMetadata` value class that parses a filename into structured fields: `timestamp`, `role`, `originalName` (optional), `extension`, `isEncrypted`, and derived `entryType`.

#### Scenario: Parse a plain file
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-f.md`
- **THEN** result is `FileMetadata(timestamp="2506-13-143022", role=FILE, originalName=null, extension="md", isEncrypted=false, entryType=TEXT)`

#### Scenario: Parse an imported file
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143912-f-report.pdf`
- **THEN** result has `originalName="report"`, `extension="pdf"`, `entryType=FILE`

#### Scenario: Parse an encrypted file
- **WHEN** `FileMetadata.fromFile(file)` is called on `2506-13-143022-f.md.enc`
- **THEN** result has `isEncrypted=true`, `extension="md"`, `entryType=TEXT`

#### Scenario: Ignore non-MindDump files
- **WHEN** `FileMetadata.fromFile(file)` is called on `readme.txt`
- **THEN** parsing returns null or a sentinel indicating "not a MindDump file" — the file SHALL be skipped during scanning

### Requirement: Backward incompatibility
The system SHALL NOT attempt to parse or migrate old-format files (`文字_*.md`, `录音_*.m4a`, etc.). Old files in the work directory MAY be ignored. No migration path is provided.

#### Scenario: Old format file encountered during scan
- **WHEN** the scanner encounters `文字_143022123.md` in a directory
- **THEN** the file is skipped (no `f-` pattern match, no error raised)

