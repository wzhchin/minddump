## ADDED Requirements

### Requirement: Pin and status indicators on entry cards
The system SHALL render a pin indicator on an entry card when the entry is pinned, and a status badge when the entry carries a todo status. The indicators SHALL appear in the card header, consistent with the type icon and timestamp, so a pinned or statused entry is identifiable at a glance.

#### Scenario: Pinned card shows a pin indicator
- **WHEN** an entry card for `9999-2506-13-143022-f.md` is rendered
- **THEN** its header shows a pin indicator

#### Scenario: Statused card shows a status badge
- **WHEN** an entry card for `2506-13-143022-TODO-f.md` is rendered
- **THEN** its header shows a badge labeled with the localized status text (e.g. "待办" for TODO)

#### Scenario: Done status is visually de-emphasized
- **WHEN** an entry card for `2506-13-143022-DONE-f.md` is rendered
- **THEN** the badge and/or card treatment conveys "done" distinctly from an open todo (e.g. a completed style)

#### Scenario: Plain entry shows neither
- **WHEN** an entry card for `2506-13-143022-f.md` is rendered
- **THEN** no pin indicator and no status badge are shown
