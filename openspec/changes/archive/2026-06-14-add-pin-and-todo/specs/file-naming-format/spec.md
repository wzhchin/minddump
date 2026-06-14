## MODIFIED Requirements

### Requirement: File naming format
The system SHALL use `[9999-][yymm-dd-HHMMSS]-[STATUS]-f[-{originalName}].{extension}[.enc]` for user-managed files, where the `9999-` prefix is an optional pin sentinel and `[STATUS]` is an optional todo-status token (`TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`) followed by `-`. Timestamp format is `yymm-dd-HHMMSS` (e.g., `2506-13-143022`). When neither pin nor status is present, the name collapses to the legacy `{ts}-f[-{name}].{ext}[.enc]` form. Comments use `{ts}-n-{ts}.md[.enc]` and never carry a pin prefix or status token.

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
