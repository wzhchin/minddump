## Why

The project is pinned to Kotlin 2.0.21, which caps Coil at 3.0.4 — a version that lacks multiplatform improvements and bug fixes available in newer releases. Upgrading to Kotlin 2.2.0 unlocks the full Coil 3.2.x line and brings K2 compiler maturity, performance gains, and new language features.

## What Changes

- **BREAKING**: Kotlin `2.0.21` → `2.2.0` — all Kotlin-dependent plugins (KSP, Compose compiler) must be updated in lockstep
- **BREAKING**: KSP `2.0.21-1.0.28` → `2.2.0-x.x.x` — must match the new Kotlin version
- Coil `3.0.4` → `3.2.0` — image loading library upgrade
- Verify all Compose BOM, Hilt, Room, and other dependencies remain compatible with Kotlin 2.2.0
- Fix any source-level breakages from Kotlin language/stdlib changes

## Capabilities

### New Capabilities
- `kotlin-2.2-toolchain`: Upgrade the Kotlin toolchain to 2.2.0 — covering KSP version alignment, Compose plugin compatibility, and any source-level migration fixes
- `coil-3.2-upgrade`: Upgrade Coil from 3.0.4 to 3.2.0 — covering dependency changes, API deprecations, and compatibility verification

### Modified Capabilities
<!-- No existing specs to modify — openspec/specs/ is empty -->

## Impact

- **Build files**: `gradle/libs.versions.toml` (primary), possibly `build.gradle.kts` files if plugin application changes
- **KSP-dependent processors**: Room compiler, Hilt compiler — must be verified against Kotlin 2.2.0
- **Source code**: Potential deprecation warnings or breakages from Kotlin stdlib/language changes
- **CI**: Build cache may need invalidation; detekt/ktlint plugins must support Kotlin 2.2.0
- **Dependencies that may need bumps**: Compose BOM, AGP, detekt, ktlint — if they don't support Kotlin 2.2.0 at current versions
