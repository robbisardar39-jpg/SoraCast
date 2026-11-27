# Changelog

All notable changes to SoraCast are documented in this file.

## [1.0.0] - 2026-11-27

### Added
- Custom SoraCast app logo (`ic_soracast_logo`) across all launcher densities (`mdpi` through `xxxhdpi`), adaptive icon XMLs, and Android 12+ (`values-v31`) splash screen resources with `#242855` background.
- Project documentation (`README.md` and `CHANGELOG.md`) and `.gitignore` rules for local APK/signature build artifacts.

### Changed
- Adjusted top category bar clearance in `FloatingDockSoraCastScreen.kt` (`56.dp` top content padding and `48.dp` spacer) for proportional spacing across `All`, `Movies`, `Anime`, and `Live`.
- Pointed in-app update checks in `InAppUpdater.kt`, `GithubReleases.kt`, and `SettingsGeneralScreen.kt` to `robbisardar39-jpg/SoraCast-`.

### Fixed
- Fixed `InflateException` (`NumberFormatException` on `?attr/download_icon_scale` and tint resolution) in `download_button_view.xml` that prevented item details pages from opening.
