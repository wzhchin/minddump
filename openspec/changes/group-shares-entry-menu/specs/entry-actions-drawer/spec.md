## MODIFIED Requirements

### Requirement: Long-press entry action drawer
The system SHALL display a bottom drawer (ModalBottomSheet) when the user long-presses an entry OR a group, replacing the current delete confirmation dialog and the separate group action sheet. The drawer SHALL present the actions available for the selected target.

#### Scenario: Long-press on an entry triggers the drawer
- **WHEN** the user long-presses an entry in the list
- **THEN** a bottom drawer slides up with the actions available for that entry

#### Scenario: Long-press on a group triggers the same drawer
- **WHEN** the user long-presses a group card
- **THEN** the same bottom drawer slides up, driven by group-targeted operations
- **AND** no separate group action sheet is shown

#### Scenario: Drawer header reflects the target
- **WHEN** the drawer is opened for an entry
- **THEN** the header shows the entry's file name
- **WHEN** the drawer is opened for a group
- **THEN** the header shows the group's display name (the segment after `-g`, or the unnamed-group placeholder when blank)

### Requirement: Delete action
The drawer SHALL include a "删除" (Delete) action. For an entry, selecting it SHALL show a confirmation dialog stating the item will be moved to the trash and can be restored; on confirmation the entry SHALL be soft-deleted (recoverable). For a group, the "删除" action SHALL soft-delete the whole group tree via the existing group-delete path: the group and all its members move into `.trash/` together and restore together, mirroring how an entry delete trashes the entry.

#### Scenario: Delete an entry with confirmation trashes it
- **WHEN** the user taps "删除" on an entry in the drawer
- **THEN** a confirmation dialog appears stating the item will be moved to the trash and can be restored
- **WHEN** the user confirms
- **THEN** the entry is moved to `.trash/` and removed from the feed
- **AND** the entry is recoverable from the trash

#### Scenario: Delete a group with confirmation trashes the whole tree
- **WHEN** the user taps "删除" on a group in the drawer
- **THEN** a confirmation dialog appears stating the group and its members will be moved to the trash and can be restored
- **WHEN** the user confirms
- **THEN** the group and all its members are moved into `.trash/` together and removed from the feed
- **AND** the group (with its members) is recoverable from the trash as a unit

## ADDED Requirements

### Requirement: Group-only dissolve action
The drawer SHALL show a distinct "解散" (Dissolve) action when the target is a group; this action SHALL NOT appear for entry targets. Dissolving a group moves its members to the parent location (month directory at root, parent group when nested) and removes the now-empty group directory; members are preserved and relocated, not trashed. The confirmation copy SHALL state that members are moved out, not deleted.

#### Scenario: Dissolve a group preserves members
- **WHEN** the user taps "解散" on a group in the drawer
- **THEN** a confirmation dialog appears stating the group will be dissolved and its members moved out (not deleted)
- **WHEN** the user confirms
- **THEN** the group's members are moved to the parent location
- **AND** the empty group directory is removed
- **AND** the members remain on the feed (relocated), not in the trash

#### Scenario: Dissolve is hidden for entries
- **WHEN** the drawer is opened for an entry
- **THEN** no "解散" action is shown

### Requirement: Action visibility adapts to the target kind
The drawer SHALL show only the actions that apply to the target. Entry-only actions (add comment, edit tags, add scheduled event, move to group, create group, move out of group, move to space) SHALL be hidden when the target is a group. The group-only "解散" action SHALL be hidden when the target is an entry. Shared actions (pin, todo status, rename, share, multi-select, delete) SHALL appear for both entries and groups.

#### Scenario: Group target hides entry-only actions and shows dissolve
- **WHEN** the drawer is opened for a group
- **THEN** the add-comment, edit-tags, add-event, move-to-group, create-group, move-out-of-group, and move-to-space actions are absent
- **AND** the pin, status, rename, share, multi-select, delete, and dissolve actions are present

#### Scenario: Entry target shows the full set and hides dissolve
- **WHEN** the drawer is opened for a non-comment entry
- **THEN** all applicable entry actions are present, unchanged from before
- **AND** no dissolve action is shown
