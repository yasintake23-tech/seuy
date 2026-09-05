# HeartBond V8 — Build Verification Notes

## Project purpose
`İkimiz` is a relationship/couple space for two paired users. The current codebase contains Firebase Authentication, Firestore/Realtime Database synchronization, couple pairing, 5-tab navigation (Biz / Eğlence / Harita / Mesajlar / Profil), realtime chat, typing/read state, memories/photos, bucket-list activities, daily questions, secret notes, status/memory pins, local profile caching, notifications and optional Cloudflare R2 media storage.

## Build configuration
- Android Gradle Plugin: 9.1.1
- Gradle Wrapper: 9.3.1
- Java/JVM target: 17
- compileSdk / targetSdk: 36
- Application ID: `com.aistudio.ikimiz.app`
- Namespace: `com.example`
- Version: `2 (1.1)`

AGP 9.1.1 officially uses Gradle 9.3.1 as its default/minimum compatible Gradle version and requires JDK 17; the project is aligned with that toolchain.

## Build blockers fixed in this package
1. `MainActivity.kt` imported `com.example.ui.screens.UnpairConfirmationDialog`, but that function is declared in `SettingsDialog.kt` in the same `com.example.ui.screens` package. The invalid import was removed. The function remains callable without an import.
2. `GreetingScreenshotTest.kt` called `AuthScreen` with a stale `onGoogleSignIn` named argument even though `AuthScreen` no longer declares that parameter. The stale argument was removed.
3. `ExampleInstrumentedTest.kt` expected the old package name `com.example`; it now asserts against the generated `BuildConfig.APPLICATION_ID`, matching the actual application ID.

## Static verification completed
- All Kotlin source braces/parentheses/brackets balance in the project source tree.
- All `com.example.*` imports were checked against the actual source declarations/paths; no unresolved internal project imports remain after the fixes above.
- No remaining `onGoogleSignIn` references exist.
- XML resources parse successfully.
- The Gradle wrapper JAR SHA-256 matches the recorded official Gradle 9.3.1 wrapper checksum: `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`.
- `google-services.json` contains the same Android package ID as the app's `applicationId`: `com.aistudio.ikimiz.app`.
- No legacy `kotlin-android`, `kotlin-kapt`, `android.kotlinOptions`, or kapt configuration is present.

## CI workflow
`.github/workflows/build-apk.yml`:
- checks out the repository
- installs JDK 17
- installs Android platform 36 and Build Tools 36.0.0
- validates the Gradle wrapper
- uses `gradle/actions/setup-gradle@v6`
- executes `:app:assembleDebug`
- verifies an APK exists
- uploads `app/build/outputs/apk/debug/*.apk` as `HeartBond-debug-apk`

## Local build limitation in this environment
A complete Android compilation could not be executed here because this environment cannot reach the external Gradle/Maven services required to download Gradle and dependencies. The wrapper attempted to download Gradle 9.3.1 and failed with `UnknownHostException: services.gradle.org`.

Therefore, the archive has been statically checked and the concrete source/test build blockers found in the supplied archive have been fixed, but GitHub Actions remains the authoritative end-to-end APK build verification because it has network access to the Android/Gradle dependency repositories.

## Remaining runtime/architecture observations (not APK build blockers)
- Cloudflare R2 credentials are currently hard-coded in `R2Config.kt`. A production architecture should move media uploads behind a trusted backend or short-lived signed upload flow rather than shipping long-lived storage credentials inside the APK.
- Chat currently mirrors writes to both Firebase Realtime Database and Firestore. That is intentional for realtime UX/recovery, but it increases write volume and requires consistent security rules.
- The `.env.example` mentions a Gemini API key, but the current Android module does not declare or use a Gemini dependency/provider. This is documentation drift rather than a current build failure.
