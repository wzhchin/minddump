## Purpose

Bottom sheet drawer for entry actions triggered by long-press, providing rename, delete, multi-select, group move, and space move operations.
## Requirements
### Requirement: Long-press entry action drawer
The system SHALL display a bottom drawer (ModalBottomSheet) when the user long-presses an entry, replacing the current delete confirmation dialog. The drawer SHALL present available actions for the selected entry.

#### Scenario: Long-press triggers drawer
- **WHEN** the user long-presses an entry in the list
- **THEN** a bottom drawer slides up with available actions

#### Scenario: Drawer does not show delete confirmation
- **WHEN** the drawer is presented
- **THEN** actions are listed directly — no separate confirmation dialog is needed for non-destructive actions

### Requirement: Delete action
The drawer SHALL include a "Delete" action. Selecting it SHALL show a confirmation dialog. On confirmation, the entry SHALL be moved to the trash (a recoverable soft delete) rather than permanently destroyed; the confirmation copy SHALL make clear the item can be restored from the trash. Multi-select batch delete SHALL follow the same recoverable path.

#### Scenario: Delete with confirmation trashes the entry
- **WHEN** the user taps "Delete" in the drawer
- **THEN** a confirmation dialog appears stating the item will be moved to the trash and can be restored
- **WHEN** the user confirms
- **THEN** the entry is moved to `.trash/` and removed from the feed
- **AND** the entry is recoverable from the trash

#### Scenario: Batch delete trashes all selected entries
- **WHEN** the user selects multiple entries and taps "Delete" in the action bar, then confirms
- **THEN** every selected entry is moved to the trash and removed from the feed

### Requirement: Rename action
The drawer SHALL include a "Rename" action that allows editing the `originalName` portion of the filename (the part after `-f-` and before the extension).

#### Scenario: Rename imported file
- **WHEN** the user taps "Rename" on `2506-13-143912-f-report.pdf`
- **THEN** a text field pre-filled with "report" is shown
- **WHEN** the user changes it to "annual-report" and confirms
- **THEN** the file is renamed to `2506-13-143912-f-annual-report.pdf`

#### Scenario: Rename file without original name
- **WHEN** the user taps "Rename" on `2506-13-143022-f.md`
- **THEN** a text field is shown (empty, since there is no original name)
- **WHEN** the user enters "meeting-notes" and confirms
- **THEN** the file is renamed to `2506-13-143022-f-meeting-notes.md`

### Requirement: Multi-select action
The drawer SHALL include a "Multi-select" action that enters multi-selection mode. In this mode, entries show checkboxes, and a top action bar appears with bulk operations.

#### Scenario: Enter multi-select mode
- **WHEN** the user taps "Multi-select" in the drawer
- **THEN** the drawer closes, entries show checkboxes, and a top action bar appears

#### Scenario: Bulk delete in multi-select
- **WHEN** the user selects 3 entries and taps "Delete" in the action bar
- **THEN** all 3 entries are deleted after confirmation

#### Scenario: Exit multi-select
- **WHEN** the user taps the back button or "Done" in the action bar
- **THEN** multi-selection mode exits and checkboxes disappear

### Requirement: Move to group action
The drawer SHALL include a "Move to group" action. Selecting it SHALL present a list of existing groups plus a "Create new group" option.

#### Scenario: Move to existing group
- **WHEN** the user taps "Move to group" and selects an existing group
- **THEN** the file is physically moved to that group's directory

#### Scenario: Move to new group
- **WHEN** the user taps "Move to group" and selects "Create new group"
- **THEN** a name input dialog appears (name is optional)
- **WHEN** confirmed, a new group directory is created and the file is moved into it

#### Scenario: Move file out of a group
- **WHEN** the user moves a file that is currently in a group
- **THEN** the file is moved out of the group directory into the parent month directory

### Requirement: Move to Public/Private action
The drawer SHALL include a "Move to {opposite space}" action that moves the file between Public and Private spaces.

#### Scenario: Move from Public to Private
- **WHEN** the user taps "Move to Private" on a Public entry
- **THEN** the file is moved from `Public/YYYY-MM/` to `Private/YYYY-MM/`, and encrypted if a session password is set

