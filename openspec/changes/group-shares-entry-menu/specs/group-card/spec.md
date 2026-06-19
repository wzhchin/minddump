## MODIFIED Requirements

### Requirement: Group card interactions preserved
The system SHALL preserve tap-to-open on the group card, with tactile feedback. Long-press on a group card SHALL open the shared entry action drawer (the same drawer used for entries), not a separate group-specific sheet.

#### Scenario: Tap opens the group
- **WHEN** the user taps the group card (outside the carousel's own gestures) and multi-select is not active
- **THEN** the group detail opens

#### Scenario: Long-press opens the shared action drawer
- **WHEN** the user long-presses the group card and multi-select is not active
- **THEN** the entry action drawer opens with the group as its target
- **AND** the drawer offers the group-only "解散" action in addition to the shared pin/status/rename/share/multi-select/delete actions
- **AND** a buildup tactile feedback is emitted
- **AND** no separate group action sheet is shown

#### Scenario: Long-press in multi-select toggles selection
- **WHEN** the user long-presses a group card while in multi-select mode
- **THEN** the group's selection is toggled and no drawer opens
