## ADDED Requirements

### Requirement: File path uniqueness
The system SHALL enforce a unique constraint on `filePath` in the `entries` Room table. No two `EntryEntity` rows SHALL share the same `filePath`.

#### Scenario: Inserting a duplicate file path
- **WHEN** an entry is inserted with a `filePath` that already exists in the database
- **THEN** the existing row SHALL be replaced (upsert semantics) and the `id` SHALL remain stable when using `OnConflictStrategy.REPLACE` on the `filePath` unique index

#### Scenario: Migration from version 1 with duplicate paths
- **WHEN** the app migrates from Room version 1 to version 2 and duplicate `filePath` values exist
- **THEN** the migration SHALL keep the row with the lowest `id` and delete duplicates before creating the unique index

### Requirement: Bidirectional disk-database reconciliation
The system SHALL provide a `reconcileWithDisk(space)` operation that synchronizes the Room database with the actual file system state.

#### Scenario: New file on disk not in database
- **WHEN** a file exists in the space directory but has no matching `filePath` in Room
- **THEN** the system SHALL create a new `EntryEntity` with correct `type`, `dateFolder`, `timestamp`, `contentPreview` (for text entries), `isEncrypted`, and `lastModified`

#### Scenario: Database entry with missing file on disk
- **WHEN** an `EntryEntity` exists in Room but the corresponding file does not exist on disk
- **THEN** the system SHALL delete the `EntryEntity` from Room

#### Scenario: File modified on disk after indexing
- **WHEN** a file's `lastModified` timestamp differs from the `lastModified` stored in Room
- **THEN** the system SHALL update the `contentPreview` (for text entries), `lastModified`, and `isEncrypted` fields

#### Scenario: All entries in sync
- **WHEN** every file on disk has a matching Room entry with identical `lastModified`
- **THEN** the system SHALL make no changes to the database

### Requirement: Import file validation
The system SHALL reject invalid file imports and MUST NOT create a database entry for a failed import.

#### Scenario: Content URI cannot be opened
- **WHEN** `ContentResolver.openInputStream()` returns null for a given URI
- **THEN** the system SHALL throw an `IOException` and MUST NOT create a file or database entry

#### Scenario: Imported file is empty
- **WHEN** the copied file has zero bytes
- **THEN** the system SHALL throw an `IOException`, delete the empty file, and MUST NOT create a database entry

### Requirement: Collision-free file naming
The system SHALL generate file names that do not collide with existing files in the same directory.

#### Scenario: Normal file creation
- **WHEN** a new file is created (text, audio, photo, video, or imported file)
- **THEN** the file name SHALL use a millisecond-precision timestamp (`HHmmssSSS`) format

#### Scenario: Timestamp collision
- **WHEN** a generated file name already exists in the target directory
- **THEN** the system SHALL append an incrementing sequence suffix (`_1`, `_2`, ...) until a unique name is found

### Requirement: Statistics query index coverage
The system SHALL maintain database indexes to cover common statistics query patterns without full table scans.

#### Scenario: Query entries by space and date
- **WHEN** the DAO executes a `GROUP BY dateFolder` query filtered by `space`
- **THEN** the query SHALL use the `(space, dateFolder)` composite index

#### Scenario: Query entries by space and type
- **WHEN** the DAO executes a `GROUP BY type` query filtered by `space`
- **THEN** the query SHALL use the `(space, type)` composite index