#### Scenario: Move from Private to Public
- **WHEN** the user taps "Move to Public" on a Private entry
- **THEN** the file is decrypted (if encrypted) and moved from `Private/YYYY-MM/` to `Public/YYYY-MM/`

#### Scenario: File in group moved between spaces
- **WHEN** a file inside a group directory is moved to the opposite space
- **THEN** the file is moved out of the group and into the corresponding space's month directory (groups are space-scoped)

### Requirement: Pin/unpin action
The entry actions drawer SHALL include a pin/unpin toggle action for file entries (not comments). The action label SHALL reflect the current state ("置顶" when unpinned, "取消置顶" when pinned).

#### Scenario: Pin from the drawer
- **WHEN** the user opens the drawer for `2506-13-143022-f.md` and taps "置顶"
- **THEN** the file is renamed to `9999-2506-13-143022-f.md` and the drawer reflects the pinned state

#### Scenario: Unpin from the drawer
- **WHEN** the user opens the drawer for `9999-2506-13-143022-f.md` and taps "取消置顶"
- **THEN** the file is renamed to `2506-13-143022-f.md`

#### Scenario: Comment entry has no pin action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no pin/unpin action is shown

### Requirement: Set todo status action
The entry actions drawer SHALL include a "待办状态" (todo status) action that opens a chooser offering the status set (`TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`) plus a "无" (clear) option, for file entries (not comments).

#### Scenario: Open the status chooser
- **WHEN** the user taps "待办状态" in the drawer
- **THEN** a chooser presents TODO, DOING, WAIT, DONE, CANCEL, and a clear option, with the current status marked

#### Scenario: Pick a status
- **WHEN** the user picks DOING for `2506-13-143022-f.md`
- **THEN** the file is renamed to `2506-13-143022-DOING-f.md` and the chooser closes

#### Scenario: Clear the status
- **WHEN** the user picks the clear option for `2506-13-143022-DONE-f.md`
- **THEN** the file is renamed to `2506-13-143022-f.md`

#### Scenario: Comment entry has no status action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no todo-status action is shown

### Requirement: Add comment action
The entry actions drawer SHALL include an "Add comment" action for file entries (not comments). Selecting it SHALL open a text input dialog; confirming with non-blank text SHALL create a comment targeting that entry.

#### Scenario: Add a comment from the drawer
- **WHEN** the user long-presses a file entry and taps "Add comment"
- **THEN** a dialog with a multi-line text field appears
- **WHEN** the user enters "follow up on this tomorrow" and confirms
- **THEN** a comment file named `{targetTs}-n-{nowTs}.md` is written into the target entry's directory
- **AND** it is indexed with the comment role and the target entry's timestamp as its `targetTimestamp`
- **AND** the parent entry's card now shows the comment folded inside it (e.g. "1 条评论")

#### Scenario: Blank comment is rejected
- **WHEN** the comment dialog is open with an empty or whitespace-only text field
- **THEN** the confirm action is unavailable, and no comment file is written

#### Scenario: Comment inherits the parent's group
- **WHEN** the user adds a comment to an entry that lives inside a group directory
- **THEN** the comment file is written into that same group directory
- **AND** the comment nests under the entry within the group's detail view

#### Scenario: Comment entry has no add-comment action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no "Add comment" action is shown (comments cannot target other comments)

### Requirement: Share action
The entry actions drawer SHALL include a "分享" (Share) action available for every entry type, including comments. Selecting it SHALL export the entry to other Android apps via the system Share sheet, as defined by the `outbound-share` capability.

#### Scenario: Share action is present
- **WHEN** the user opens the entry actions drawer for any entry (file or comment)
- **THEN** a "分享" action SHALL be listed among the drawer actions

#### Scenario: Share action on a text note
- **WHEN** the user taps "分享" on a text note
- **THEN** the drawer SHALL dismiss and the system Share sheet SHALL open with the note's text content

#### Scenario: Share action on a comment
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** the "分享" action SHALL still be available (unlike pin/status/comment, share applies to comments)

#### Scenario: Share action on an encrypted Private entry while locked
- **WHEN** the user taps "分享" on an encrypted Private entry AND the session is locked
- **THEN** the Share sheet SHALL NOT open and a locked-session message SHALL be shown

