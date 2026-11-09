# Aqua Home — Frutiger Aero Android Launcher

A native Android home-screen launcher with a glossy aqua / Frutiger Aero look.
It discovers installed launcher apps on the device, uses their real icons, launches them, provides an app drawer with search, and opens Android's app-info screen on long press.

## Requirements

- Android 8.0 (API 26) or newer
- Android Studio with Android SDK Platform 35
- JDK 17

## Build in Android Studio

1. Extract the project archive.
2. Open the `AquaHomeLauncher` folder in Android Studio.
3. Allow Gradle sync and install Android SDK Platform 35 if prompted.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK with GitHub Actions

The project includes `.github/workflows/build-apk.yml`.

1. Create an empty GitHub repository.
2. Upload the contents of this project to the repository's `main` branch.
3. Open the repository's **Actions** tab and choose **Build Aqua Home APK**.
4. Press **Run workflow**. When it finishes, open the run and download the `Aqua-Home-debug` artifact.
5. Extract the artifact ZIP to get `app-debug.apk`.

The workflow also runs automatically after pushes to `main` or `master`.

## Set Aqua Home as the default launcher

1. Install `app-debug.apk` on the phone and open **Aqua Home** once.
2. Open Android Settings → Apps → Choose default apps → Home app (the exact wording can vary by device / One UI version).
3. Select **Aqua Home**.
4. Press the Home gesture or Home button to use it.

You can also open Aqua Home → **Настроить** → **Открыть настройки главного экрана**.

## Controls

- Tap an app icon to launch it.
- Long-press an app icon to open its Android app-info page.
- Open the app drawer from the search panel or swipe up on the home screen.
- Search installed apps by their visible name or package name.
- Swipe left/right on the home screen to switch between the two pages, or tap the page dots.

## Notes

- This is an installable Android launcher project, not a web mock-up.
- The app requests `QUERY_ALL_PACKAGES` so it can show the installed apps in its app drawer. Keep this permission in mind if you later plan to publish the launcher on Google Play; Play distribution has separate visibility-policy requirements.
- The weather/decoration area is intentionally decorative and does not display fabricated live weather data.
- This first version uses Android's native app icons and app launches; it does not replace the phone's notification shade, lock screen, or system settings.
