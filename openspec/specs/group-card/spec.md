# Group Card

## Purpose

Define how groups are rendered as expressive cards in the MindDump main list, including media carousel preview, header presentation, member stats, and interaction patterns.
## Requirements
### Requirement: Group card shows a media carousel preview
The system SHALL render a group as a card whose top region is a horizontally browsable media carousel previewing the group's media-bearing members (photos and video thumbnails), so the user can glance at a group's contents and swipe through them without entering the group.

#### Scenario: Group with media members
- **WHEN** a group card is rendered and the group contains photo/video members
- **THEN** the card shows a horizontal media carousel of those members' thumbnails
- **AND** the user can swipe/drag to browse them
- **AND** each carousel item is a generously sized rounded media tile

#### Scenario: Group with no media members
- **WHEN** a group card is rendered and the group contains no photo/video members (only text/files)
- **THEN** the carousel region is omitted (no empty media area is shown)

#### Scenario: Tapping a carousel item
- **WHEN** the user taps a media item in the carousel
- **THEN** the group opens (the card's primary open action is preserved)
- **OR** the tapped media opens directly, consistent with the card's open behavior

### Requirement: Group card header and stats
The system SHALL show, on the group card, the group name (or a placeholder for unnamed groups), the member count, and a row of the distinct entry types found inside with their counts.

#### Scenario: Named group
- **WHEN** a group card with a non-blank name is rendered
- **THEN** the header shows the group name and the member count

#### Scenario: Unnamed group
- **WHEN** a group card with a blank name is rendered
- **THEN** the header shows a localized placeholder name (e.g. "未命名分组") and the member count

#### Scenario: Empty group
- **WHEN** a group card with no members is rendered
- **THEN** the type-stat row shows a localized empty indication (e.g. "空分组")

### Requirement: Group card interactions preserved
The system SHALL preserve tap-to-open and long-press-to-open-menu on the group card, with tactile feedback.

#### Scenario: Tap opens the group
- **WHEN** the user taps the group card (outside the carousel's own gestures)
- **THEN** the group detail opens

#### Scenario: Long-press opens the group action menu
- **WHEN** the user long-presses the group card
- **THEN** the group action menu opens
- **AND** a buildup tactile feedback is emitted

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

