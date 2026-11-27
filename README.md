# SoraCast

SoraCast is a customized Android streaming client built on the CloudStream architecture with updated branding, refined navigation spacing, inflation stability fixes, and direct GitHub release updates.

## Key Changes & Features

- **Custom SoraCast Branding:** Updated launcher icons (`mipmap-*`), adaptive icon layers, legacy splash screen (`splash_background.xml`), and Android 12+ splash screen (`values-v31/styles.xml`) using `ic_soracast_logo` on a `#242855` background.
- **Proportional Top Category Bar Spacing:** Standardized top padding (`56.dp`) and hero clearance (`48.dp`) in `FloatingDockSoraCastScreen.kt` across all sections (`All`, `Movies`, `Anime`, `Live`) so content aligns cleanly below the floating category bar without overlap or excess gap.
- **Download Button Inflation Fix:** Resolved `InflateException` / `NumberFormatException` crashes when opening item detail screens (`fragment_result` / `fragment_result_swipe`) by replacing unresolved theme attribute lookups in `download_button_view.xml` with explicit values.
- **GitHub Self-Updater Integration:** Configured `InAppUpdater.kt`, `GithubReleases.kt`, and `SettingsGeneralScreen.kt` to check `robbisardar39-jpg/SoraCast-` for new releases.

## Installation

1. Download `SoraCast-release-signed.apk` from the [Releases](https://github.com/robbisardar39-jpg/SoraCast-/releases) page.
2. Enable **Install from unknown sources** if prompted by Android.
3. Install and launch SoraCast.
