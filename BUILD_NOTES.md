# HeartBond V8 — Build Verification Notes

This package is based on the supplied V7 project and was rechecked before packaging.

## Build configuration
- Android Gradle Plugin: 9.1.1
- Gradle Wrapper: 9.3.1
- Gradle distribution: `gradle-9.3.1-bin.zip`
- Wrapper SHA-256: `b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06`
- Wrapper JAR SHA-256: `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`
- Java/JVM target: 17
- compileSdk / targetSdk: 36
- Application versionCode / versionName: 2 / 1.1

## V7 → V8 changes actually present in this archive
- Removed the invalid/missing `libs.plugins.kotlin.android` reference from the root build file.
- Kept AGP 9 built-in Kotlin and the Compose compiler plugin; no legacy `org.jetbrains.kotlin.android` or `org.jetbrains.kotlin.kapt` plugin is applied.
- No `android.kotlinOptions` block remains. Java 17 is configured through `compileOptions`.
- Kept the Gradle 9.3.1 binary distribution and its matching official SHA-256 checksum.
- Kept the official Gradle 9.3.1 wrapper JAR.
- GitHub Actions explicitly installs Android platform 36 and Build Tools 36.0.0 and performs `chmod +x ./gradlew` before the build.
- Build command is executed through `bash ./gradlew :app:assembleDebug` with plain console output and a timeout.
- APK existence is checked before artifact upload.
- Synchronized the displayed application version with the About screen (`1.1`).

## Static verification performed
- ZIP integrity checked with `unzip -t`.
- `gradlew` has executable mode 755 inside the package.
- Gradle wrapper JAR checksum matches the official Gradle 9.3.1 checksum.
- Kotlin source delimiters were checked across the complete source tree.
- XML resources were parsed successfully.
- Google Services package in `google-services.json` matches the app `applicationId` (`com.aistudio.ikimiz.app`).
- No references to `kotlin-android`, `kotlin-kapt`, `android.kotlinOptions`, or kapt configuration remain in the project.
- Main source/resource references were scanned for obvious missing internal resources/classes.

## Important limitation
A complete Android compilation could not be executed in this environment because the environment cannot reach the external Gradle/Maven services needed to resolve the Android toolchain and dependencies. The package therefore has been statically validated, but the final proof of a successful APK build remains the GitHub Actions run using this exact archive after it is pushed.
