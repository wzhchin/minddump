## ADDED Requirements

### Requirement: Group detail is a drillable sub-homepage
The group detail screen SHALL render the group's direct members, any nested sub-group cards, and a bottom input bar — i.e. it behaves like the main feed scoped to one group. The group's identity SHALL be carried by the navigation route (`group_detail/{groupPath}`), not by a single-value selection field, so multiple levels can be open on the back stack.

#### Scenario: Enter a group from the main feed
- **WHEN** the user taps a group card on the main feed
- **THEN** the group detail screen opens for that group, showing its direct members, sub-group cards, and an input bar

#### Scenario: Drill into a sub-group
- **WHEN** the user taps a sub-group card inside a group page
- **THEN** a new group detail screen opens for that sub-group
- **AND** the previous group page remains on the back stack

#### Scenario: Back returns one level at a time
- **WHEN** the user presses back from a sub-group page reached via `A → B`
- **THEN** the app returns to group A's page, not the main feed

### Requirement: Group page shows only direct members
A group page SHALL list only entries whose `groupPath` equals that group directory's absolute path. Members of nested sub-groups SHALL NOT appear in the parent's page; they appear in their own sub-group page.

#### Scenario: Direct members listed
- **WHEN** a group page is open
- **THEN** only entries placed directly in that group directory are shown

#### Scenario: Sub-group members excluded
- **WHEN** a group contains a sub-group with its own members
- **THEN** those sub-group members are not listed in the parent group page

### Requirement: Nested sub-group cards in a group page
A group page SHALL display a card for each direct sub-group (sub-directory with group role) found inside the current group directory, using the same card presentation as group cards on the main feed. Tapping a sub-group card drills into it.

#### Scenario: Sub-groups shown as cards
- **WHEN** a group contains one or more sub-group directories
- **THEN** each sub-group is rendered as a card with its name and member summary

#### Scenario: Group with no sub-groups
- **WHEN** a group contains no sub-group directories
- **THEN** no sub-group cards are rendered (only its direct members and input bar)

### Requirement: In-group capture writes into the open group
When the user creates an entry from a group page's input bar (text, photo, audio, or imported file), the entry SHALL be written directly into the currently open group directory and indexed with that group's path, rather than being created at the month-top level and moved afterward.

#### Scenario: Text entry created inside a group
- **WHEN** the user types text in a group page's input bar and submits
- **THEN** the text entry file is created inside that group directory and appears as a direct member

#### Scenario: Photo captured inside a group
- **WHEN** the user captures a photo from a group page
- **THEN** the photo file is created inside that group directory and appears as a direct member

#### Scenario: Encrypted in-group entry
- **WHEN** the open group is in Private space and the user creates a text entry from its input bar
- **THEN** the entry is encrypted at rest, same as a month-top Private-space entry

### Requirement: Sub-group creation under an open group
The user SHALL be able to create a sub-group inside the currently open group. "新建组" invoked from within a group page (via the entry action drawer or multi-select merge) SHALL create the new group as a child of the open group, not at the month-top level.

#### Scenario: Create sub-group from entry action drawer
- **WHEN** the user is in group A, long-presses an entry, and creates a new group
- **THEN** the new group directory is created inside group A, and the entry is moved into it

#### Scenario: Multi-select merge inside a group
- **WHEN** the user is in group A, multi-selects entries, and merges them into a new group
- **THEN** the new group is created inside group A and the selected entries move into it

#### Scenario: Month-top creation unchanged
- **WHEN** the user creates a group from the main feed (no group open)
- **THEN** the group is created at the month-top level, as before

### Requirement: Parent-aware group dissolve
Dissolving a group SHALL move its members to the group's parent location: if the parent is itself a group, members move into that parent group; if the parent is the month directory, members move to month-top (current behavior). The dissolved group directory is then deleted.

#### Scenario: Dissolve a sub-group into its parent group
- **WHEN** the user dissolves a sub-group whose parent is group A
- **THEN** the sub-group's direct members move into group A, and the sub-group directory is deleted

#### Scenario: Dissolve a month-top group
- **WHEN** the user dissolves a group whose parent is the month directory
- **THEN** its members move to the month-top level (feed), and the group directory is deleted

#### Scenario: Dissolve a group that itself contains sub-groups
- **WHEN** the user dissolves a group that contains sub-group directories
- **THEN** those sub-group directories (with their contents) move into the parent location, preserving their internal nesting, and the dissolved group directory is deleted
