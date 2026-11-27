<div align="center">

# WidgetsPlus

**Clean, simple home screen widgets for Android.** No account, no API key, no tracking.

![License: PolyForm Noncommercial](https://img.shields.io/badge/license-PolyForm%20Noncommercial-blue)

</div>

---

**Early version.** Downloads will appear on the [Releases](https://github.com/BallbeIan/WidgetsPlus/releases) page. A Google Play release is planned.

## What it does

WidgetsPlus is a collection of home screen widgets, grouped by category. The app is where you set them up: the **Widgets** tab lists every WidgetsPlus widget on your home screen and every widget you can add, and categories with something to set up (lists, places, events) have their own tab.

### Make each widget yours

Tap a widget on the Widgets tab to open its editor, with a live preview of the real widget:

- **What it shows:** the list, place, event, symbol or time zone.
- **Background:** your phone's colours (follows the wallpaper and dark mode), or white, black and a set of tints.
- **Background opacity:** down to fully transparent, so the wallpaper shows through.
- **Text colour:** automatic, light or dark.
- **Corner roundness:** from square to very round (Android 12 and newer).

Changes apply to the widget on your home screen straight away, and one look can be applied to all widgets at once.

### Clock

- **Clock:** time and date in large, clean type. It can show another city's time with a label, for a world clock.
- **Analog clock:** a clock face in your phone's colours.
- Clocks keep time by themselves, so they use no battery to tick. Tap one to open your alarms.

### Weather

- Add places by searching for a city. No location permission is needed.
- The **Weather** widget shows the current temperature and conditions. Make it bigger to add the next hours, then the coming days.
- °C or °F. Weather data by [Open-Meteo](https://open-meteo.com) (CC BY 4.0).

### Days

- **Countdown:** days to go until an event you add, such as a holiday or a birthday (with yearly repeat), or days since it.
- **Progress:** how far through today, this week, this month and this year you are.

### Finance

Stock and crypto prices, from lists you make yourself.

- **Lists:** start with *Stocks* and *Crypto*, rename them, or make your own (for example *Tech* or *ETFs*).
- **Price list widget:** shows one list, so stocks and crypto can sit on separate widgets. Scrolls, and shows small charts when the widget is wide enough.
- **Single price widget:** one stock or coin with its price, the change today (stocks) or over 24 hours (crypto), and a chart. Resize it to show more or less.
- **Stocks, funds and indices** use the symbols shown on Yahoo Finance: `AAPL`, `^GSPC` (S&P 500), `VWCE.DE`.
- **Crypto** uses the pairs traded on Coinbase: `BTC` (in US dollars) or `ETH-EUR`.
- Prices update about every 30 minutes in the background, or tap the refresh button. Tap a symbol to open it in Yahoo Finance or Coinbase.

More categories are planned.


Unless you choose otherwise, widgets follow your phone's light or dark mode and, on Android 12 and newer, its wallpaper colours.

## Installing

1. On your phone, open the [latest release](https://github.com/BallbeIan/WidgetsPlus/releases/latest) and download `WidgetsPlus-<version>.apk`. Open it and allow your browser or file manager to install apps when Android asks.
2. Open **WidgetsPlus**, open a category (for example **Finance**) and set it up.
3. Tap **Add** next to a widget, or long-press your home screen, tap **Widgets**, find **WidgetsPlus** and drag a widget onto the screen.

To check the download, compare its SHA-256 with the `.sha256` file on the release page. The APK is signed with the project's release key, whose certificate SHA-256 is `95d6c134d8f813a49612437ac9fe1d9d46ab03361662d3fb9c249cf23fd17bed` (check it with `apksigner verify --print-certs`). Updates install over an existing install and keep your list.

Once WidgetsPlus is on Google Play, the Play version and this APK are signed differently and cannot update each other. To switch, uninstall one and install the other, then set up your widgets again.

## Requirements

Android 8.0 or newer. Weather and Finance need an internet connection.

## Where the data comes from

### Weather

Forecasts and place search come from [Open-Meteo](https://open-meteo.com), which is free for non-commercial use and needs no key. Its data is licensed under CC BY 4.0.

### Finance

| What | Source | Notes |
|---|---|---|
| Stocks, funds, indices | Yahoo Finance | Yahoo's public chart data, which has no official API. If Yahoo changes it, the stock widgets stop updating until the app is fixed. |
| Crypto | Coinbase Exchange public API | Official and needs no key. |

Prices can be delayed (stock prices often by 15 minutes or more, depending on the exchange). They are for information only and are not investment advice. WidgetsPlus is not affiliated with Yahoo or Coinbase.

## Building from source

You need JDK 17 and the Android SDK (platform 36, build-tools 36.0.0).

```powershell
git clone https://github.com/BallbeIan/WidgetsPlus.git
cd WidgetsPlus
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug    # app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat installDebug     # onto a connected phone
.\gradlew.bat lint
```

On macOS or Linux, use `export ANDROID_HOME=~/Android/Sdk` and `./gradlew` instead.

Or open the folder in Android Studio and run it on a device. Debug builds install as **WidgetsPlus (debug)** next to the release version.

Release builds are signed with the key in `keystore.properties` (copy `keystore.properties.example`; the file and the key are git-ignored). Without it, `assembleRelease` falls back to the debug key, which is fine for local testing but not for publishing.

## Privacy

WidgetsPlus has no account and collects nothing. Your settings stay on your phone. Widgets that need online data fetch it directly from the source: for Weather, only the searched place name or the place's coordinates are sent to Open-Meteo; for Finance, only the symbol being looked up is sent to Yahoo Finance or Coinbase. Clock and Days work entirely offline. See the [privacy policy](docs/PRIVACY.md).

## Credits

The interface uses [Inter](https://github.com/rsms/inter), bundled in the app, under the SIL Open Font License ([`docs/licenses/Inter-OFL.txt`](docs/licenses/Inter-OFL.txt)). Weather data by [Open-Meteo](https://open-meteo.com) (CC BY 4.0).

## Contributing

Bug reports, device reports and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

WidgetsPlus is **source-available** under the [PolyForm Noncommercial License 1.0.0](LICENSE). You may read, learn from, modify and build it for personal and other noncommercial use. Commercial use, including selling or publishing it, is not permitted.
