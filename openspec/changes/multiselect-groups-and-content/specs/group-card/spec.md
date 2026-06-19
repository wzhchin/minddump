## ADDED Requirements

### Requirement: Group cards are selectable in multi-select
The system SHALL make group cards selectable whenever multi-select mode is active. A group card SHALL show a selection affordance (checkbox) in multi-select, and tapping a group card SHALL toggle that group's selection instead of opening the group detail. Outside multi-select, tapping a group card opens the group detail as before.

#### Scenario: Entering multi-select from a group
- **WHEN** the user long-presses a group card and chooses the "多选" action from the group action sheet
- **THEN** multi-select mode is entered with that group already selected
- **AND** the group card shows a checked checkbox

#### Scenario: Toggling a group selection
- **WHEN** the user taps a group card while in multi-select mode
- **THEN** the group is added to or removed from the selection
- **AND** the group card's checkbox reflects the new state
- **AND** the group detail does NOT open

#### Scenario: Group tap outside multi-select is unchanged
- **WHEN** the user taps a group card and multi-select mode is not active
- **THEN** the group detail opens as before

#### Scenario: Long-press on a group in multi-select
- **WHEN** the user long-presses a group card while in multi-select mode
- **THEN** the group's selection is toggled (consistent with entry behavior in multi-select)
- **AND** the group action sheet does NOT open while in multi-select

### Requirement: Group card header shows a selection affordance in multi-select
In multi-select mode the group card SHALL render a checkbox affordance (checked when the group is selected) without displacing the group name, member count, or media carousel.

#### Scenario: Selected group shows a checked affordance
- **WHEN** a group card is rendered in multi-select mode and the group is selected
- **THEN** the card shows a checked checkbox
- **AND** the media carousel, name, and member stats remain visible

#### Scenario: Unselected group shows an unchecked affordance
- **WHEN** a group card is rendered in multi-select mode and the group is not selected
- **THEN** the card shows an unchecked checkbox
