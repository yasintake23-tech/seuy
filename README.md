<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/960b98fa-3ce9-4314-8397-1ec3f9458ef8

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.


## HeartBond V8 – Build-stable relationship space

- Biz ana ekranı sadeleştirildi; yalnızca gerçek uygulama akışlarına yönlendiren içerikler bırakıldı.
- Eğlence bölümü üç gerçek sekmeye ayrıldı: Oyunlar, Planlarımız, Sorular.
- Oyunlar: Bu mu Şu mu, Doğruluk/Cesaret ve yerel kalp sayacı.
- Profil & Anılar yeniden tasarlandı; gerçek profil verileri, partner bilgisi, anı sayıları, fotoğraf ekleme ve anı detayları öne çıkarıldı.
- Harita ekranına dokunulmadı.
- Mesajlaşma ve mevcut veri/repository akışları korunmuştur.


## Build configuration
This V8 revision is aligned with Android Gradle Plugin 9.1.1, Gradle 9.3.1, JDK 17, and compile/target SDK 36. AGP 9+ built-in Kotlin is used; the legacy `kotlin-android`, `kotlin-kapt`, and `android.kotlinOptions` configuration is not present. Java 17 compile options provide the JVM target.

The GitHub Actions workflow installs the required Android 36 platform/build tools, makes `gradlew` executable, runs `:app:assembleDebug`, verifies the generated APK, and uploads it as an artifact.
