package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R
import kotlin.math.roundToInt

private const val PERCENT = 100

/** A setting A steps through: [options] in order, showing the current one's label. */
@Composable
internal fun <T> InspectChoiceRow(
    @StringRes labelRes: Int,
    current: T,
    options: List<T>,
    optionLabel: (T) -> Int,
    supporting: String? = null,
    onChoose: (T) -> Unit,
) {
    JarnValueRow(
        label = stringResource(labelRes),
        value = stringResource(optionLabel(current)),
        onClick = { onChoose(options.after(current)) },
        modifier = Modifier.fillMaxWidth(),
        supporting = supporting,
    )
}

/** A setting A moves on to its next value, showing [valueRes]. */
@Composable
internal fun InspectStepRow(@StringRes labelRes: Int, @StringRes valueRes: Int, onClick: () -> Unit) {
    JarnValueRow(
        label = stringResource(labelRes),
        value = stringResource(valueRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A share of the screen, from 0 to 100%, in 1% steps with left and right. */
@Composable
internal fun InspectFractionRow(
    @StringRes labelRes: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onChange: (Float) -> Unit,
) {
    JarnSliderRow(
        label = stringResource(labelRes),
        value = value.coerceIn(range),
        valueText = stringResource(R.string.jarnfe_settings_percent, (value * PERCENT).roundToInt()),
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        valueRange = range,
        steps = ((range.endInclusive - range.start) * PERCENT).roundToInt(),
    )
}

/** A whole number in [range], one step per press. */
@Composable
internal fun InspectCountRow(@StringRes labelRes: Int, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    JarnSliderRow(
        label = stringResource(labelRes),
        value = value.coerceIn(range).toFloat(),
        valueText = value.toString(),
        onValueChange = { onChange(it.roundToInt()) },
        modifier = Modifier.fillMaxWidth(),
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = range.last - range.first,
    )
}

@Composable
internal fun InspectToggleRow(
    @StringRes labelRes: Int,
    checked: Boolean,
    supporting: String? = null,
    label: String = stringResource(labelRes),
    onChange: (Boolean) -> Unit,
) {
    JarnToggleRow(
        label = label,
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = supporting,
    )
}

@Composable
internal fun InspectActionRow(label: String, supporting: String? = null, onClick: () -> Unit) {
    JarnNavigateRow(label = label, onClick = onClick, modifier = Modifier.fillMaxWidth(), supporting = supporting)
}
