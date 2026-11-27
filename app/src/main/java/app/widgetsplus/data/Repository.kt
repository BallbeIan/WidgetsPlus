package app.widgetsplus.data

import android.content.Context
import android.content.SharedPreferences
import app.widgetsplus.widget.updateAllWidgets
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject

/**
 * The watchlist, the last fetched prices and the per-widget settings, kept in one SharedPreferences file.
 * Shared by the app screen, the widgets and the background refresh, which all run in the same process.
 */
class Repository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("widgetsplus", Context.MODE_PRIVATE)
    private val refreshLock = Mutex()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    private fun load(): Snapshot {
        val items = prefs.getString(KEY_ITEMS, null)?.let { json ->
            JSONArray(json).let { a -> List(a.length()) { WatchItem.fromJson(a.getJSONObject(it)) } }
        } ?: DEFAULT_ITEMS
        val quotes = prefs.getString(KEY_QUOTES, null)?.let { json ->
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { Quote.fromJson(o.getJSONObject(it)) }
        } ?: emptyMap()
        return Snapshot(
            items = items,
            quotes = quotes,
            lastRefresh = prefs.getLong(KEY_LAST_REFRESH, 0),
            lastRefreshFailed = prefs.getBoolean(KEY_LAST_FAILED, false),
            refreshing = false,
        )
    }

    private fun save(s: Snapshot) {
        prefs.edit()
            .putString(KEY_ITEMS, JSONArray(s.items.map { it.toJson() }).toString())
            .putString(KEY_QUOTES, JSONObject().apply { s.quotes.forEach { (k, q) -> put(k, q.toJson()) } }.toString())
            .putLong(KEY_LAST_REFRESH, s.lastRefresh)
            .putBoolean(KEY_LAST_FAILED, s.lastRefreshFailed)
            .apply()
    }

    private suspend fun change(block: (Snapshot) -> Snapshot) {
        _state.update(block)
        save(_state.value)
        updateAllWidgets(context)
    }

    /** Adds an item that came from [PriceSources.lookUp], with its first quote if one was fetched. */
    suspend fun add(item: WatchItem, quote: Quote?) = change { s ->
        if (s.items.any { it.key == item.key }) s
        else s.copy(items = s.items + item, quotes = if (quote != null) s.quotes + (item.key to quote) else s.quotes)
    }

    suspend fun remove(key: String) = change { s ->
        s.copy(items = s.items.filterNot { it.key == key }, quotes = s.quotes - key)
    }

    suspend fun move(key: String, by: Int) = change { s ->
        val from = s.items.indexOfFirst { it.key == key }
        val to = from + by
        if (from < 0 || to !in s.items.indices) s
        else s.copy(items = s.items.toMutableList().apply { add(to, removeAt(from)) })
    }

    /**
     * Fetches every price again. A symbol that fails keeps its previous price.
     * Returns false if nothing could be fetched (usually no network). Does nothing if a refresh is already running.
     */
    suspend fun refresh(): Boolean {
        if (!refreshLock.tryLock()) return true
        try {
            _state.update { it.copy(refreshing = true) }
            updateAllWidgets(context)
            val items = _state.value.items
            val fetched = coroutineScope {
                items.map { item -> async { item.key to runCatching { PriceSources.quote(item) }.getOrNull() } }.awaitAll()
            }.mapNotNull { (k, q) -> q?.let { k to it } }.toMap()
            val failed = items.isNotEmpty() && fetched.isEmpty()
            change { s ->
                s.copy(
                    quotes = s.quotes + fetched,
                    lastRefresh = if (failed) s.lastRefresh else System.currentTimeMillis(),
                    lastRefreshFailed = failed,
                    refreshing = false,
                )
            }
            return !failed
        } finally {
            if (_state.value.refreshing) _state.update { it.copy(refreshing = false) }
            refreshLock.unlock()
        }
    }

    /** The item a single-symbol widget shows: the one picked for it, else the first in the list. */
    fun itemForWidget(appWidgetId: Int, s: Snapshot = _state.value): WatchItem? {
        val key = prefs.getString("$KEY_WIDGET$appWidgetId", null)
        return s.items.firstOrNull { it.key == key } ?: s.items.firstOrNull()
    }

    fun setItemForWidget(appWidgetId: Int, key: String) {
        prefs.edit().putString("$KEY_WIDGET$appWidgetId", key).apply()
    }

    fun forgetWidget(appWidgetId: Int) {
        prefs.edit().remove("$KEY_WIDGET$appWidgetId").apply()
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val KEY_QUOTES = "quotes"
        private const val KEY_LAST_REFRESH = "lastRefresh"
        private const val KEY_LAST_FAILED = "lastRefreshFailed"
        private const val KEY_WIDGET = "widget."

        /** A starting list so the widgets show something on first use. The user can remove these. */
        private val DEFAULT_ITEMS = listOf(
            WatchItem(AssetType.STOCK, "^GSPC", "S&P 500"),
            WatchItem(AssetType.STOCK, "AAPL", "Apple Inc."),
            WatchItem(AssetType.CRYPTO, "BTC-USD", "Bitcoin"),
            WatchItem(AssetType.CRYPTO, "ETH-USD", "Ethereum"),
        )

        @Volatile private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }
}
