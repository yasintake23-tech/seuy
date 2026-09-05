# HeartBond Build Fix V3

Build toolchain aligned for GitHub Actions:
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.2.21
- JDK 17
- compileSdk/targetSdk 36
- Removed unused KSP/Room/Moshi code-generation plugins from the build path.
- Uses the standard Android debug signing configuration (no custom debug keystore).
- GitHub Actions uses `./gradlew :app:assembleDebug` and disables configuration cache for deterministic CI builds.

The application source and Firebase configuration from the supplied project are preserved.
