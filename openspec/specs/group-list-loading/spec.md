## Purpose

Loading and exposing group directories from disk as ViewModel state for the group picker UI.

## Requirements

### Requirement: Group list populated from disk
The system SHALL scan the current space's current month directory for group subdirectories (matching the `*-g*` naming pattern) and expose them as a `groups: List<File>` field in the ViewModel `UiState`.

#### Scenario: Existing groups shown in picker
- **WHEN** the user opens the EntryActionDrawer and taps "移动到组"
- **THEN** the GroupPickerSheet SHALL display all group directories found in the current month directory

#### Scenario: No groups on disk
- **WHEN** the current month directory has no group subdirectories
- **THEN** the GroupPickerSheet SHALL display "暂无分组" text

#### Scenario: Groups refresh after entry operations
- **WHEN** an entry is moved to a group, moved out of a group, or a new group is created
- **THEN** the group list SHALL be refreshed to reflect the current disk state

### Requirement: Create-and-move atomic operation
The system SHALL provide a combined create-and-move operation so that selecting "新建组" from the GroupPickerSheet creates the group and immediately moves the selected entry into it. The operation SHALL accept an optional parent group directory: when no parent is given, the group is created at the current month-top level (default behavior); when a parent group is given, the group is created as a child of that group, enabling nested grouping.

#### Scenario: Create named group and move entry
- **WHEN** the user enters a group name and confirms
- **THEN** the system SHALL create a new group directory with the given name and move the selected entry's file into it

#### Scenario: Create anonymous group and move entry
- **WHEN** the user leaves the group name blank and confirms
- **THEN** the system SHALL create a new anonymous group directory and move the selected entry's file into it

#### Scenario: Move to existing group
- **WHEN** the user selects an existing group from the list
- **THEN** the system SHALL move the selected entry's file into that group directory
- **AND** the database record SHALL be updated with the new file path and `groupPath`

#### Scenario: Create group at month-top when no parent
- **WHEN** the user creates a group and no parent group is in scope
- **THEN** the group directory SHALL be created directly under the current month directory

#### Scenario: Create sub-group under an open group
- **WHEN** the user creates a group while a parent group is in scope (an open group page)
- **THEN** the group directory SHALL be created inside that parent group directory

### Requirement: Group list scoped to current month
The group list SHALL only include groups from the current space's current month directory. Groups from other months or spaces SHALL NOT appear in the picker.

#### Scenario: Switching spaces
- **WHEN** the user switches from Public to Private space
- **THEN** the group list SHALL update to show only groups in the Private space's current month directory
