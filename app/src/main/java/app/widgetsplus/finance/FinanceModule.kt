package app.widgetsplus.finance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.Hue
import app.widgetsplus.core.WidgetKind
import app.widgetsplus.core.WidgetModule
import app.widgetsplus.finance.ui.FinanceConfigureActivity
import app.widgetsplus.finance.ui.FinanceScreen
import app.widgetsplus.finance.widget.PriceListWidget
import app.widgetsplus.finance.widget.PriceListWidgetReceiver
import app.widgetsplus.finance.widget.SymbolWidget
import app.widgetsplus.finance.widget.SymbolWidgetReceiver

/** Stock prices from Yahoo Finance and crypto prices from Coinbase. */
object FinanceModule : WidgetModule {
    override val id = "finance"
    override val title = R.string.finance_title
    override val description = R.string.finance_description
    override val icon = R.drawable.ic_module_finance
    override val hue = Hue.GREEN

    val symbolWidget = WidgetKind(SymbolWidgetReceiver::class.java, R.string.widget_symbol_name, R.string.widget_symbol_description, R.drawable.ic_widget_symbol, FinanceConfigureActivity::class.java)
    val listWidget = WidgetKind(PriceListWidgetReceiver::class.java, R.string.widget_list_name, R.string.widget_list_description, R.drawable.ic_widget_list, FinanceConfigureActivity::class.java)
    override val widgets = listOf(symbolWidget, listWidget)

    @Composable
    override fun summary(): String {
        val s by FinanceRepository.get(LocalContext.current).state.collectAsStateWithLifecycle()
        val lists = pluralStringResource(R.plurals.lists, s.lists.size, s.lists.size)
        val symbols = pluralStringResource(R.plurals.symbols, s.allItems.size, s.allItems.size)
        return "$lists, $symbols"
    }

    @Composable
    override fun Screen() = FinanceScreen()

    override suspend fun refresh(context: Context) = FinanceRepository.get(context).refresh()

    override suspend fun updateWidgets(context: Context) {
        SymbolWidget().updateAll(context)
        PriceListWidget().updateAll(context)
    }

    override fun describeWidget(context: Context, appWidgetId: Int, kind: WidgetKind): String {
        val repo = FinanceRepository.get(context)
        return when (kind) {
            symbolWidget -> repo.itemForWidget(appWidgetId)?.let { "${it.label} · ${it.name}" }
            else -> repo.listForWidget(appWidgetId)?.name
        } ?: context.getString(R.string.nothing_chosen)
    }

    override fun forgetWidget(context: Context, appWidgetId: Int) = FinanceRepository.get(context).forgetWidget(appWidgetId)
}
