# WidgetsPlus privacy policy

*Effective 27 November 2026 (updated for version 0.3)*

WidgetsPlus is a collection of home screen widgets for Android. It has clock, weather, countdown and finance widgets. This page explains what the app does with your information. The short version: **it collects nothing about you. Weather widgets ask Open-Meteo for the forecast of the places you add, and Finance widgets ask Yahoo Finance and Coinbase for the prices of the symbols in your lists.**

## What WidgetsPlus stores

Your settings (such as your places, events and lists of symbols), the last data fetched and what each widget shows are stored **only on your phone**, in the app's private storage. The developer cannot see them. There is no WidgetsPlus server.

## What is sent over the internet

The Clock and Days widgets never use the internet. The others connect directly from your phone to:

- **Open-Meteo** (`api.open-meteo.com` and `geocoding-api.open-meteo.com`) for weather. Searching for a place sends the name you type; forecasts send the coordinates of the town you picked, never your phone's own location (WidgetsPlus does not use the location permission).

- **Yahoo Finance** (`query1.finance.yahoo.com`) for stocks, funds and indices.
- **Coinbase** (`api.exchange.coinbase.com` and `api.coinbase.com`) for cryptocurrencies.

Finance requests contain only the symbol being looked up (for example `AAPL` or `BTC-USD`). No account, name, email, location, device identifier or list of your other symbols is sent. Like any website, these services can see your IP address and the time of the request; their own privacy policies cover how they handle that:

- Open-Meteo: <https://open-meteo.com/en/terms#privacy>
- Yahoo: <https://legal.yahoo.com/us/en/yahoo/privacy/index.html>
- Coinbase: <https://www.coinbase.com/legal/privacy>

If future widget categories need other online sources, they will be listed here before they are released.

WidgetsPlus has no accounts, no sign-in, no advertising, no analytics, no crash reporting and no third-party tracking.

## Opening other apps

Tapping a clock opens your phone's clock app. Tapping a symbol on a widget opens its page on Yahoo Finance or Coinbase, in their app if you have it installed or in your browser. From then on you are using their service under their terms.

## Permissions

| Permission | Why | Required? |
|---|---|---|
| Internet | To fetch data for widgets that need it (Weather: Open-Meteo; Finance: Yahoo Finance and Coinbase). | Yes |
| Run at startup, keep awake, foreground service, network state | Declared by the Android WorkManager library so the widgets keep updating in the background and after a restart. | Automatic |

## Backups

Android may include an app's data in the device backup you control in your phone's settings (for example a Google backup). That is handled by your phone and your Google account under their own terms, not by WidgetsPlus. You can turn it off in your phone's settings.

## Deleting your data

Uninstalling WidgetsPlus, or choosing *Clear storage* in the app's system settings, removes all of its data from your phone.

## Children

WidgetsPlus is a general-purpose tool and is not aimed at children. It collects no personal information from anyone.

## Changes

If this policy changes, the new version will be published at this address with a new effective date.

## Contact

Questions or concerns: open an issue at <https://github.com/BallbeIan/WidgetsPlus/issues>.
