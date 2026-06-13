## 1. Version Catalog Updates

- [x] 1.1 Update `kotlin` version to `"2.2.0"` in `gradle/libs.versions.toml`
- [x] 1.2 Update `ksp` version to the `2.2.0-x.x.x` release in `gradle/libs.versions.toml`
- [x] 1.3 Update `coil` version to `"3.2.0"` in `gradle/libs.versions.toml`

## 2. Dependency Compatibility Verification

- [x] 2.1 Run `./gradlew assembleDebug` and verify Kotlin + KSP compilation succeeds
- [x] 2.2 Check if detekt `1.23.7` is compatible with Kotlin 2.2.0; bump if needed → bumped to 1.23.8
- [x] 2.3 Check if ktlint `12.1.1` is compatible with Kotlin 2.2.0; bump if needed → bumped to 14.2.0
- [x] 2.4 Check if Compose BOM `2024.12.01` works with Kotlin 2.2.0 Compose plugin; bump if needed → works as-is
- [x] 2.5 Check if Room `2.6.1` KSP processor works with new KSP version; bump if needed → bumped to 2.8.4
- [x] 2.6 Check if Hilt `2.53.1` KSP processor works with new KSP version; bump if needed → bumped to 2.59.2, AGP bumped to 9.2.1

## 3. Source-Level Fixes

- [x] 3.1 Fix any Kotlin 2.2.0 deprecation warnings or compilation errors in source files → removed kotlin-android plugin, migrated kotlinOptions to compilerOptions DSL
- [x] 3.2 Fix any Coil 3.2.0 API deprecation warnings or compilation errors in source files → no changes needed, API compatible
- [x] 3.3 Update ProGuard/R8 rules if Coil 3.2.0 requires changes → no changes needed

## 4. Verification

- [x] 4.1 Run `./gradlew assembleDebug` — full clean build passes
- [x] 4.2 Run `./gradlew detekt` — static analysis passes (pre-existing issues only, no new failures)
- [x] 4.3 Run `./gradlew ktlintCheck` — lint passes (pre-existing issues only, no new failures)
- [ ] 4.4 Verify Coil image loading works in app (thumbnails render, image viewer opens) — requires device/emulator
