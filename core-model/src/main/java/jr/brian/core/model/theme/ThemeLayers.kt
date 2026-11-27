package jr.brian.core.model.theme

import jr.brian.core.model.MediaKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * A decor layer. Every layer has a stable [id] (unique in its view), a [rect] in screen
 * fractions, a draw order [z] (higher on top; the primary sits at 0), [opacity] and an
 * optional [visibleWhen].
 */
@Serializable
sealed interface ThemeLayer {
    val id: String
    val rect: ThemeRect
    val z: Int
    val opacity: Float
    val visibleWhen: ThemeVisibleWhen?

    @Serializable
    @SerialName("picture")
    data class Picture(
        override val id: String,
        override val rect: ThemeRect,
        override val z: Int = 0,
        override val opacity: Float = 1f,
        override val visibleWhen: ThemeVisibleWhen? = null,
        val source: ThemePicture,
        val fit: ThemeFit = ThemeFit.Fit,
        val corner: ThemeCorner = ThemeCorner.None,
    ) : ThemeLayer

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String,
        override val rect: ThemeRect,
        override val z: Int = 0,
        override val opacity: Float = 1f,
        override val visibleWhen: ThemeVisibleWhen? = null,
        val content: ThemeText,
        val style: ThemeTextRole = ThemeTextRole.Body,
        val color: ThemeColorRole = ThemeColorRole.TextHigh,
        val align: ThemeAlign = ThemeAlign.Start,
        val maxLines: Int = 1,
    ) : ThemeLayer

    /** The focused game's rating as stars. */
    @Serializable
    @SerialName("rating")
    data class Rating(
        override val id: String,
        override val rect: ThemeRect,
        override val z: Int = 0,
        override val opacity: Float = 1f,
        override val visibleWhen: ThemeVisibleWhen? = null,
        val color: ThemeColorRole = ThemeColorRole.Accent,
    ) : ThemeLayer

    @Serializable
    @SerialName("date")
    data class Date(
        override val id: String,
        override val rect: ThemeRect,
        override val z: Int = 0,
        override val opacity: Float = 1f,
        override val visibleWhen: ThemeVisibleWhen? = null,
        val field: ThemeDateField,
        val format: ThemeDateFormat = ThemeDateFormat.Year,
        val style: ThemeTextRole = ThemeTextRole.Label,
        val color: ThemeColorRole = ThemeColorRole.TextMid,
        val align: ThemeAlign = ThemeAlign.Start,
    ) : ThemeLayer

    /** A plain filled plate, for backing text or framing art. */
    @Serializable
    @SerialName("panel")
    data class Panel(
        override val id: String,
        override val rect: ThemeRect,
        override val z: Int = 0,
        override val opacity: Float = 1f,
        override val visibleWhen: ThemeVisibleWhen? = null,
        val color: ThemeColorRole = ThemeColorRole.Surface,
        val corner: ThemeCorner = ThemeCorner.Md,
        val outlined: Boolean = false,
    ) : ThemeLayer

    /** The focused game's video, with [fallback] shown before it starts or when there is none. */
    @Serializable
    @SerialName("video")
    data class Video(
        override val id: String,
        override val rect: ThemeRect,
        override val z: Int = 0,
        override val opacity: Float = 1f,
        override val visibleWhen: ThemeVisibleWhen? = null,
        val delayMs: Int = 0,
        val audio: Boolean = false,
        val fallback: ThemePicture? = ThemePicture.GameMedia(MediaKind.Screenshot),
        val corner: ThemeCorner = ThemeCorner.None,
    ) : ThemeLayer
}

/** Drops layers of a type this build doesn't know, so a newer theme's extra layers don't refuse the theme. */
internal object ThemeLayerListSerializer :
    JsonTransformingSerializer<List<ThemeLayer>>(ListSerializer(ThemeLayer.serializer())) {

    private val knownTypes: Set<String> by lazy {
        ThemeLayer.serializer().descriptor.getElementDescriptor(1).elementNames.toSet()
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) return element
        return JsonArray(element.filter { layer -> layer.layerType() in knownTypes })
    }

    private fun JsonElement.layerType(): String? =
        ((this as? JsonObject)?.get("type") as? JsonPrimitive)?.takeIf { it.isString }?.content
}
