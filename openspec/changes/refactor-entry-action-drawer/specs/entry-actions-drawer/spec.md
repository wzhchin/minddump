## MODIFIED Requirements

### Requirement: Long-press entry action drawer
The system SHALL display a bottom drawer (ModalBottomSheet) when the user long-presses an entry. The drawer SHALL group its actions into two tiers: a compact **quick-action bar** of icon-only buttons at the top, and **detail rows** below for actions that summarize entry state.

- **Quick-action bar** (icon buttons, wrapping to a second row as the width allows, no visible text labels; each exposes its name as the icon's accessibility label): pin/unpin, add comment, share, rename, multi-select, move to group, move out of group (only when the entry is in a group), and move to the opposite space.
- **Detail rows** (full-width, each may show a trailing state summary): todo status, add tags, add reminder, delete.

The same action callbacks, conditional visibility, confirmation flows, and sub-dialogs as before SHALL be preserved; only presentation and the addition of the reminder-time summary change.

#### Scenario: Long-press triggers drawer
- **WHEN** the user long-presses an entry in the list
- **THEN** a bottom drawer slides up with a quick-action icon bar at the top, followed by the detail rows

#### Scenario: Quick actions wrap into two rows
- **WHEN** the drawer is shown for a file entry that is not a comment and not in a group
- **THEN** the icon bar renders its visible quick actions and wraps to a second row as needed rather than overflowing horizontally

#### Scenario: Detail rows retain their trailing state summaries
- **WHEN** the drawer is shown for an entry that has a todo status, tags, or a scheduled reminder
- **THEN** each corresponding detail row shows its trailing summary (the current status, the existing tags, or the reminder time), while pin/comment/share/rename/multi-select/move actions live only in the icon bar

#### Scenario: Comment entry shows a reduced action set
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** the quick-action bar omits the pin, add-comment, and status actions, but still shows share, rename, multi-select, move, and delete

### Requirement: Pin/unpin action
The entry actions drawer SHALL include a pin/unpin toggle action for file entries (not comments). It SHALL appear as an icon button in the quick-action bar. The icon button SHALL give a **filled-tint visual cue** when the entry is currently pinned (for example, a filled/highlighted pin icon versus the default outlined treatment), so the pinned state is readable without a label.

#### Scenario: Pin from the drawer
- **WHEN** the user opens the drawer for `2506-13-143022-f.md` and taps the pin action
- **THEN** the file is renamed to `9999-2506-13-143022-f.md` and the pin icon reflects the pinned state

#### Scenario: Unpin from the drawer
- **WHEN** the user opens the drawer for `9999-2506-13-143022-f.md` and taps the pin action
- **THEN** the file is renamed to `2506-13-143022-f.md`

#### Scenario: Pinned entry shows the pinned visual cue
- **WHEN** the user opens the drawer for a pinned entry
- **THEN** the pin icon button is rendered with a filled-tint treatment distinguishing it from an unpinned entry

#### Scenario: Comment entry has no pin action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no pin/unpin action is shown in the quick-action bar

### Requirement: Rename action
The drawer SHALL include a "Rename" action in the quick-action bar that allows editing the `originalName` portion of the filename (the part after `-f-` and before the extension).

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
The drawer SHALL include a "Multi-select" action in the quick-action bar that enters multi-selection mode. In this mode, entries show checkboxes, and a top action bar appears with bulk operations.

#### Scenario: Enter multi-select mode
- **WHEN** the user taps the multi-select action in the drawer
- **THEN** the drawer closes, entries show checkboxes, and a top action bar appears

#### Scenario: Bulk delete in multi-select
- **WHEN** the user selects 3 entries and taps "Delete" in the action bar
- **THEN** all 3 entries are deleted after confirmation

#### Scenario: Exit multi-select
- **WHEN** the user taps the back button or "Done" in the action bar
- **THEN** multi-selection mode exits and checkboxes disappear

### Requirement: Move to group action
The drawer SHALL include a "Move to group" action in the quick-action bar. Selecting it SHALL present a list of existing groups plus a "Create new group" option.

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
The drawer SHALL include a "Move to {opposite space}" action in the quick-action bar that moves the file between Public and Private spaces.

#### Scenario: Move from Public to Private
- **WHEN** the user taps "Move to Private" on a Public entry
- **THEN** the file is moved from `Public/YYYY-MM/` to `Private/YYYY-MM/`, and encrypted if a session password is set

#### Scenario: Move from Private to Public
- **WHEN** the user taps "Move to Public" on a Private entry
- **THEN** the file is decrypted (if encrypted) and moved from `Private/YYYY-MM/` to `Public/YYYY-MM/`

#### Scenario: File in group moved between spaces
- **WHEN** a file inside a group directory is moved to the opposite space
- **THEN** the file is moved out of the group and into the corresponding space's month directory (groups are space-scoped)

### Requirement: Share action
The entry actions drawer SHALL include a "Share" action in the quick-action bar, available for every entry type, including comments. Selecting it SHALL export the entry to other Android apps via the system Share sheet, as defined by the `outbound-share` capability.

#### Scenario: Share action is present
- **WHEN** the user opens the entry actions drawer for any entry (file or comment)
- **THEN** a share action SHALL be present in the quick-action bar

#### Scenario: Share action on a text note
- **WHEN** the user taps share on a text note
- **THEN** the drawer SHALL dismiss and the system Share sheet SHALL open with the note's text content

#### Scenario: Share action on a comment
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** the share action SHALL still be available (unlike pin/status/comment, share applies to comments)

#### Scenario: Share action on an encrypted Private entry while locked
- **WHEN** the user taps share on an encrypted Private entry AND the session is locked
- **THEN** the Share sheet SHALL NOT open and a locked-session message SHALL be shown

## ADDED Requirements

### Requirement: Add tags action
The entry actions drawer SHALL include an "Add tags" action as a detail row for entries that support metadata. The row SHALL show, as a trailing summary, the entry's existing tags (rendered as `#tag1 #tag2 …`) when at least one tag is set, so the user can see the current tags without opening the editor. Selecting the row SHALL open the tag editor sheet, as defined by the `entry-tags` capability.

#### Scenario: Tags row shows existing tags
- **WHEN** the user opens the drawer for an entry tagged `idea` and `followup`
- **THEN** the "Add tags" detail row shows a trailing `#idea #followup` summary

#### Scenario: Tags row with no tags
- **WHEN** the user opens the drawer for an entry with no tags
- **THEN** the "Add tags" detail row shows no trailing summary

#### Scenario: Open the tag editor
- **WHEN** the user taps the "Add tags" row
- **THEN** the drawer dismisses and the tag editor sheet opens for that entry

### Requirement: Scheduled reminder action with time summary
The entry actions drawer SHALL include an "Add reminder" action as a detail row for entries that support scheduled events. When the entry has at least one scheduled event, the row SHALL show the event's due time as a trailing summary — formatted as a friendly local date-time (reusing the app's existing relative-date formatting, e.g. "今天 HH:mm", "昨天 HH:mm", "M月d日 HH:mm", "yyyy年M月d日 HH:mm") rather than a bare state word. Selecting the row SHALL open the date/time picker, as defined by the `scheduled-events` capability.

