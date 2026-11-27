package app.widgetsplus.data

import org.json.JSONArray
import org.json.JSONObject

enum class AssetType { STOCK, CRYPTO }

/** One entry of the user's watchlist. [code] is a Yahoo ticker (AAPL, ^GSPC) or a Coinbase pair (BTC-USD). */
data class WatchItem(val type: AssetType, val code: String, val name: String) {
    /** Unique key across both sources, used for storage and for widget settings. */
    val key: String get() = "${type.name}:$code"

    /** Short label for widgets: "BTC" for BTC-USD, the ticker as is for stocks. */
    val label: String
        get() = if (type == AssetType.CRYPTO && code.endsWith("-USD")) code.removeSuffix("-USD") else code

    fun toJson(): JSONObject = JSONObject().put("type", type.name).put("code", code).put("name", name)

    companion object {
        fun fromJson(o: JSONObject) = WatchItem(AssetType.valueOf(o.getString("type")), o.getString("code"), o.optString("name"))
    }
}

/**
 * The latest known price of a [WatchItem].
 * [reference] is what the change is measured against: the previous close for stocks, the price 24 hours ago for crypto.
 * [currency] is null for things that are not priced in a currency, like stock indices.
 */
data class Quote(
    val price: Double,
    val reference: Double,
    val currency: String?,
    val chart: List<Double>,
    val fetchedAt: Long,
) {
    val change: Double get() = price - reference
    val changePercent: Double get() = if (reference == 0.0) 0.0 else (price - reference) / reference * 100

    fun toJson(): JSONObject = JSONObject()
        .put("price", price)
        .put("reference", reference)
        .put("currency", currency ?: JSONObject.NULL)
        .put("chart", JSONArray(chart))
        .put("fetchedAt", fetchedAt)

    companion object {
        fun fromJson(o: JSONObject): Quote {
            val chart = o.getJSONArray("chart")
            return Quote(
                price = o.getDouble("price"),
                reference = o.getDouble("reference"),
                currency = if (o.isNull("currency")) null else o.getString("currency"),
                chart = List(chart.length()) { chart.getDouble(it) },
                fetchedAt = o.getLong("fetchedAt"),
            )
        }
    }
}

/** Everything the app screen and the widgets show. */
data class Snapshot(
    val items: List<WatchItem>,
    val quotes: Map<String, Quote>,
    val lastRefresh: Long,
    val lastRefreshFailed: Boolean,
    val refreshing: Boolean,
)
