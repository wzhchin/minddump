## MODIFIED Requirements

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
- **AND** the database record SHALL be updated with the new file path and `parentId` (the owning group's `tid`)

#### Scenario: Create group at month-top when no parent
- **WHEN** the user creates a group and no parent group is in scope
- **THEN** the group directory SHALL be created directly under the current month directory

#### Scenario: Create sub-group under an open group
- **WHEN** the user creates a group while a parent group is in scope (an open group page)
- **THEN** the group directory SHALL be created inside that parent group directory
