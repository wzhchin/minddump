## ADDED Requirements

### Requirement: FileStorageEngine handles encryption internally
The system SHALL make `FileStorageEngine` responsible for encrypting files when saving to Private space. All `saveTextEntry()`, `getRecordingFile()` + post-save registration, `getPhotoFile()` + post-save registration, `getVideoFile()` + post-save registration, and `importFile()` paths SHALL automatically encrypt the output file when the target space is `PRIVATE` and a session password has been set via `FileStorageEngine.setSessionPassword()`.

#### Scenario: Save text entry to Private space with password
- **WHEN** `saveTextEntry(PRIVATE, "hello")` is called and a session password is set
- **THEN** the returned file ends with `.enc`, the plaintext file is deleted, and no other layer performs encryption

#### Scenario: Save text entry to Public space
- **WHEN** `saveTextEntry(PUBLIC, "hello")` is called
- **THEN** the returned file is plaintext `.md` with no encryption step

#### Scenario: Save text entry to Private space without password
- **WHEN** `saveTextEntry(PRIVATE, "hello")` is called and no session password is set
- **THEN** the returned file is plaintext `.md` — encryption is skipped (user hasn't set a password yet)

### Requirement: FileStorageEngine provides transparent decryption for viewing
The system SHALL provide a `readForViewing(file: File): File` method on `FileStorageEngine` that returns a readable (decrypted) file. If the file is not encrypted, it returns the file itself. If encrypted, it decrypts to a temp file in `.cache/` and returns the temp path.

#### Scenario: View a plaintext file
- **WHEN** `readForViewing(file)` is called on `文字_143022123.md`
- **THEN** the same `file` reference is returned with no decryption or temp file created

#### Scenario: View an encrypted file
- **WHEN** `readForViewing(file)` is called on `文字_143022123.md.enc` and a session password is set
- **THEN** a decrypted temp file is created under `.cache/` and returned

#### Scenario: View encrypted file without session password
- **WHEN** `readForViewing(file)` is called on an encrypted file and no session password is set
- **THEN** an `IllegalStateException` is thrown

### Requirement: Repository no longer accesses CryptoEngine directly
The system SHALL remove `CryptoEngine` from `MindDumpRepository`'s constructor. All encryption/decryption SHALL flow through `FileStorageEngine`. `MindDumpRepository` SHALL NOT reference `CryptoEngine`, `.enc` suffixes, or temp decryption directories.

#### Scenario: Repository constructor after refactor
- **WHEN** the refactored `MindDumpRepository` is compiled
- **THEN** its constructor takes `MindDumpDatabase`, `FileStorageEngine`, and `PasswordStore` only — no `CryptoEngine`

#### Scenario: Repository saveTextEntry after refactor
- **WHEN** `MindDumpRepository.saveTextEntry(space, content)` is called
- **THEN** it calls `storageEngine.saveTextEntry(space, content)` and receives the already-encrypted (or plaintext) file — no post-hoc encryption step

### Requirement: Reconciliation split into composable steps
The system SHALL split `reconcileWithDisk()` into three independent methods: `syncNewDiskEntries(space)`, `removeOrphanedDbEntries(space)`, and `refreshStaleEntries(space)`. A convenience `reconcileWithDisk(space)` SHALL call all three in order.

#### Scenario: syncNewDiskEntries inserts new files
- **WHEN** disk has files that are not in the DB
- **THEN** `syncNewDiskEntries(space)` inserts Room rows for each, including content previews for TEXT entries

#### Scenario: removeOrphanedDbEntries deletes stale rows
- **WHEN** DB has rows whose files no longer exist on disk
- **THEN** `removeOrphanedDbEntries(space)` deletes those rows

#### Scenario: refreshStaleEntries updates changed entries
- **WHEN** a file's `lastModified` differs from its DB row
- **THEN** `refreshStaleEntries(space)` updates `lastModified` and `contentPreview`

#### Scenario: reconcileWithDisk calls all three
- **WHEN** `reconcileWithDisk(space)` is called
- **THEN** it invokes `syncNewDiskEntries`, `removeOrphanedDbEntries`, and `refreshStaleEntries` in that order, producing the same result as the current monolithic method

### Requirement: Session password propagated to FileStorageEngine
The system SHALL propagate the session password from `MindDumpRepository` to `FileStorageEngine` whenever the password changes (set, verify, clear). `FileStorageEngine` SHALL store this password in memory for the session lifetime only.

#### Scenario: Password set via Repository
- **WHEN** `MindDumpRepository.setPassword(password)` is called
- **THEN** `FileStorageEngine.setSessionPassword(password)` is also called

#### Scenario: Password cleared
- **WHEN** `MindDumpRepository.clearSessionPassword()` is called
- **THEN** `FileStorageEngine.setSessionPassword(null)` is also called

### Requirement: Decrypted cache cleanup preserved
The system SHALL preserve the `cleanDecryptedCache()` functionality. After refactor, this method SHALL delegate to `FileStorageEngine.cleanDecryptedCache()`.

#### Scenario: Clean cache after session
- **WHEN** `MindDumpRepository.cleanDecryptedCache()` is called
- **THEN** all files under `.cache/` in the root directory are deleted
