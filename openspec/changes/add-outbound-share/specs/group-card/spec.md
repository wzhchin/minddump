## ADDED Requirements

### Requirement: Group share action
The group long-press action sheet SHALL include a "分享" (Share) action that exports every member of the group to other Android apps via a single multi-content Share sheet, as defined by the `outbound-share` capability.

#### Scenario: Share a group's members
- **WHEN** the user long-presses a group card and taps "分享"
- **THEN** the action sheet SHALL dismiss and the system Share sheet SHALL open with all of the group's member entries as multiple streams

#### Scenario: Share a group with no members
- **WHEN** the user taps "分享" on a group that has no members
- **THEN** the Share sheet SHALL NOT open
