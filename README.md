<div align="center">

# WidgetsPlus

**Clean home screen widgets for stock and crypto prices.** No account, no API key, no tracking.

![License: PolyForm Noncommercial](https://img.shields.io/badge/license-PolyForm%20Noncommercial-blue)

</div>

---

**Early version.** Downloads will appear on the [Releases](https://github.com/BallbeIan/WidgetsPlus/releases) page. A Google Play release is planned.

## What it does

Make a list of the stocks, funds, indices and coins you follow, and WidgetsPlus shows their prices on your home screen.

- **Single symbol widget:** one stock or coin with its price, the change today (stocks) or over 24 hours (crypto), and a chart. Resize it to show more or less.
- **Watchlist widget:** your whole list, scrolling, with small charts when the widget is wide enough.
- **Stocks, funds and indices** use the symbols shown on Yahoo Finance: `AAPL`, `^GSPC` (S&P 500), `VWCE.DE`.
- **Crypto** uses the pairs traded on Coinbase: `BTC` (in US dollars) or `ETH-EUR`.
- Prices update about every 30 minutes in the background, or tap the refresh button. Tap a symbol to open it in Yahoo Finance or Coinbase.
- Follows your phone's light or dark mode and, on Android 12 and newer, its wallpaper colours.

## Installing

1. On your phone, open the [latest release](https://github.com/BallbeIan/WidgetsPlus/releases/latest) and download `WidgetsPlus-<version>.apk`. Open it and allow your browser or file manager to install apps when Android asks.
2. Open **WidgetsPlus** and add the stocks and coins you want to follow.
3. Long-press your home screen, tap **Widgets**, find **WidgetsPlus** and drag a widget onto the screen.

To check the download, compare its SHA-256 with the `.sha256` file on the release page. The APK is signed with the project's release key, whose certificate SHA-256 is `95d6c134d8f813a49612437ac9fe1d9d46ab03361662d3fb9c249cf23fd17bed` (check it with `apksigner verify --print-certs`). Updates install over an existing install and keep your list.

Once WidgetsPlus is on Google Play, the Play version and this APK are signed differently and cannot update each other. To switch, uninstall one and install the other, then add your symbols again.

## Requirements

Android 8.0 or newer and an internet connection.

## Where the prices come from

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

WidgetsPlus has no account and collects nothing. Your list stays on your phone. Only the symbol being looked up is sent to Yahoo Finance or Coinbase. See the [privacy policy](docs/PRIVACY.md).

## Contributing

Bug reports, device reports and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

WidgetsPlus is **source-available** under the [PolyForm Noncommercial License 1.0.0](LICENSE). You may read, learn from, modify and build it for personal and other noncommercial use. Commercial use, including selling or publishing it, is not permitted.
