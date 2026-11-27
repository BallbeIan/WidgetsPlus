package app.widgetsplus.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Thrown when a source says the symbol does not exist, as opposed to a network problem. */
class UnknownSymbolException(code: String) : Exception("Unknown symbol $code")

/**
 * Public price endpoints that need no account or key. Only the symbol is sent; nothing about the user.
 *
 * Stocks use Yahoo Finance's chart endpoint. It is unofficial: if Yahoo changes it, only [Yahoo] needs fixing.
 * Crypto uses Coinbase's public Exchange API.
 */
object PriceSources {

    suspend fun quote(item: WatchItem): Quote = when (item.type) {
        AssetType.STOCK -> Yahoo.quote(item.code)
        AssetType.CRYPTO -> Coinbase.quote(item.code)
    }

    /** Checks a symbol the user typed and returns it with its proper code and name. */
    suspend fun lookUp(type: AssetType, input: String): WatchItem = when (type) {
        AssetType.STOCK -> Yahoo.lookUp(input)
        AssetType.CRYPTO -> Coinbase.lookUp(input)
    }

    private object Yahoo {
        private fun chartUrl(code: String) =
            "https://query1.finance.yahoo.com/v8/finance/chart/${enc(code)}?range=1d&interval=5m"

        suspend fun quote(code: String): Quote {
            val result = chart(code)
            val meta = result.getJSONObject("meta")
            val price = meta.getDouble("regularMarketPrice")
            val reference = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", price))
            val closes = result.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
            val isIndex = meta.optString("instrumentType") == "INDEX"
            return Quote(
                price = price,
                reference = reference,
                currency = if (isIndex) null else meta.optString("currency").ifEmpty { null },
                chart = closes.doubles(),
                fetchedAt = System.currentTimeMillis(),
            )
        }

        suspend fun lookUp(input: String): WatchItem {
            val code = input.trim().uppercase()
            if (code.isEmpty()) throw UnknownSymbolException(input)
            val meta = chart(code).getJSONObject("meta")
            val name = meta.optString("shortName").ifEmpty { meta.optString("longName") }.ifEmpty { code }
            return WatchItem(AssetType.STOCK, meta.optString("symbol", code), name)
        }

        private suspend fun chart(code: String): JSONObject {
            val body = get(chartUrl(code), code)
            val chart = JSONObject(body).getJSONObject("chart")
            val results = chart.optJSONArray("result")
            if (results == null || results.length() == 0) throw UnknownSymbolException(code)
            return results.getJSONObject(0)
        }
    }

    private object Coinbase {
        private const val BASE = "https://api.exchange.coinbase.com/products"

        suspend fun quote(pair: String): Quote {
            val stats = JSONObject(get("$BASE/${enc(pair)}/stats", pair))
            val product = JSONObject(get("$BASE/${enc(pair)}", pair))
            val price = stats.getString("last").toDouble()
            // 15-minute candles, newest first: [time, low, high, open, close, volume]. Keep the last 24 hours.
            val since = System.currentTimeMillis() / 1000 - 24 * 3600
            val candles = JSONArray(get("$BASE/${enc(pair)}/candles?granularity=900", pair))
            val chart = (0 until candles.length())
                .map { candles.getJSONArray(it) }
                .filter { it.getLong(0) >= since }
                .map { it.getDouble(4) }
                .reversed()
            return Quote(
                price = price,
                reference = stats.getString("open").toDouble(),
                currency = product.optString("quote_currency").ifEmpty { null },
                chart = chart,
                fetchedAt = System.currentTimeMillis(),
            )
        }

        /** "btc" becomes BTC-USD; a full pair like "eth-eur" is kept. */
        suspend fun lookUp(input: String): WatchItem {
            val raw = input.trim().uppercase().replace('/', '-')
            if (raw.isEmpty()) throw UnknownSymbolException(input)
            val pair = if ('-' in raw) raw else "$raw-USD"
            val product = JSONObject(get("$BASE/${enc(pair)}", pair))
            val base = product.getString("base_currency")
            return WatchItem(AssetType.CRYPTO, product.getString("id"), coinName(base) ?: base)
        }

        private suspend fun coinName(code: String): String? = runCatching {
            val list = JSONObject(get("https://api.coinbase.com/v2/currencies/crypto", code)).getJSONArray("data")
            (0 until list.length()).map { list.getJSONObject(it) }.firstOrNull { it.optString("code") == code }?.optString("name")
        }.getOrNull()
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun JSONArray?.doubles(): List<Double> =
        if (this == null) emptyList() else (0 until length()).filter { !isNull(it) }.map { getDouble(it) }

    private suspend fun get(url: String, code: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            // Both services reject requests without a browser-like user agent.
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) WidgetsPlus")
            conn.setRequestProperty("Accept", "application/json")
            when (val status = conn.responseCode) {
                in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }
                404 -> throw UnknownSymbolException(code)
                else -> throw IOException("HTTP $status from ${URL(url).host}")
            }
        } finally {
            conn.disconnect()
        }
    }
}
