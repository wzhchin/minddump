## MODIFIED Requirements

### Requirement: Single entry renders as an expressive card
The system SHALL render each entry (text, photo, recording, video, file) as a card with a calm surface tone, large rounded corners, light tonal elevation, and a subtle border, rather than as a chat bubble.

#### Scenario: Entry appears as a card
- **WHEN** any entry is shown in the feed
- **THEN** it is presented as a card with a rounded-rectangle shape and a quiet surface tone
- **AND** it has a visible-but-subtle border and a light elevation that separates it from the background
- **AND** it does not resemble a left/right-aligned chat bubble

#### Scenario: Multi-select keeps the card content visible
- **WHEN** the user enters multi-select mode
- **THEN** each entry card shows a selection affordance (checkbox) and tapping a card toggles its selection
- **AND** the card body is NOT collapsed to the header — the same per-type body that renders outside multi-select (text body with expand affordance, photo/video media hero, recording chip, file chip) SHALL still render in multi-select
- **AND** the card layout remains stable (no layout jump beyond the selection affordance overlay)
