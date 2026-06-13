## ADDED Requirements

### Requirement: Crash-safe encryption
The system SHALL ensure that encrypting a file never leaves the original plaintext file on disk if the encrypted output is missing or corrupt.

#### Scenario: Successful encryption
- **WHEN** a file is encrypted and the encrypted output file exists with non-zero size
- **THEN** the system SHALL delete the original plaintext file

#### Scenario: Encryption output is missing or empty
- **WHEN** the encryption operation completes but the output file does not exist or has zero bytes
- **THEN** the system SHALL throw an exception and MUST NOT delete the original plaintext file

#### Scenario: App crash during encryption
- **WHEN** the app process is killed between writing the encrypted file and deleting the plaintext
- **THEN** the plaintext file MAY remain on disk, but the encrypted file SHALL also exist (both files present)
- **AND** on next `reconcileWithDisk`, the system SHALL detect the unencrypted file as a duplicate and clean it up

### Requirement: Entry type determined before encryption
The system SHALL determine the `EntryType` from the original (pre-encryption) file name and MUST NOT attempt to infer type from an encrypted file name.

#### Scenario: Registering a media file in Private space
- **WHEN** a media file (audio, photo, video) is registered for Private space
- **THEN** the system SHALL parse the `EntryType` from the original file name before encrypting
- **AND** the encrypted file name (with `.enc` suffix) SHALL NOT be used for type inference

#### Scenario: Scanning encrypted files from disk
- **WHEN** `scanEntries()` encounters a file with `.enc` suffix
- **THEN** the system SHALL strip the `.enc` suffix before attempting type inference via `EntryType.fromFileName()`
