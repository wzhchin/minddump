## MODIFIED Requirements

### Requirement: Group card shows a media carousel preview
The system SHALL render a group as a card whose top region is a horizontally browsable media carousel previewing the group's media-bearing members (photos and video thumbnails), so the user can glance at a group's contents and swipe through them without entering the group. The video play overlay in the carousel SHALL share the same component and size specification as the video play overlay used on individual entry cards.

#### Scenario: Group with media members
- **WHEN** a group card is rendered and the group contains photo/video members
- **THEN** the card shows a horizontal media carousel of those members' thumbnails
- **AND** the user can swipe/drag to browse them
- **AND** each carousel item is a generously sized rounded media tile

#### Scenario: Video carousel tiles use the shared play overlay
- **WHEN** a video member is rendered as a carousel tile
- **THEN** its play overlay is rendered by the same component and at the same size as the play overlay on a video entry card (no two different sizes for the same affordance)

#### Scenario: Group with no media members
- **WHEN** a group card is rendered and the group contains no photo/video members (only text/files)
- **THEN** the carousel region is omitted (no empty media area is shown)

#### Scenario: Tapping a carousel item
- **WHEN** the user taps a media item in the carousel
- **THEN** the group opens (the card's primary open action is preserved)
- **OR** the tapped media opens directly, consistent with the card's open behavior
