## ADDED Requirements

### Requirement: Google Sans Flex variable font
The system SHALL bundle the Google Sans Flex variable font file in `res/font/` and use it as the primary font family for all text in the application.

#### Scenario: Font renders with rounded letterforms
- **WHEN** the app displays any text
- **THEN** the text SHALL render using Google Sans Flex with ROND axis set to 100 (fully rounded)

#### Scenario: Font file available at runtime
- **WHEN** the app is installed on a device
- **THEN** the `google_sans_flex.ttf` file SHALL be present in the APK's font resources

### Requirement: M3 Expressive typography scale
The system SHALL define a complete Material 3 typography scale using Google Sans Flex with variation settings appropriate for M3 Expressive style.

#### Scenario: Display and headline styles use wider width
- **WHEN** display or headline text is rendered
- **THEN** the font SHALL use width axis value of 110 (expanded) for visual hierarchy

#### Scenario: Body and label styles use normal width
- **WHEN** body, title, or label text is rendered
- **THEN** the font SHALL use width axis value of 100 (normal)

#### Scenario: Typography weight ladder
- **WHEN** the theme is applied
- **THEN** the typography SHALL define weights as follows: Display=Bold, Headline=SemiBold, Title=SemiBold/Medium, Body=Medium, Label=Medium

### Requirement: Typography applied via MaterialTheme
The system SHALL provide the custom typography through `MaterialTheme.typography` so all standard Material 3 components automatically use it.

#### Scenario: Material components use custom font
- **WHEN** any Material 3 component (Button, Card, TextField, etc.) renders text
- **THEN** the text SHALL use Google Sans Flex with ROND=100 without explicit font overrides

### Requirement: Fallback for old Android versions
The system SHALL gracefully handle devices below API 26 (Android 8.0) where variable font axes are not supported.

#### Scenario: Pre-API 26 device
- **WHEN** the app runs on a device below API 26
- **THEN** the font SHALL render using standard weight-based fallback without variable axes
