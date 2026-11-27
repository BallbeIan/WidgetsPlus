# Contributing

Thanks for your interest in WidgetsPlus! Bug reports, device reports and pull requests are welcome.

## Reports

Widgets are drawn by each phone's launcher, so they can look different from phone to phone. If you try WidgetsPlus on a new phone or launcher, please open a **Bug report** issue, even when everything works, and include your phone model, Android version and launcher.

## Building

See [Building from source](README.md#building-from-source). Run `.\gradlew.bat lint assembleDebug` (`./gradlew` on macOS or Linux) before opening a pull request.

## Pull requests

- Keep changes focused, and describe what you tested and on which device.
- The app is split into modules, one per widget category. Shared pieces are in `core/` (the `WidgetModule` interface, background refresh, widget building blocks, app theme). Each category has its own package, like `finance/`, with its data, screens and widgets.
- A new category implements `WidgetModule`, is added to `Modules.all`, and declares its widget receivers in the manifest. Build widgets from `core/widget` (`WidgetSurface`, `WidgetHeader`, `Charts`) and wrap their content in `StyledWidget(appWidgetId)`, so they match the rest and support the shared customisation.
- App screens use the design kit in `core/ui/CoreUi.kt` (`Page`, `Grouped`, `ListRow`, `SectionHeader`, ...), shared with the developer's other apps. Don't use plain Material cards or top bars.
- Widget receiver class names must never change once released: launchers store them, and renaming one removes that widget from users' home screens.
- No new third-party libraries without a good reason.
- Nothing may be sent except what a widget needs to fetch its data, and no widget may need an account or API key.

## Licensing of contributions

The project is under the [PolyForm Noncommercial License 1.0.0](LICENSE). By submitting a contribution you confirm that you wrote it (or have the right to submit it) and that it may be distributed under that license.

## Reporting security problems

Please do not open a public issue for a security problem. Use GitHub's private vulnerability reporting for this repository if it is enabled, or contact the maintainer through their GitHub profile.
