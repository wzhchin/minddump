## Requirement: Card surfaces tags and reminders in a footer

An entry card SHALL render a footer row below its per-type content showing the entry's tags and a reminder chip, so that sidecar metadata is visible at a glance while scanning the feed. The footer SHALL be omitted entirely when the entry has no tags and no events, and SHALL be omitted during multi-select mode. Each tag SHALL be rendered as a tonal chip with a leading `#`. The reminder chip SHALL summarize the soonest pending event (or, when all events have fired, the most-recent fired event) as a friendly local time with a bell indicator; fired reminders SHALL be visually de-emphasized.

#### Scenario: Card with tags shows tag chips
- **WHEN** an entry card carrying the tags "idea" and "work" is rendered (outside multi-select)
- **THEN** its footer SHALL show two tonal chips "#idea" and "#work"

#### Scenario: Card with a pending reminder shows the reminder chip
- **WHEN** an entry card with a pending event due "今天 14:30" is rendered
- **THEN** its footer SHALL show a reminder chip containing a bell indicator and "今天 14:30"

#### Scenario: Card with only fired reminders de-emphasizes them
- **WHEN** an entry card whose events are all fired is rendered
- **THEN** the reminder chip SHALL still appear but SHALL be visually de-emphasized (e.g. reduced alpha)

#### Scenario: Plain card has no footer
- **WHEN** an entry card with no tags and no events is rendered
- **THEN** no footer row SHALL be rendered

#### Scenario: Footer hidden in multi-select
- **WHEN** the user enters multi-select mode
- **THEN** entry cards SHALL NOT render the tag/reminder footer
