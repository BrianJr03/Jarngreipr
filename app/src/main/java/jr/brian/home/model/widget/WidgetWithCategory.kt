package jr.brian.home.model.widget

data class WidgetWithCategory(
    val widget: WidgetProviderInfo,
    val categoryName: String,
    val categoryIcon: android.graphics.drawable.Drawable?
) {
    val category = widget
}