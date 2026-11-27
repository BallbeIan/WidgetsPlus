# Glance tells widget types apart by the class of their GlanceAppWidget. R8 merged SymbolWidget and PriceListWidget
# into one class in 0.2-beta, after which every redraw drew single-price widgets into list widgets as well.
# Keep every widget class as its own, unrenamed class (this covers future modules too).
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
