# DeynBook Third-Party Licenses

DeynBook uses open-source Android/JVM libraries. This file is a release-time inventory; the complete license text distributed by each upstream project remains authoritative.

## Runtime dependencies

| Component | Project / publisher | License family |
|---|---|---|
| AndroidX Core KTX | Android Open Source Project / Google | Apache License 2.0 |
| AndroidX Activity Compose | Android Open Source Project / Google | Apache License 2.0 |
| Jetpack Compose UI / Material / Material 3 | Android Open Source Project / Google | Apache License 2.0 |
| AndroidX Lifecycle | Android Open Source Project / Google | Apache License 2.0 |
| AndroidX Navigation Compose | Android Open Source Project / Google | Apache License 2.0 |
| AndroidX Room | Android Open Source Project / Google | Apache License 2.0 |
| Kotlin / Kotlin Coroutines | JetBrains | Apache License 2.0 |

## Test/build-only dependencies

| Component | Project / publisher | License family |
|---|---|---|
| JUnit 4 | JUnit project | Eclipse Public License 1.0 |
| AndroidX Test / Espresso | Android Open Source Project / Google | Apache License 2.0 |
| Robolectric | Robolectric project | MIT License |
| Roborazzi | takahirom / contributors | Apache License 2.0 |
| Kotlin Symbol Processing (KSP) | Google | Apache License 2.0 |
| Android Gradle Plugin | Android Open Source Project / Google | Apache License 2.0 |

## Notes

- No Firebase, advertising, analytics, networking, cloud database, authentication, remote AI or payment SDK is included in the DeynBook 1.0 runtime dependency set.
- Dependency versions are pinned in `gradle/libs.versions.toml` and the Android module build file.
- Before every public release, regenerate/verify this inventory against the resolved Gradle dependency graph and preserve all notices required by upstream licenses.
