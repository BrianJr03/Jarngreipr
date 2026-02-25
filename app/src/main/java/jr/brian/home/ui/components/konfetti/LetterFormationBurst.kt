package jr.brian.home.ui.components.konfetti

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.get
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Two-phase animation composable:
 * Phase 1 – Particles form the shape of a letter/number on screen.
 * Phase 2 – Once formed, the particles burst outward using Konfetti.
 *
 * @param char The character to form (letter or digit).
 * @param colors Konfetti particle colors (ARGB ints).
 * @param burstPreset Which preset to use for the burst phase (default EXPLODE).
 * @param formationDurationMs How long the formation animation takes.
 * @param holdDurationMs How long to hold the formed shape before bursting.
 * @param particleCount Number of dots that form the letter.
 * @param onComplete Called when the entire animation (formation + burst) finishes.
 */
@Composable
fun LetterFormationBurst(
    char: Char,
    colors: List<Int>,
    burstPreset: KonfettiPreset = KonfettiPreset.EXPLODE,
    formationDurationMs: Int = 1200,
    holdDurationMs: Long = 400L,
    particleCount: Int = 100,
    onComplete: () -> Unit = {}
) {
    var phase by remember { mutableStateOf(AnimationPhase.FORMING) }
    val progress = remember { Animatable(0f) }

    // Sample target points once
    val targetPoints = remember(char, particleCount) {
        sampleLetterPoints(char, particleCount)
    }

    // Random start positions (one per target)
    val startPoints = remember(targetPoints) {
        targetPoints.map {
            Offset(
                x = Random.nextFloat(),
                y = Random.nextFloat()
            )
        }
    }

    // Random color per particle from the provided colors
    val particleColors = remember(targetPoints, colors) {
        targetPoints.map { colors[Random.nextInt(colors.size)] }
    }

    // Phase 1: Formation animation
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = formationDurationMs,
                easing = FastOutSlowInEasing
            )
        )
        delay(holdDurationMs)
        phase = AnimationPhase.BURSTING
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            AnimationPhase.FORMING -> {
                // Draw particles converging into the letter shape
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val p = progress.value
                    drawFormingParticles(
                        startPoints = startPoints,
                        targetPoints = targetPoints,
                        particleColors = particleColors,
                        progress = p
                    )
                }
            }

            AnimationPhase.BURSTING -> {
                // Fire Konfetti from clustered positions matching the letter shape
                val burstParties = remember(targetPoints, colors) {
                    buildBurstParties(targetPoints, colors, burstPreset)
                }

                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = burstParties,
                    updateListener = object : OnParticleSystemUpdateListener {
                        override fun onParticleSystemEnded(
                            system: PartySystem,
                            activeSystems: Int
                        ) {
                            if (activeSystems == 0) {
                                phase = AnimationPhase.DONE
                                onComplete()
                            }
                        }
                    }
                )
            }

            AnimationPhase.DONE -> {
                // Animation complete – nothing to render
            }
        }
    }
}

private enum class AnimationPhase {
    FORMING, BURSTING, DONE
}

/**
 * Rasterize a character into a bitmap and samples non-transparent pixel positions
 * as normalized (0..1) coordinates.
 */
private fun sampleLetterPoints(char: Char, count: Int): List<Offset> {
    // Only uppercase letters and digits
    val displayChar = if (char.isLetterOrDigit()) char.uppercaseChar() else 'A'

    val bitmapSize = 256
    val bitmap = createBitmap(bitmapSize, bitmapSize)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = bitmapSize * 0.75f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Center the text
    val x = bitmapSize / 2f
    val y = bitmapSize / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(displayChar.toString(), x, y, paint)

    // Collect filled pixel positions with wider spacing for better legibility
    val filledPixels = mutableListOf<Offset>()
    val step = 3 // wider step = more spacing between points, clearer letterforms
    for (px in 0 until bitmapSize step step) {
        for (py in 0 until bitmapSize step step) {
            if (bitmap[px, py] != Color.TRANSPARENT) {
                filledPixels.add(
                    Offset(
                        x = px.toFloat() / bitmapSize,
                        y = py.toFloat() / bitmapSize
                    )
                )
            }
        }
    }

    bitmap.recycle()

    // Return shuffled subset
    return filledPixels.shuffled().take(count)
}

/**
 * Draws the forming particles interpolated between start and target positions.
 */
private fun DrawScope.drawFormingParticles(
    startPoints: List<Offset>,
    targetPoints: List<Offset>,
    particleColors: List<Int>,
    progress: Float
) {
    val canvasW = size.width
    val canvasH = size.height
    val radius = 6.dp.toPx()

    // Add some padding so the letter isn't pressed against edges
    val padX = canvasW * 0.15f
    val padY = canvasH * 0.10f
    val drawW = canvasW - padX * 2
    val drawH = canvasH - padY * 2

    startPoints.forEachIndexed { i, start ->
        val target = targetPoints[i]
        val argb = particleColors[i]

        val startPx = Offset(start.x * canvasW, start.y * canvasH)
        val targetPx = Offset(
            padX + target.x * drawW,
            padY + target.y * drawH
        )

        val currentX = androidx.compose.ui.util.lerp(startPx.x, targetPx.x, progress)
        val currentY = androidx.compose.ui.util.lerp(startPx.y, targetPx.y, progress)

        // Fade in as particles converge
        val alpha = (progress * 1.5f).coerceAtMost(1f)
        val color = ComposeColor(argb).copy(alpha = alpha)

        drawCircle(
            color = color,
            radius = radius * (0.5f + progress * 0.5f), // grow slightly
            center = Offset(currentX, currentY)
        )
    }
}

/**
 * Clusters the target points and creates Konfetti [Party] instances at each cluster center
 * for the burst phase. Uses [burstPreset] to get the base Party config from [KonfettiPresets],
 * then repositions each party at a cluster center derived from the letter shape.
 */
private fun buildBurstParties(
    targetPoints: List<Offset>,
    colors: List<Int>,
    burstPreset: KonfettiPreset
): List<Party> {
    val clusterCount = 8.coerceAtMost(targetPoints.size)
    val clusters = targetPoints.chunked(
        (targetPoints.size / clusterCount).coerceAtLeast(1)
    )

    // Get the base parties from the chosen burst preset for their speed/spread/damping etc.
    val baseParties = KonfettiPresets.getPartiesFromColors(burstPreset, colors)
    val templateParty = baseParties.firstOrNull() ?: Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        spread = 360,
        colors = colors,
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(30),
        position = Position.Relative(0.5, 0.5)
    )

    return clusters.map { cluster ->
        val cx = cluster.map { it.x }.average()
        val cy = cluster.map { it.y }.average()

        // Apply same padding as the draw phase
        val relX = 0.15 + cx * 0.7
        val relY = 0.10 + cy * 0.8

        templateParty.copy(
            colors = colors,
            emitter = Emitter(
                duration = 100,
                TimeUnit.MILLISECONDS
            ).max(cluster.size.coerceAtLeast(5)),
            position = Position.Relative(relX, relY)
        )
    }
}
