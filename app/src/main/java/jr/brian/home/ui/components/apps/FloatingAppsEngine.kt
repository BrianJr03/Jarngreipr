package jr.brian.home.ui.components.apps

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import jr.brian.home.model.floaty.FloatingAppInit
import jr.brian.home.model.floaty.FloatingAppState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Physics engine that makes items float around the screen and bounce off walls
 * and each other. Used for the "Floaty Mode" easter egg.
 *
 * Each item is assigned a random velocity. On each tick the engine:
 * 1. Moves every item by its velocity.
 * 2. Bounces items off the container walls (top / bottom / left / right).
 * 3. Resolves elastic collisions between overlapping item pairs.
 *
 * Items can be rectangular (width ≠ height). Wall bouncing uses exact
 * width/height; item-to-item collision uses a bounding-circle approximation
 * with radius = max(width, height) / 2.
 */
class FloatingAppsEngine {
    /** Current floating positions keyed by item ID (package name, element ID, etc.). */
    val positions: SnapshotStateMap<String, FloatingAppState> = mutableStateMapOf()

    private var containerWidth: Float = 0f
    private var containerHeight: Float = 0f

    /**
     * Initialise (or re-initialise) the engine for the given container size
     * and set of items.  Items that are already tracked keep their current
     * velocity; new ones receive a random velocity.
     *
     * @param apps  list of initial item descriptors
     * @param width  container width in px
     * @param height container height in px
     */
    fun initialize(
        apps: List<FloatingAppInit>,
        width: Float,
        height: Float
    ) {
        containerWidth = width
        containerHeight = height

        // Remove stale entries
        val currentIds = apps.map { it.id }.toSet()
        positions.keys.filterNot { it in currentIds }.forEach { positions.remove(it) }

        // Add / update entries
        for (app in apps) {
            val existing = positions[app.id]
            if (existing != null) {
                // Keep velocity, just update dimensions in case they changed
                positions[app.id] = existing.copy(width = app.width, height = app.height)
            } else {
                positions[app.id] = FloatingAppState(
                    x = app.x,
                    y = app.y,
                    vx = randomVelocity(),
                    vy = randomVelocity(),
                    width = app.width,
                    height = app.height
                )
            }
        }
    }

    /**
     * Advance the simulation by one frame.
     *
     * @param dt delta-time in seconds (typically ~0.016 for 60 fps)
     */
    fun tick(dt: Float) {
        if (containerWidth <= 0f || containerHeight <= 0f) return

        // 1. Move
        for ((id, state) in positions) {
            positions[id] = state.copy(
                x = state.x + state.vx * dt,
                y = state.y + state.vy * dt
            )
        }

        // 2. Wall collisions
        for ((id, state) in positions) {
            var (x, y, vx, vy, w, h) = state

            // Left wall
            if (x < 0f) {
                x = 0f
                vx = abs(vx) * WALL_RESTITUTION
            }
            // Right wall
            if (x + w > containerWidth) {
                x = containerWidth - w
                vx = -abs(vx) * WALL_RESTITUTION
            }
            // Top wall
            if (y < 0f) {
                y = 0f
                vy = abs(vy) * WALL_RESTITUTION
            }
            // Bottom wall
            if (y + h > containerHeight) {
                y = containerHeight - h
                vy = -abs(vy) * WALL_RESTITUTION
            }

            positions[id] = FloatingAppState(x, y, vx, vy, w, h)
        }

        // 3. Item-to-item collisions (bounding-circle approximation)
        val keys = positions.keys.toList()
        for (i in keys.indices) {
            for (j in i + 1 until keys.size) {
                resolveCollision(keys[i], keys[j])
            }
        }
        
        // 4. Keep movement lively by preventing near-stall speeds.
        for ((id, state) in positions) {
            positions[id] = keepSpeedInRange(state)
        }
    }

    private fun resolveCollision(idA: String, idB: String) {
        val a = positions[idA] ?: return
        val b = positions[idB] ?: return

        // Bounding-circle: radius = half of the larger dimension
        val rA = max(a.width, a.height) / 2f
        val rB = max(b.width, b.height) / 2f
        val cxA = a.x + a.width / 2f
        val cyA = a.y + a.height / 2f
        val cxB = b.x + b.width / 2f
        val cyB = b.y + b.height / 2f

        val dx = cxB - cxA
        val dy = cyB - cyA
        val dist = sqrt(dx * dx + dy * dy)
        val minDist = rA + rB

        if (dist >= minDist || dist == 0f) return

        // Normal vector
        val nx = dx / dist
        val ny = dy / dist

        // Relative velocity along normal
        val dvx = a.vx - b.vx
        val dvy = a.vy - b.vy
        val relVelNormal = dvx * nx + dvy * ny

        // Only resolve if objects are moving toward each other
        if (relVelNormal <= 0f) return

        // Elastic collision impulse (equal mass)
        val impulse = relVelNormal * APP_RESTITUTION

        val newAVx = a.vx - impulse * nx
        val newAVy = a.vy - impulse * ny
        val newBVx = b.vx + impulse * nx
        val newBVy = b.vy + impulse * ny

        // Separate overlapping items
        val overlap = minDist - dist
        val sepX = overlap / 2f * nx
        val sepY = overlap / 2f * ny

        positions[idA] = a.copy(
            x = a.x - sepX,
            y = a.y - sepY,
            vx = newAVx,
            vy = newAVy
        )
        positions[idB] = b.copy(
            x = b.x + sepX,
            y = b.y + sepY,
            vx = newBVx,
            vy = newBVy
        )
    }

    private fun randomVelocity(): Float {
        val speed = Random.nextFloat() * (MAX_INITIAL_SPEED - MIN_INITIAL_SPEED) + MIN_INITIAL_SPEED
        return if (Random.nextBoolean()) speed else -speed
    }
    
    private fun keepSpeedInRange(state: FloatingAppState): FloatingAppState {
        val speed = sqrt(state.vx * state.vx + state.vy * state.vy)
        if (speed in MIN_CRUISE_SPEED..MAX_CRUISE_SPEED) return state
        
        // If velocity got too close to zero, kick it in random directions.
        if (speed < 0.001f) {
            return state.copy(vx = randomVelocity(), vy = randomVelocity())
        }
        
        val targetSpeed = speed.coerceIn(MIN_CRUISE_SPEED, MAX_CRUISE_SPEED)
        val scale = targetSpeed / speed
        return state.copy(vx = state.vx * scale, vy = state.vy * scale)
    }

    companion object {
        /** Pixels per second – lower end of initial random speed. */
        private const val MIN_INITIAL_SPEED = 80f

        /** Pixels per second – upper end of initial random speed. */
        private const val MAX_INITIAL_SPEED = 220f

        /** Energy retained after hitting a wall (1.0 = perfectly elastic). */
        private const val WALL_RESTITUTION = 0.95f

        /** Energy retained after app-to-app collision. */
        private const val APP_RESTITUTION = 0.9f
        
        /** Lower bound so items never feel too slow. */
        private const val MIN_CRUISE_SPEED = 110f
        
        /** Upper bound to avoid runaway fast movement. */
        private const val MAX_CRUISE_SPEED = 260f
    }
}

