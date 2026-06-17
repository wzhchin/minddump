## ADDED Requirements

### Requirement: The app exposes long-press launcher shortcuts

The system SHALL expose static launcher shortcuts available via long-pressing the app icon: "新建笔记" (New text note), "拍照" (Take photo), "录音" (Record audio), "打开公开" (Open Public), and "打开私密" (Open Private). Each shortcut SHALL launch the app directly into its corresponding action or destination, without first requiring the user to navigate the main feed.

#### Scenario: Long-press shows the shortcuts
- **WHEN** the user long-presses the app icon on the launcher
- **THEN** the five shortcuts — New text, Take photo, Record audio, Open Public, Open Private — SHALL be available

#### Scenario: Shortcut works from a cold start
- **WHEN** the app is not running and the user taps a shortcut
- **THEN** the app launches directly into that shortcut's action or destination

#### Scenario: Shortcut works from a warm start
- **WHEN** the app is already running and the user taps a shortcut
- **THEN** the running instance receives the action and performs it without relaunching

### Requirement: Each shortcut routes to an existing capture or navigation action

The shortcuts SHALL reuse the app's existing entry points rather than introducing new ones. New text SHALL open the editor in new-entry mode; Take photo SHALL open camera capture in the current space; Record audio SHALL start a recording; Open Public SHALL switch the active space to Public; Open Private SHALL request the Private space.

#### Scenario: New text opens the editor
- **WHEN** the user taps "新建笔记"
- **THEN** the fullscreen editor opens in new-entry mode, ready for input

#### Scenario: Take photo opens the camera
- **WHEN** the user taps "拍照"
- **THEN** the camera capture is launched in the current space (requesting camera permission if not already granted)

#### Scenario: Record audio starts a recording
- **WHEN** the user taps "录音"
- **THEN** an audio recording begins in the current space (requesting microphone permission if not already granted)

#### Scenario: Open Public switches to the Public space
- **WHEN** the user taps "打开公开"
- **THEN** the active space becomes Public without any authentication

### Requirement: The Open Private shortcut never bypasses authentication

The "打开私密" (Open Private) shortcut SHALL route through the same biometric/password gate as switching to Private from within the app. The shortcut SHALL NOT reveal Private content until the user has authenticated.

#### Scenario: Open Private requires authentication
- **WHEN** the user taps "打开私密" and the Private space is locked
- **THEN** the biometric/password gate is presented
- **AND** no Private content is shown until authentication succeeds

### Requirement: Capture shortcuts target the current space and do not bypass the Private gate

The Take photo and Record audio shortcuts SHALL capture into the current active space. They SHALL NOT provide a way to write into the Private space without first unlocking it. If the active space is Public (the default on a fresh launch), captures SHALL be written to Public.

#### Scenario: Capture defaults to Public on a fresh launch
- **WHEN** the app is launched fresh and the user uses a capture shortcut
- **THEN** the captured photo or recording is stored in the Public space
