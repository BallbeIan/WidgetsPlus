package app.widgetsplus.finance

import org.json.JSONArray
import org.json.JSONObject

enum class AssetType { STOCK, CRYPTO }

/** A stock or coin. [code] is a Yahoo ticker (AAPL, ^GSPC) or a Coinbase pair (BTC-USD). */
data class WatchItem(val type: AssetType, val code: String, val name: String) {
    /** Unique key across both sources, used for quotes and widget settings. */
    val key: String get() = "${type.name}:$code"

    /** Short label for widgets: "BTC" for BTC-USD, the ticker as is for stocks. */
    val label: String
        get() = if (type == AssetType.CRYPTO && code.endsWith("-USD")) code.removeSuffix("-USD") else code

    fun toJson(): JSONObject = JSONObject().put("type", type.name).put("code", code).put("name", name)

    companion object {
        fun fromJson(o: JSONObject) = WatchItem(AssetType.valueOf(o.getString("type")), o.getString("code"), o.optString("name"))
    }
}

/** A named list of symbols. Each list widget shows one list, so stocks and crypto can be on separate widgets. */
data class PriceList(val id: String, val name: String, val items: List<WatchItem>) {

    /** The type most items in the list have, used as the default when adding to it. */
    val mainType: AssetType
        get() = items.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key ?: AssetType.STOCK

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("items", JSONArray(items.map { it.toJson() }))

    companion object {
        fun fromJson(o: JSONObject): PriceList {
            val a = o.getJSONArray("items")
            return PriceList(o.getString("id"), o.getString("name"), List(a.length()) { WatchItem.fromJson(a.getJSONObject(it)) })
        }
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
    /** Today's (stocks) or the last 24 hours' (crypto) high and low, and volume. Null if the source didn't say. */
    val high: Double? = null,
    val low: Double? = null,
    val volume: Double? = null,
) {
    val change: Double get() = price - reference
    val changePercent: Double get() = if (reference == 0.0) 0.0 else (price - reference) / reference * 100

    fun toJson(): JSONObject = JSONObject()
        .put("price", price)
        .put("reference", reference)
        .put("currency", currency ?: JSONObject.NULL)
        .put("chart", JSONArray(chart))
        .put("fetchedAt", fetchedAt)
        .put("high", high ?: JSONObject.NULL)
        .put("low", low ?: JSONObject.NULL)
        .put("volume", volume ?: JSONObject.NULL)

    companion object {
        private fun JSONObject.doubleOrNull(key: String) = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

        fun fromJson(o: JSONObject): Quote {
            val chart = o.getJSONArray("chart")
            return Quote(
                price = o.getDouble("price"),
                reference = o.getDouble("reference"),
                currency = if (o.isNull("currency")) null else o.getString("currency"),
                chart = List(chart.length()) { chart.getDouble(it) },
                fetchedAt = o.getLong("fetchedAt"),
                high = o.doubleOrNull("high"),
                low = o.doubleOrNull("low"),
                volume = o.doubleOrNull("volume"),
            )
        }
    }
}

/** Everything the finance screen and widgets show. */
data class FinanceState(
    val lists: List<PriceList>,
    val quotes: Map<String, Quote>,
    val lastRefresh: Long,
    val lastRefreshFailed: Boolean,
    val refreshing: Boolean,
    /** Goes up whenever a widget's choice changes, so widgets showing this state redraw. */
    val widgetSettingsVersion: Int = 0,
) {
    /** Every symbol across all lists, once each, in list order. */
    val allItems: List<WatchItem> get() = lists.flatMap { it.items }.distinctBy { it.key }
}
