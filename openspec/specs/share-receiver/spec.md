## ADDED Requirements

### Requirement: MindDump appears in Android Share sheet
The system SHALL register intent-filters for `ACTION_SEND` and `ACTION_SEND_MULTIPLE` with MIME type `*/*`, making MindDump visible as a share target from any Android application.

#### Scenario: Sharing text from another app
- **WHEN** user selects text in any app and taps "Share"
- **THEN** MindDump SHALL appear as a share target in the system Share sheet

#### Scenario: Sharing a photo from Gallery
- **WHEN** user selects a photo in the Gallery app and taps "Share"
- **THEN** MindDump SHALL appear as a share target in the system Share sheet

#### Scenario: Sharing multiple files from Files app
- **WHEN** user selects multiple files in the Files app and taps "Share"
- **THEN** MindDump SHALL appear as a share target in the system Share sheet

#### Scenario: Sharing an arbitrary file type
- **WHEN** user shares a file with any MIME type (PDF, ZIP, APK, etc.)
- **THEN** MindDump SHALL appear as a share target in the system Share sheet

### Requirement: Incoming share intent is parsed and stored
The system SHALL parse incoming share intents and extract all shared content (text and/or file URIs) into a structured internal representation.

#### Scenario: Receiving a text share
- **WHEN** a share intent with `ACTION_SEND`, MIME type `text/plain`, and `EXTRA_TEXT` is received
- **THEN** the system SHALL extract the text content as a `ShareItem.Text`

#### Scenario: Receiving a single file share
- **WHEN** a share intent with `ACTION_SEND` and a stream URI (`EXTRA_STREAM`) is received
- **THEN** the system SHALL extract the URI and resolve the original file name via `ContentResolver`
- **THEN** the system SHALL create a `ShareItem.File` with the URI and file name

#### Scenario: Receiving multiple files share
- **WHEN** a share intent with `ACTION_SEND_MULTIPLE` and a list of stream URIs (`EXTRA_STREAM` as `Parcelable[]`) is received
- **THEN** the system SHALL extract each URI and resolve each file name
- **THEN** the system SHALL create a list of `ShareItem.File` entries

#### Scenario: File name resolution fallback
- **WHEN** the original file name cannot be resolved via `ContentResolver`
- **THEN** the system SHALL fall back to `uri.lastPathSegment`, and if that also fails, use `"shared_file"` as the file name

### Requirement: User selects target space before saving
The system SHALL present a space selection dialog (Public / Private) when shared content is received, and only save after the user makes a choice.

#### Scenario: Share received with no prior space selection
- **WHEN** shared content arrives and no space has been selected yet
- **THEN** the system SHALL display a Material 3 dialog with "公开" (Public) and "私密" (Private) options
- **THEN** the dialog SHALL show a summary of what is being saved (e.g., "1 张图片", "3 个文件", "1 段文字")

#### Scenario: User selects Public space
- **WHEN** user taps "公开" in the space selection dialog
- **THEN** all shared items SHALL be saved to Public space
- **THEN** the dialog SHALL dismiss and a confirmation SHALL be shown

#### Scenario: User selects Private space with session unlocked
- **WHEN** user taps "私密" in the space selection dialog AND the Private space session is already unlocked
- **THEN** all shared items SHALL be saved to Private space with encryption
- **THEN** the dialog SHALL dismiss and a confirmation SHALL be shown

#### Scenario: User selects Private space with session locked
- **WHEN** user taps "私密" in the space selection dialog AND the Private space session is NOT unlocked
- **THEN** all shared items SHALL be saved to Private space without encryption (no password available)
- **THEN** the system SHALL indicate that items were saved unencrypted

#### Scenario: User cancels the dialog
- **WHEN** user dismisses the space selection dialog without selecting
- **THEN** no items SHALL be saved
- **THEN** the pending share state SHALL be cleared

### Requirement: Shared content is persisted as MindDump entries
The system SHALL save each shared item as a proper MindDump entry using existing repository methods.

#### Scenario: Saving shared text
- **WHEN** a `ShareItem.Text` is confirmed for a space
- **THEN** the system SHALL call `repository.saveTextEntry(space, content)` to create a Markdown file entry

#### Scenario: Saving shared file
- **WHEN** a `ShareItem.File` is confirmed for a space
- **THEN** the system SHALL call `repository.importFile(space, uri, fileName)` to copy and register the file

#### Scenario: Saving multiple shared items
- **WHEN** multiple items are confirmed for a space
- **THEN** the system SHALL save each item sequentially
- **THEN** all items SHALL be saved to the same space

### Requirement: Activity handles share intents across lifecycle
The system SHALL correctly handle share intents regardless of whether the activity is newly created or already running.

#### Scenario: App not running when share is triggered
- **WHEN** MindDump is not in memory and user shares from another app
- **THEN** the system SHALL receive the intent in `onCreate()` via `getIntent()` and process it

#### Scenario: App already running when share is triggered
- **WHEN** MindDump is already running (possibly in background) and user shares from another app
- **THEN** the system SHALL receive the intent via `onNewIntent()` and process it
- **THEN** the activity SHALL be reused (singleTask launch mode) without creating a duplicate instance

#### Scenario: Rapid consecutive shares
- **WHEN** user shares content while a previous share is still being processed
- **THEN** the system SHALL queue the new share and process it after the current one completes
