# HeartBond V10 – Runtime, Firebase and UX stabilization

- Moved chat notifications to FCM/Cloud Functions; removed obsolete foreground polling service and boot receiver.
- Added one-time stale notification cleanup for installs migrated from the old notification implementation.
- Chat now observes only the newest 30 Firestore messages and loads older pages on demand while preserving the scroll anchor.
- Added Firebase-backed Heart Wars with animated jars and live counters.
- Added Firebase-backed relationship start date/time with a live days/hours/minutes/seconds counter.
- Simplified Settings to functional categories only.
- Removed Games content and dead quick-access action until games are implemented.
- Improved type readability and dark-theme contrast in Settings.
- Added FCM token synchronization and notification preference storage.
- Added couple-scoped Firestore rules and Firebase Functions configuration (Node 22).

Gradle itself could not be executed in this sandbox because services.gradle.org was unreachable.
The project is structurally validated (JSON/XML/JS/delimiter checks); final verification remains the GitHub Actions build.
