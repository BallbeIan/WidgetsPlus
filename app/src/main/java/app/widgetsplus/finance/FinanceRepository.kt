package app.widgetsplus.finance

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject

/**
 * The finance module's lists, last fetched prices and per-widget settings, kept in their own SharedPreferences file.
 * Shared by the app screens, the widgets and the background refresh, which all run in the same process.
 */
class FinanceRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("finance", Context.MODE_PRIVATE)
    private val refreshLock = Mutex()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<FinanceState> = _state.asStateFlow()

    private fun load(): FinanceState {
        val lists = prefs.getString(KEY_LISTS, null)?.let { json ->
            JSONArray(json).let { a -> List(a.length()) { PriceList.fromJson(a.getJSONObject(it)) } }
        } ?: migrateFromBeta() ?: defaultLists()
        val quotes = prefs.getString(KEY_QUOTES, null)?.let { json ->
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { Quote.fromJson(o.getJSONObject(it)) }
        } ?: emptyMap()
        return FinanceState(
            lists = lists,
            quotes = quotes,
            lastRefresh = prefs.getLong(KEY_LAST_REFRESH, 0),
            lastRefreshFailed = prefs.getBoolean(KEY_LAST_FAILED, false),
            refreshing = false,
        ).also { save(it) }
    }

    /** Version 0.1-beta kept one flat list in another file. Split it into Stocks and Crypto. */
    private fun migrateFromBeta(): List<PriceList>? {
        val old = context.getSharedPreferences("widgetsplus", Context.MODE_PRIVATE)
        val json = old.getString("items", null) ?: return null
        val items = JSONArray(json).let { a -> List(a.length()) { WatchItem.fromJson(a.getJSONObject(it)) } }
        old.getString("quotes", null)?.let { prefs.edit().putString(KEY_QUOTES, it).apply() }
        old.edit().clear().apply()
        return listOf(
            PriceList(newId(), "Stocks", items.filter { it.type == AssetType.STOCK }),
            PriceList(newId(), "Crypto", items.filter { it.type == AssetType.CRYPTO }),
        )
    }

    /** A starting point so the widgets show something on first use. The user can change or delete these. */
    private fun defaultLists() = listOf(
        PriceList(newId(), "Stocks", listOf(
            WatchItem(AssetType.STOCK, "^GSPC", "S&P 500"),
            WatchItem(AssetType.STOCK, "AAPL", "Apple Inc."),
        )),
        PriceList(newId(), "Crypto", listOf(
            WatchItem(AssetType.CRYPTO, "BTC-USD", "Bitcoin"),
            WatchItem(AssetType.CRYPTO, "ETH-USD", "Ethereum"),
        )),
    )

    private fun save(s: FinanceState) {
        // Prices of symbols no longer in any list are dropped.
        val keys = s.allItems.map { it.key }.toSet()
        prefs.edit()
            .putString(KEY_LISTS, JSONArray(s.lists.map { it.toJson() }).toString())
            .putString(KEY_QUOTES, JSONObject().apply { s.quotes.filterKeys { it in keys }.forEach { (k, q) -> put(k, q.toJson()) } }.toString())
            .putLong(KEY_LAST_REFRESH, s.lastRefresh)
            .putBoolean(KEY_LAST_FAILED, s.lastRefreshFailed)
            .apply()
    }

    private suspend fun change(block: (FinanceState) -> FinanceState) {
        _state.update(block)
        save(_state.value)
        FinanceModule.updateWidgets(context)
    }

    private suspend fun changeList(listId: String, block: (PriceList) -> PriceList) = change { s ->
        s.copy(lists = s.lists.map { if (it.id == listId) block(it) else it })
    }

    // Lists

    /** Creates a list and returns its id. */
    suspend fun addList(name: String): String {
        val id = newId()
        change { s -> s.copy(lists = s.lists + PriceList(id, name.trim(), emptyList())) }
        return id
    }

    suspend fun renameList(listId: String, name: String) = changeList(listId) { it.copy(name = name.trim()) }

    suspend fun deleteList(listId: String) = change { s -> s.copy(lists = s.lists.filterNot { it.id == listId }) }

    suspend fun moveList(listId: String, by: Int) = change { s -> s.copy(lists = s.lists.moved({ it.id == listId }, by)) }

    // Symbols within a list

    /** Adds an item that came from [PriceSources.lookUp], with its first quote if one was fetched. */
    suspend fun addItem(listId: String, item: WatchItem, quote: Quote?) = change { s ->
        s.copy(
            lists = s.lists.map { l -> if (l.id == listId && l.items.none { it.key == item.key }) l.copy(items = l.items + item) else l },
            quotes = if (quote != null) s.quotes + (item.key to quote) else s.quotes,
        )
    }

    suspend fun removeItem(listId: String, key: String) = changeList(listId) { l -> l.copy(items = l.items.filterNot { it.key == key }) }

    suspend fun moveItem(listId: String, key: String, by: Int) = changeList(listId) { l -> l.copy(items = l.items.moved({ it.key == key }, by)) }

    /**
     * Fetches every price again. A symbol that fails keeps its previous price.
     * Returns false if nothing could be fetched (usually no network). Does nothing if a refresh is already running.
     */
    suspend fun refresh(): Boolean {
        if (!refreshLock.tryLock()) return true
        try {
            _state.update { it.copy(refreshing = true) }
            FinanceModule.updateWidgets(context)
            val items = _state.value.allItems
            val fetched = coroutineScope {
                // A few at a time: Coinbase allows about 10 requests a second, and each coin needs 2.
                val limit = Semaphore(4)
                items.map { item ->
                    async { item.key to limit.withPermit { runCatching { PriceSources.quote(item) }.getOrNull() } }
                }.awaitAll()
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

    // Widget settings

    /** The symbol a symbol widget shows: the one picked for it, else the first symbol of the first list. */
    fun itemForWidget(appWidgetId: Int, s: FinanceState = _state.value): WatchItem? {
        val key = prefs.getString("$KEY_SYMBOL_WIDGET$appWidgetId", null)
        return s.allItems.firstOrNull { it.key == key } ?: s.allItems.firstOrNull()
    }

    fun setItemForWidget(appWidgetId: Int, key: String) {
        prefs.edit().putString("$KEY_SYMBOL_WIDGET$appWidgetId", key).apply()
        widgetSettingsChanged()
    }

    /** The list a list widget shows: the one picked for it, else the first list. */
    fun listForWidget(appWidgetId: Int, s: FinanceState = _state.value): PriceList? {
        val id = prefs.getString("$KEY_LIST_WIDGET$appWidgetId", null)
        return s.lists.firstOrNull { it.id == id } ?: s.lists.firstOrNull()
    }

    fun setListForWidget(appWidgetId: Int, listId: String) {
        prefs.edit().putString("$KEY_LIST_WIDGET$appWidgetId", listId).apply()
        widgetSettingsChanged()
    }

    /** The choice is read during composition, so a running widget only redraws when the state changes. */
    private fun widgetSettingsChanged() = _state.update { it.copy(widgetSettingsVersion = it.widgetSettingsVersion + 1) }

    fun forgetWidget(appWidgetId: Int) =
        prefs.edit().remove("$KEY_SYMBOL_WIDGET$appWidgetId").remove("$KEY_LIST_WIDGET$appWidgetId").apply()

    companion object {
        private const val KEY_LISTS = "lists"
        private const val KEY_QUOTES = "quotes"
        private const val KEY_LAST_REFRESH = "lastRefresh"
        private const val KEY_LAST_FAILED = "lastRefreshFailed"
        private const val KEY_SYMBOL_WIDGET = "symbolWidget."
        private const val KEY_LIST_WIDGET = "listWidget."

        private fun newId() = UUID.randomUUID().toString().take(8)

        private fun <T> List<T>.moved(match: (T) -> Boolean, by: Int): List<T> {
            val from = indexOfFirst(match)
            val to = from + by
            if (from < 0 || to !in indices) return this
            return toMutableList().apply { add(to, removeAt(from)) }
        }

        @Volatile private var instance: FinanceRepository? = null

        fun get(context: Context): FinanceRepository =
            instance ?: synchronized(this) {
                instance ?: FinanceRepository(context.applicationContext).also { instance = it }
            }
    }
}
