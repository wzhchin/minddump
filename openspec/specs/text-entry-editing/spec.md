# Purpose

The text-entry-editing capability enables users to edit existing text entries and comments within the MindDump app using an in-app fullscreen editor, rather than composing new entries only.

## Requirements

### Requirement: Tapping a text entry opens the in-app editor
The system SHALL open the in-app fullscreen editor when the user taps an entry whose type is `TEXT`, instead of launching an external application. Non-text entries (photos, audio, video, generic files) SHALL continue to open via the external viewer.

#### Scenario: Tap a text entry on the main list
- **WHEN** the user taps a `TEXT` entry in the main feed
- **THEN** the in-app fullscreen editor opens, pre-filled with that entry's content

#### Scenario: Tap a text entry inside a group
- **WHEN** the user taps a `TEXT` entry in the group detail list
- **THEN** the in-app fullscreen editor opens, pre-filled with that entry's content

#### Scenario: Tap a non-text entry
- **WHEN** the user taps a photo, audio, video, or generic-file entry
- **THEN** the external viewer launches (current behavior unchanged)

### Requirement: Tapping a comment opens the in-app editor
The system SHALL treat comments (`EntryRole.COMMENT`) as editable text: tapping a comment opens the in-app fullscreen editor pre-filled with the comment's content.

#### Scenario: Tap a comment bubble
- **WHEN** the user taps a comment nested under a file entry
- **THEN** the in-app fullscreen editor opens, pre-filled with that comment's content

### Requirement: Single tap-to-open dispatch point
The system SHALL route all entry taps through one dispatch rule: text/comments open the in-app editor; all other types open the external viewer. The main list, the group detail list, and the comment bubble SHALL all use this same rule.

#### Scenario: Dispatch rule is shared across screens
- **WHEN** the same entry type is tapped from the main list, from group detail, or via its comment bubble
- **THEN** the behavior is identical for each (text/comment → editor; other → external viewer)

### Requirement: Edit mode loads the entry's content
The fullscreen editor SHALL support an edit-existing mode: on open, it SHALL load the target entry's current text content as the initial value of the editor field. The new-entry flow (blank field, creates a new entry) SHALL remain unchanged and SHALL NOT share state with edit mode.

#### Scenario: Editor pre-filled from existing entry
- **WHEN** edit mode opens for a text entry
- **THEN** the editor field shows the entry's existing content

#### Scenario: Encrypted entry decrypted for editing
- **WHEN** edit mode opens for an encrypted (`.enc`) entry in Private space
- **THEN** the system decrypts the entry off the main thread and shows its plaintext content in the editor

#### Scenario: New-entry flow unaffected
- **WHEN** the user opens the editor to compose a new entry (no target entry)
- **THEN** the editor field is blank and saving creates a new entry, as before

### Requirement: Saving an edit writes back to the original entry
When the user saves in edit mode, the system SHALL overwrite the original entry's file with the edited content, preserving the entry's identity (same file path). The entry's `lastModified` SHALL update, causing it to move to the top of the time-ordered feed. The Room index row SHALL be refreshed with the new content and timestamp.

#### Scenario: Save overwrites the original text entry
- **WHEN** the user edits a text entry and chooses to save
- **THEN** the entry's file is overwritten with the new content and the entry reappears at the top of the feed

#### Scenario: Save re-encrypts a Private-space entry
- **WHEN** the user edits an encrypted entry and saves
- **THEN** the new content is written back encrypted to the original `.enc` path

#### Scenario: Save a comment edits the comment in place
- **WHEN** the user edits a comment and saves
- **THEN** the comment's file is overwritten and the comment reappears with updated content

### Requirement: Unsaved-changes confirmation on close
When the user closes the editor (via the close button or system back) with edits that differ from the loaded content, the system SHALL present a confirmation ("保留这次编辑吗？") offering three actions: **保存** (save then close), **丢弃** (close without saving), and **继续编辑** (dismiss and keep editing). When there are no unsaved changes, the editor SHALL close immediately without confirmation.

#### Scenario: Close with no changes
- **WHEN** the user closes the editor without having changed the text
- **THEN** the editor closes immediately with no confirmation

#### Scenario: Close with unsaved changes — save
- **WHEN** the user closes the editor with unsaved changes and taps "保存"
- **THEN** the edited content is saved to the original entry and the editor closes

#### Scenario: Close with unsaved changes — discard
- **WHEN** the user closes the editor with unsaved changes and taps "丢弃"
- **THEN** the editor closes without saving and the entry is unchanged

#### Scenario: Close with unsaved changes — keep editing
- **WHEN** the user closes the editor with unsaved changes and taps "继续编辑"
- **THEN** the confirmation is dismissed and the user remains in the editor with their text intact

#### Scenario: System back button also confirms
- **WHEN** the user presses the system back button with unsaved changes
- **THEN** the same confirmation is presented (the back press does not silently discard)

### Requirement: Save unavailable when locked out of encrypted entry
If the session is not unlocked at save time for an encrypted entry, the system SHALL NOT save and SHALL inform the user, keeping the editor open so the user can copy their text elsewhere.

#### Scenario: Save fails because session locked
- **WHEN** the user saves an encrypted entry but the session password is no longer available
- **THEN** the system shows an error message and keeps the editor open without discarding the edits
