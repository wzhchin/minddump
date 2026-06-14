## ADDED Requirements

### Requirement: Pin and status indicators on group cards
The system SHALL render a pin indicator on a group card when the group is pinned, and a status badge when the group carries a todo status, placed in the group card header alongside the name and member stats.

#### Scenario: Pinned group shows a pin indicator
- **WHEN** a group card for `9999-2506-13-120000-g-travel` is rendered
- **THEN** its header shows a pin indicator

#### Scenario: Statused group shows a status badge
- **WHEN** a group card for `2506-13-120000-TODO-g-travel` is rendered
- **THEN** its header shows a status badge labeled with the localized status text

#### Scenario: Plain group shows neither
- **WHEN** a group card for `2506-13-120000-g-travel` is rendered
- **THEN** no pin indicator and no status badge are shown
