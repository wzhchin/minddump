## ADDED Requirements

### Requirement: Share action exports a single entry to other apps
The system SHALL provide a "分享" (Share) affordance that exports any stored entry to other Android apps via `ACTION_SEND`. Text entries and comments SHALL be shared as `text/plain` content in `EXTRA_TEXT`; all other entry types (photos, recordings, videos, imported files) SHALL be shared as a content URI in `EXTRA_STREAM` with a MIME type derived from the file extension.

#### Scenario: Sharing a text note
- **WHEN** the user taps "分享" on a text note
- **THEN** the system SHALL open the Android Share sheet with the note's full text content as `text/plain`

#### Scenario: Sharing a comment
- **WHEN** the user taps "分享" on a comment entry
- **THEN** the system SHALL open the Android Share sheet with the comment's text content as `text/plain`

#### Scenario: Sharing a photo
- **WHEN** the user taps "分享" on a photo entry
- **THEN** the system SHALL open the Android Share sheet with a FileProvider content URI of the photo in `EXTRA_STREAM` and MIME type `image/jpeg`

#### Scenario: Sharing a recording
- **WHEN** the user taps "分享" on an audio recording
- **THEN** the system SHALL open the Android Share sheet with the recording's content URI in `EXTRA_STREAM` and an audio MIME type

#### Scenario: Sharing an imported file
- **WHEN** the user taps "分享" on an imported file (e.g. a PDF)
- **THEN** the system SHALL open the Android Share sheet with the file's content URI in `EXTRA_STREAM` and a MIME type derived from its extension

### Requirement: Encrypted Private entries are decrypted transparently before sharing
The system SHALL decrypt Private-space `.enc` entries to the `.cache/` directory before sharing, so that the receiving app receives usable plaintext content. The original encrypted file SHALL NOT be sent. The decrypted temp file SHALL live under the FileProvider-exposed `.cache/` path and be subject to the same cache cleanup as decrypted-for-viewing files.

#### Scenario: Sharing an encrypted Private photo
- **WHEN** the user shares an encrypted (`.enc`) Private photo
- **THEN** the system SHALL decrypt it to `.cache/`
- **AND** the Share sheet SHALL carry a content URI of the decrypted plaintext file, not the `.enc` file

#### Scenario: Sharing a Public entry does not copy
- **WHEN** the user shares a plaintext Public entry
- **THEN** the system SHALL share the original file directly without creating a cache copy

### Requirement: Sharing fails safely when the Private session is locked
The system SHALL NOT share an encrypted Private entry when the session password is no longer cached. Instead it SHALL surface a clear message to the user and SHALL NOT open the Share sheet.

#### Scenario: Encrypted share while session locked
- **WHEN** the user shares an encrypted Private entry AND the session is not unlocked (no cached password)
- **THEN** the Share sheet SHALL NOT open
- **AND** the system SHALL show a message indicating the Private content is locked and must be unlocked first

### Requirement: Multi-select bulk share
The system SHALL allow sharing all entries selected in multi-select mode together via a single `ACTION_SEND_MULTIPLE` intent.

#### Scenario: Share a multi-selected batch
- **WHEN** the user selects several entries and taps "分享" in the multi-select top bar
- **THEN** the system SHALL open the Share sheet with all selected entries as multiple streams
- **AND** encrypted Private entries in the batch SHALL be decrypted to cache; Public entries SHALL be shared directly

#### Scenario: Batch contains a locked Private entry
- **WHEN** a selected batch contains an encrypted Private entry AND the session is locked
- **THEN** the Share sheet SHALL NOT open
- **AND** the system SHALL show the locked-session message for the whole batch

### Requirement: Whole-group share
The system SHALL allow sharing every member of a group together via a single `ACTION_SEND_MULTIPLE` intent, triggered from the group's long-press action sheet.

#### Scenario: Share an entire group
- **WHEN** the user long-presses a group and taps "分享"
- **THEN** the system SHALL resolve all member entries of the group
- **AND** open the Share sheet with every member as multiple streams

#### Scenario: Share an empty group
- **WHEN** the user taps "分享" on a group that has no members
- **THEN** the Share sheet SHALL NOT open and no empty intent SHALL be dispatched

### Requirement: Shared content URIs are readable by the receiving app
The system SHALL grant read permission on every shared content URI to the receiving application, so that the receiver can read the shared content without a permission denial.

#### Scenario: Receiver can read a shared file
- **WHEN** the Share sheet is opened with one or more content URIs
- **THEN** the launched intent SHALL carry `FLAG_GRANT_READ_URI_PERMISSION`
- **AND** each URI SHALL be resolvable through the MindDump FileProvider authority
