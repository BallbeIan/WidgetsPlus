package app.widgetsplus.finance

import android.net.Uri
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.abs

object FinanceFormat {

    /** "$1,234.56", "€0.4312", or "5,912.30" for an index. Small prices get more decimals. */
    fun price(value: Double, currency: String?): String {
        val decimals = when {
            abs(value) >= 1.0 || value == 0.0 -> 2
            abs(value) >= 0.01 -> 4
            else -> 8
        }
        // London stocks are quoted in pence ("GBp"), which is not an ISO currency.
        if (currency == "GBp" || currency == "GBX") return number(value, decimals) + "p"
        val iso = currency?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
            ?: return number(value, decimals) + (currency?.let { " $it" } ?: "")
        return NumberFormat.getCurrencyInstance().apply {
            this.currency = iso
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)
    }

    /** "+1.23%" or "−0.45%". */
    fun percent(value: Double): String {
        val sign = if (value >= 0) "+" else "−"
        return sign + number(abs(value), 2) + "%"
    }

    /** A plain number with the same decimals [price] would use: "1,234.56", "0.4312". */
    fun plain(value: Double): String = number(value, decimalsFor(value))

    /** "+1,116.04" or "−0.42", for the change in money. */
    fun signed(value: Double): String = (if (value >= 0) "+" else "\u2212") + number(abs(value), decimalsFor(value))

    /** Short volume: "546.9", "12.4K", "53.1M", "2.3B". */
    fun compact(value: Double): String {
        val a = abs(value)
        return when {
            a >= 1e9 -> number(value / 1e9, 1) + "B"
            a >= 1e6 -> number(value / 1e6, 1) + "M"
            a >= 1e4 -> number(value / 1e3, 1) + "K"
            else -> number(value, 1)
        }
    }

    /** "345.44 – 352.60". */
    fun range(low: Double, high: Double): String = plain(low) + " \u2013 " + plain(high)

    private fun decimalsFor(value: Double) = when {
        abs(value) >= 1.0 || value == 0.0 -> 2
        abs(value) >= 0.01 -> 4
        else -> 8
    }

    private fun number(value: Double, decimals: Int): String =
        NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)


    /** The web page for a symbol. Opens in the Yahoo Finance or Coinbase app if installed, otherwise the browser. */
    fun pageFor(item: WatchItem): Uri = when (item.type) {
        AssetType.STOCK -> Uri.parse("https://finance.yahoo.com/quote/" + Uri.encode(item.code))
        AssetType.CRYPTO -> Uri.parse("https://www.coinbase.com/advanced-trade/spot/" + Uri.encode(item.code))
    }
}
