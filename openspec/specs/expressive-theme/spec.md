# Expressive Theme

## Purpose

Define the expressive theme system for MindDump, including seed-color theming with palette styles, AMOLED dark mode, theme preference persistence, theme settings entry point, and unified color baseline.

## Requirements

### Requirement: Seed-color theming with palette styles
The system SHALL theme the app from a user-chosen seed color and palette style using a dynamic material theme engine, so the entire color scheme (primary/secondary/tertiary, surfaces, containers) derives from that seed and palette, producing a cohesive flowing appearance across light, dark, and AMOLED modes.

#### Scenario: Default theme on first launch
- **WHEN** the user launches the app for the first time
- **THEN** the app is themed from a sensible default seed color and the default palette style
- **AND** the theme renders correctly in both light and dark mode following the system setting

#### Scenario: User picks a new seed color
- **WHEN** the user opens theme settings and selects a different seed color
- **THEN** the entire app re-themes immediately to derive from the new seed color
- **AND** the choice persists across app restarts

#### Scenario: User changes palette style
- **WHEN** the user selects a different palette style (e.g. TonalSpot, Vibrant, Expressive)
- **THEN** the derived color scheme updates to reflect that palette style
- **AND** the choice persists across restarts

### Requirement: AMOLED dark mode
The system SHALL offer an AMOLED toggle that, when enabled in dark mode, renders backgrounds as true black to save power on OLED displays.

#### Scenario: AMOLED enabled in dark mode
- **WHEN** the user enables AMOLED mode while the app is in dark mode
- **THEN** the background and surface tones render as near-true-black
- **AND** foreground/contrast remains legible

#### Scenario: AMOLED off
- **WHEN** AMOLED mode is disabled
- **THEN** dark mode uses the standard dark surface tones (not true black)

### Requirement: Theme preference persistence
The system SHALL persist the theme preferences (seed color, palette style, AMOLED, light/dark/system) in a preference store so the selected theme is restored exactly on every launch.

#### Scenario: Restart restores the chosen theme
- **WHEN** the user configures a theme and force-stops then relaunches the app
- **THEN** the app restores the exact seed color, palette style, AMOLED, and light/dark/system setting previously chosen

#### Scenario: Corrupted or missing preferences
- **WHEN** the preference store is unreadable or empty
- **THEN** the app falls back to the default theme without crashing

### Requirement: Theme settings entry point
The system SHALL expose theme configuration (seed color, palette style, AMOLED, light/dark/system) from the app's settings, in the user's locale (zh-CN and en).

#### Scenario: Opening theme settings
- **WHEN** the user opens Settings and selects the theme entry
- **THEN** the theme configuration panel is shown with the current selections reflected
- **AND** all labels appear in the active locale

### Requirement: Unified color baseline
The system SHALL derive all app colors (including any legacy declarative color resources) from the theme seed, eliminating the previous inconsistency between green declarative colors and purple Compose-theme colors.

#### Scenario: Colors are consistent with the seed
- **WHEN** the app renders any surface, icon, or control
- **THEN** its color is consistent with the active theme derived from the seed and palette style
- **AND** no two uncoordinated color palettes (e.g. green XML vs purple Compose) coexist
