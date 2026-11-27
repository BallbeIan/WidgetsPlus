# Contributing

Thanks for your interest in WidgetsPlus! Bug reports, device reports and pull requests are welcome.

## Reports

Widgets are drawn by each phone's launcher, so they can look different from phone to phone. If you try WidgetsPlus on a new phone or launcher, please open a **Bug report** issue, even when everything works, and include your phone model, Android version and launcher.

## Building

See [Building from source](README.md#building-from-source). Run `.\gradlew.bat lint assembleDebug` (`./gradlew` on macOS or Linux) before opening a pull request.

## Pull requests

- Keep changes focused, and describe what you tested and on which device.
- Match the existing style: price fetching in `PriceSources`, stored state in `Repository`, widgets built with Jetpack Glance.
- No new third-party libraries without a good reason.
- The app must not send anything except the symbol being looked up, and must not need an account or API key.

## Licensing of contributions

The project is under the [PolyForm Noncommercial License 1.0.0](LICENSE). By submitting a contribution you confirm that you wrote it (or have the right to submit it) and that it may be distributed under that license.

## Reporting security problems

Please do not open a public issue for a security problem. Use GitHub's private vulnerability reporting for this repository if it is enabled, or contact the maintainer through their GitHub profile.
