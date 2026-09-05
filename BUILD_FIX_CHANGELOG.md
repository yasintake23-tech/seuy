# HeartBond V8 Build Fix Changelog

Changed files:

- `app/src/main/java/com/example/MainActivity.kt`
  - Removed invalid same-package import for `UnpairConfirmationDialog`.

- `app/src/test/java/com/example/GreetingScreenshotTest.kt`
  - Removed stale `onGoogleSignIn` argument from the `AuthScreen` call.

- `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`
  - Replaced the stale hard-coded package assertion with `BuildConfig.APPLICATION_ID`.

- `BUILD_NOTES.md`
  - Updated build verification and documented the concrete blockers/fixes and environment limitation.

No application feature flow was intentionally removed; the changes above are compatibility/build corrections.
