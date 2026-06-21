## MODIFIED Requirements

### Requirement: Each shortcut routes to an existing capture or navigation action

The shortcuts SHALL reuse the app's existing entry points rather than introducing new ones. New text SHALL open the editor in new-entry mode; Take photo SHALL launch the system camera to capture into the current space; Record audio SHALL start a recording; Open Public SHALL switch the active space to Public; Open Private SHALL request the Private space.

#### Scenario: Take photo opens the camera
- **WHEN** the user activates the "拍照" (Take photo) shortcut
- **THEN** the system camera is launched in the current space (requesting camera permission if not already granted), and a successful capture is indexed into the feed

#### Scenario: Record audio starts a recording
- **WHEN** the user activates the "录音" (Record audio) shortcut
- **THEN** an audio recording begins in the current space (requesting microphone permission if not already granted)