#### Scenario: Reminder row shows the scheduled time
- **WHEN** the user opens the drawer for an entry with one scheduled event due at a specific local date-time
- **THEN** the "Add reminder" detail row shows that due time as a formatted trailing summary

#### Scenario: Reminder row with no scheduled event
- **WHEN** the user opens the drawer for an entry with no scheduled events
- **THEN** the "Add reminder" detail row shows no trailing summary

#### Scenario: Open the date/time picker
- **WHEN** the user taps the "Add reminder" row
- **THEN** the drawer dismisses and the date/time picker opens for that entry

### Requirement: Action label localization
The drawer's quick-action icon buttons and detail-row labels SHALL draw their user-facing names from string resources, with localized values in zh-CN (`res/values/strings.xml`) and en (`res/values-en/strings.xml`). No action label SHALL be hardcoded in source. Each quick-action icon button SHALL expose its localized name as the icon's accessibility label (`contentDescription`).

#### Scenario: Icon buttons expose localized accessibility labels
- **WHEN** the quick-action bar is rendered
- **THEN** each icon button's `contentDescription` is the localized action name from string resources

#### Scenario: Action labels have English counterparts
- **WHEN** the app runs under the English locale
- **THEN** every drawer action (rename, multi-select, move to group, move out of group, move to private/public, and the others) shows its English string-resource value
