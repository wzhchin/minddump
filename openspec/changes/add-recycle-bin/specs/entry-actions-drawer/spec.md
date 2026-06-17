## MODIFIED Requirements

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
