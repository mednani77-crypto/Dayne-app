# DeynBook — دفتر الديون

DeynBook is an offline-first Android debt ledger for small shops, traders and personal accounts in Djibouti, Ethiopia, Somalia, Somaliland, Kenya and neighboring markets.

## What it does

- Tracks customer receivables and supplier payables separately.
- Keeps customer and supplier ledgers separate even when the same person is both.
- Supports DJF, ETB, SOS, SLS, USD and KES plus custom currencies.
- Supports Arabic, Somali, Amharic, French and English.
- All bookkeeping and standard report export works without an Internet connection or user account.
- Generates statement PDFs, shareable summary images and text summaries.
- Exports localized PDF, image, text, CSV and Excel reports independently of the app language.
- Optionally translates user-entered report content when the user requests it and Internet is available.
- Creates and restores local JSON backups.
- Includes daily, 7-day, 30-day, monthly and custom-period reports.

## Technology

- Kotlin
- Jetpack Compose / Material 3
- Room / SQLite
- Kotlin Coroutines / Flow
- Android SDK 36
- Minimum Android SDK 23

The app requests `INTERNET` only for the optional, user-initiated report-content translation feature. Android cloud backup is disabled.

## Project structure

- `app/src/main/java/com/example/data/` — Room entities, DAOs, models and repository.
- `app/src/main/java/com/example/ui/` — Compose screens and view models.
- `app/src/main/java/com/example/services/` — PDF, image, sharing and statement helpers.
- `app/src/test/` — financial, backup and repository tests.
- `PRIVACY_POLICY.md` — privacy policy source.
- `PLAY_STORE_RELEASE.md` — localized Play Store copy, Data Safety draft and release checklist.
- `THIRD_PARTY_LICENSES.md` — dependency license summary.

## Development requirements

Use a current Android Studio with Android SDK 36 installed. CI uses:

- JDK 21
- Gradle 9.3.1
- Android platform 36
- Android build tools 36.0.0

This repository currently relies on an installed Gradle distribution rather than a committed wrapper JAR. In Android Studio, Gradle can be configured automatically. On the command line, ensure Gradle 9.3.1 is available.

## Build and test

From the repository root:

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon lintDebug
gradle --no-daemon assembleDebug
gradle --no-daemon bundleRelease
```

GitHub Actions runs the same gate on pull requests and on every push to `main` and uploads a debug APK plus an unsigned release AAB when all checks succeed.

## Release signing

The repository intentionally contains no private keystore and no signing password.

For Google Play:

1. Create or use a long-lived upload key outside Git.
2. Keep the keystore and passwords in a secure password/secret manager.
3. Sign the release bundle with that upload key.
4. Use Google Play App Signing for the production application-signing key.
5. Never commit `.jks`, `.keystore`, passwords or signing environment files.

The AAB produced by public CI is deliberately **unsigned** and is a build-verification artifact, not a Play-upload credential.

## Adding a currency

Built-in currencies are defined in `AppDatabase.kt`. A user can also create a custom currency from Settings. Monetary values are stored as integer minor units (`Long`) rather than floating-point numbers.

## Adding a language

Language metadata and localized strings live under `core/localization`. Any new language must include all user-facing strings, correct text direction and PDF/image rendering validation.

## Optional online report translation

Built-in report labels are translated locally and never need Internet. Users can additionally choose original content, translated content, or a bilingual report for names, notes, ledger identity and other user-entered text. Online translation uses Google Cloud Translation only after the user selects a translation mode.

The API key is deliberately absent from the repository. Configure it with one of these sources before producing a translation-enabled build:

- `DEYNBOOK_TRANSLATION_API_KEY` environment variable;
- `DEYNBOOK_TRANSLATION_API_KEY` Gradle property; or
- `DEYNBOOK_TRANSLATION_API_KEY=...` in the untracked `local.properties` file.

Restrict the key to the Cloud Translation API and the Android package/signing certificate. Builds without a key keep all offline export modes available and show a clear message if online translation is requested.

## Database migrations

Room destructive migration fallback is intentionally disabled. When increasing the Room schema version, add an explicit migration and tests so existing ledger data is never silently wiped.

## Privacy

DeynBook has no application server, advertising SDK, analytics SDK, cloud database or account system. Ledger data stays on device unless the user explicitly exports/shares a file or requests online translation of entered report text. See `PRIVACY_POLICY.md`.

## Release checklist

See `PLAY_STORE_RELEASE.md` for localized listing copy, permissions, Data Safety guidance and submission steps.
