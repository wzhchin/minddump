## ADDED Requirements

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
