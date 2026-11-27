# WidgetsPlus privacy policy

*Effective 27 November 2026*

WidgetsPlus shows stock and cryptocurrency prices in home screen widgets on Android. This page explains what the app does with your information. The short version: **it collects nothing about you. It only asks Yahoo Finance and Coinbase for the prices of the symbols in your list.**

## What WidgetsPlus stores

Your list of symbols, the last prices fetched and which symbol each widget shows are stored **only on your phone**, in the app's private storage. The developer cannot see them. There is no WidgetsPlus server.

## What is sent over the internet

To show prices, WidgetsPlus connects directly from your phone to:

- **Yahoo Finance** (`query1.finance.yahoo.com`) for stocks, funds and indices.
- **Coinbase** (`api.exchange.coinbase.com` and `api.coinbase.com`) for cryptocurrencies.

Each request contains only the symbol being looked up (for example `AAPL` or `BTC-USD`). No account, name, email, location, device identifier or list of your other symbols is sent. Like any website, these services can see your IP address and the time of the request; their own privacy policies cover how they handle that:

- Yahoo: <https://legal.yahoo.com/us/en/yahoo/privacy/index.html>
- Coinbase: <https://www.coinbase.com/legal/privacy>

WidgetsPlus has no accounts, no sign-in, no advertising, no analytics, no crash reporting and no third-party tracking.

## Opening Yahoo Finance or Coinbase

Tapping a symbol on a widget opens its page on Yahoo Finance or Coinbase, in their app if you have it installed or in your browser. From then on you are using their service under their terms.

## Permissions

| Permission | Why | Required? |
|---|---|---|
| Internet | To fetch prices from Yahoo Finance and Coinbase. | Yes |
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
