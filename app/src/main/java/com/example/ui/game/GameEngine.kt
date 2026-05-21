package com.example.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import java.util.UUID
import kotlin.math.*

class GameEngine {

    // Raycasts the current 2D grid map and draws a vintage pseudo-3D first person view
    fun renderPseudo3D(
        drawScope: DrawScope,
        map: GameMap,
        playerX: Float,
        playerY: Float,
        playerAngle: Float, // in radians
        sprites: List<SpriteEntity>,
        scaredLevel: Float // 0f to 1f
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val fov = Math.toRadians(60.0).toFloat() // 60 degree field of view
        val numRays = 80 // Number of vertical columns to draw
        val maxDepth = 12.0f // Maximum rendering distance

        // 1. Draw solid ceiling & floor gradients
        // Ceiling
        drawScope.drawRect(
            color = map.ceilingColor,
            topLeft = Offset(0f, 0f),
            size = Size(width, height / 2)
        )
        // Floor
        drawScope.drawRect(
            color = map.floorColor,
            topLeft = Offset(0f, height / 2),
            size = Size(width, height / 2)
        )

        // Keep track of depth buffer for bills (sprites rendering)
        val zBuffer = FloatArray(numRays) { maxDepth }

        val columnWidth = width / numRays

        // 2. Cast Rays for walls
        for (i in 0 until numRays) {
            // Find relative ray angle
            val rayAngle = (playerAngle - fov / 2) + (i.toFloat() / numRays.toFloat()) * fov

            var distanceToWall = 0.0f
            var hitWall = false
            var wallType = 1

            val eyeX = sin(rayAngle)
            val eyeY = cos(rayAngle)

            while (!hitWall && distanceToWall < maxDepth) {
                distanceToWall += 0.05f

                val testX = (playerX + eyeX * distanceToWall).toInt()
                val testY = (playerY + eyeY * distanceToWall).toInt()

                // Check bounds
                if (testX < 0 || testX >= map.width || testY < 0 || testY >= map.height) {
                    hitWall = true
                    distanceToWall = maxDepth
                } else {
                    val cell = map.grid[testY * map.width + testX]
                    if (cell > 0) {
                        hitWall = true
                        wallType = cell
                    }
                }
            }

            zBuffer[i] = distanceToWall

            // Calculate height of wall slice
            val ceiling = (height / 2.0f) - (height / distanceToWall)
            val floor = height - ceiling

            val sliceHeight = floor - ceiling
            val wallColorOriginal = map.wallColors[wallType] ?: Color.Gray

            // Shading by distance (fog effect)
            val shadeFactor = (1f - (distanceToWall / maxDepth)).coerceIn(0.1f, 1.0f)
            val wallColor = Color(
                red = (wallColorOriginal.red * shadeFactor).coerceIn(0f, 1f),
                green = (wallColorOriginal.green * shadeFactor).coerceIn(0f, 1f),
                blue = (wallColorOriginal.blue * shadeFactor).coerceIn(0f, 1f),
                alpha = 1f
            )

            // Draw wall slice stripe
            drawScope.drawRect(
                color = wallColor,
                topLeft = Offset(i * columnWidth, ceiling),
                size = Size(columnWidth + 1f, sliceHeight)
            )

            // Draw subtle vertical grid splits for depth
            if (distanceToWall < shadowSplitThreshold) {
                drawScope.drawLine(
                    color = Color.Black.copy(alpha = 0.15f * shadeFactor),
                    start = Offset(i * columnWidth, ceiling),
                    end = Offset(i * columnWidth, floor),
                    strokeWidth = 2f
                )
            }
        }

        // 3. Render 2D billboard sprites relative to player (ordered by distance)
        val sortedSprites = sprites.map { sprite ->
            val dx = sprite.x - playerX
            val dy = sprite.y - playerY

            // Transform sprite position using inverse camera-player rotation matrix
            val spriteAngle = atan2(dx, dy) - playerAngle
            var correctedAngle = spriteAngle
            while (correctedAngle < -PI) correctedAngle += (2 * PI).toFloat()
            while (correctedAngle > PI) correctedAngle -= (2 * PI).toFloat()

            val dist = sqrt(dx * dx + dy * dy)
            Triple(sprite, correctedAngle, dist)
        }.sortedByDescending { it.third }

        for (item in sortedSprites) {
            val sprite = item.first
            val angle = item.second
            val dist = item.third

            // Hide entities that are too far or behind player camera line
            if (dist > maxDepth || dist < 0.2f || abs(angle) > fov / 1.5f) continue

            // Determine X screen coordinate projection
            val screenX = (width / 2) + (tan(angle) * (width / 2) / tan(fov / 2)).toFloat()

            // Calculate size based on distance
            val spriteHeight = (height / dist) * sprite.scale
            val spriteWidth = spriteHeight * 0.6f

            val topY = (height / 2.0f) - (spriteHeight / 2.0f)
            val leftX = screenX - (spriteWidth / 2.0f)

            // Check zBuffer column overlap: ensure sprite is actually visible and not hidden behind walls
            val colCheck = (screenX / columnWidth).toInt().coerceIn(0, numRays - 1)
            if (zBuffer[colCheck] < dist - 0.1f) continue

            // Draw the unique silhouette/icon representing the terrifying being!
            drawRetroSprite(
                drawScope = drawScope,
                sprite = sprite,
                leftX = leftX,
                topY = topY,
                spriteWidth = spriteWidth,
                spriteHeight = spriteHeight,
                distance = dist,
                scaredFactor = scaredLevel
            )
        }

        // 4. CRT scanline effect & static interference simulation
        val drawLinesCount = 30
        val spaceBetween = height / drawLinesCount
        for (line in 0 until drawLinesCount) {
            drawScope.drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = Offset(0f, line * spaceBetween),
                end = Offset(width, line * spaceBetween),
                strokeWidth = 3f
            )
        }

        // Creepy vignette bounds mapping to increasing pulse
        val redPulseAlpha = (sin(System.currentTimeMillis() / 250f) * 0.12f + 0.13f + (scaredLevel * 0.4f)).coerceIn(0f, 0.82f)
        drawScope.drawRect(
            color = Color(0x9900000).copy(alpha = redPulseAlpha * 0.35f),
            size = drawScope.size
        )
    }

    private val shadowSplitThreshold = 8f

    // Draws specialized pixel/vector illustrations representing the entities in Nossair Zaidi's book
    private fun drawRetroSprite(
        drawScope: DrawScope,
        sprite: SpriteEntity,
        leftX: Float,
        topY: Float,
        spriteWidth: Float,
        spriteHeight: Float,
        distance: Float,
        scaredFactor: Float
    ) {
        val opacity = (1f - (distance / 12.0f)).coerceIn(0.2f, 1f)
        val headRadius = spriteWidth * 0.35f
        val centerX = leftX + spriteWidth / 2f

        when (sprite.type) {
            "shadow" -> {
                // The ominous looming shadow with terrifying giant glowing white eyes
                // Draw shadowy torso
                drawScope.drawRect(
                    color = Color.Black.copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.2f, topY + spriteHeight * 0.4f),
                    size = Size(spriteWidth * 0.6f, spriteHeight * 0.6f)
                )
                // Head
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.25f),
                    radius = headRadius
                )
                // Glaring giant white eyes
                val eyeWidth = headRadius * 0.4f
                val eyeOffset = headRadius * 0.4f
                val leftEyeX = centerX - eyeOffset
                val rightEyeX = centerX + eyeOffset
                val eyeY = topY + spriteHeight * 0.23f

                val pulseSize = (1f + 0.15f * sin(System.currentTimeMillis() / 150f)).toFloat()

                // Draw solid white eyes
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = opacity),
                    center = Offset(leftEyeX, eyeY),
                    radius = eyeWidth * pulseSize
                )
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = opacity),
                    center = Offset(rightEyeX, eyeY),
                    radius = eyeWidth * pulseSize
                )
            }
            "dream_maker" -> {
                // Tall elegant hat figure, crimson-lined flowy shadow
                // Draw wizard hat
                drawScope.drawRect(
                    color = Color.Black.copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.1f, topY + spriteHeight * 0.45f),
                    size = Size(spriteWidth * 0.8f, spriteHeight * 0.55f)
                )
                // Crimson liner highlighting details
                drawScope.drawRect(
                    color = Color(0xFFC62828).copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.25f, topY + spriteHeight * 0.55f),
                    size = Size(spriteWidth * 0.5f, spriteHeight * 0.4f)
                )
                // Twisted crooked wizard hat top
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.22f),
                    radius = headRadius * 1.3f
                )
                // Giant gleaming golden eyes of cruel amusement
                drawScope.drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = opacity),
                    center = Offset(centerX - headRadius * 0.35f, topY + spriteHeight * 0.23f),
                    radius = headRadius * 0.25f
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = opacity),
                    center = Offset(centerX + headRadius * 0.35f, topY + spriteHeight * 0.23f),
                    radius = headRadius * 0.25f
                )
            }
            "rat_head" -> {
                // Rat headed worker dressed in black suit
                // Black suit jacket body
                drawScope.drawRect(
                    color = Color(0xFF151516).copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.2f, topY + spriteHeight * 0.5f),
                    size = Size(spriteWidth * 0.6f, spriteHeight * 0.5f)
                )
                // Grey furred head
                drawScope.drawCircle(
                    color = Color(0xFF555556).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.35f),
                    radius = headRadius
                )
                // Pink pointed snout / nose
                drawScope.drawCircle(
                    color = Color(0xFFFF8A80).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.38f),
                    radius = headRadius * 0.28f
                )
                // Hollow black eyes
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = opacity),
                    center = Offset(centerX - headRadius * 0.4f, topY + spriteHeight * 0.32f),
                    radius = headRadius * 0.15f
                )
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = opacity),
                    center = Offset(centerX + headRadius * 0.4f, topY + spriteHeight * 0.32f),
                    radius = headRadius * 0.15f
                )
                // Highlight chain dangling
                drawScope.drawRect(
                    color = Color(0xFF90A4AE).copy(alpha = opacity),
                    topLeft = Offset(centerX - headRadius * 0.3f, topY + spriteHeight * 0.58f),
                    size = Size(headRadius * 0.6f, 15f)
                )
            }
            "cheese" -> {
                // Gigantic slice of cheese floating glows beneath pale light
                val glitchPulse = sin(System.currentTimeMillis() / 200f) * 10f
                drawScope.drawCircle(
                    color = Color(0x33FFD54F),
                    center = Offset(centerX, topY + spriteHeight / 2f),
                    radius = spriteWidth * 0.7f + glitchPulse
                )
                // Draw triangular cheese
                drawScope.drawCircle(
                    color = Color(0xFFFFC107).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight / 2f),
                    radius = spriteWidth * 0.45f
                )
                // Bubbles/holes in cheese
                drawScope.drawCircle(
                    color = Color(0xFFFFA000).copy(alpha = opacity),
                    center = Offset(centerX - spriteWidth * 0.15f, topY + spriteHeight * 0.45f),
                    radius = spriteWidth * 0.08f
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFA000).copy(alpha = opacity),
                    center = Offset(centerX + spriteWidth * 0.12f, topY + spriteHeight * 0.55f),
                    radius = spriteWidth * 0.11f
                )
            }
            "baker" -> {
                // Giant fluffy white creature hums softly holding pie
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = opacity * 0.9f),
                    center = Offset(centerX, topY + spriteHeight * 0.45f),
                    radius = spriteWidth * 0.5f
                )
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.25f),
                    radius = headRadius * 1.2f
                )
                // Sad ancient eyes
                drawScope.drawCircle(
                    color = Color(0xFF1E1E24).copy(alpha = opacity),
                    center = Offset(centerX - headRadius * 0.4f, topY + spriteHeight * 0.25f),
                    radius = headRadius * 0.2f
                )
                drawScope.drawCircle(
                    color = Color(0xFF1E1E24).copy(alpha = opacity),
                    center = Offset(centerX + headRadius * 0.4f, topY + spriteHeight * 0.25f),
                    radius = headRadius * 0.2f
                )
                // Apple Pie in massive hands
                drawScope.drawRect(
                    color = Color(0xFF8D6E63).copy(alpha = opacity),
                    topLeft = Offset(centerX - spriteWidth * 0.35f, topY + spriteHeight * 0.55f),
                    size = Size(spriteWidth * 0.7f, spriteHeight * 0.15f)
                )
            }
            "marry" -> {
                // Beautiful sleek black beetle in elegant tailored suit jacket
                drawScope.drawRect(
                    color = Color(0xFF1A1A1D).copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.25f, topY + spriteHeight * 0.45f),
                    size = Size(spriteWidth * 0.5f, spriteHeight * 0.5f)
                )
                // Dark polished shell purple-indigo tint
                drawScope.drawCircle(
                    color = Color(0xFF152A38).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.28f),
                    radius = headRadius
                )
                // Amber hair decoration
                drawScope.drawCircle(
                    color = Color(0xFFFFB300).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.18f),
                    radius = headRadius * 0.38f
                )
            }
            "headless" -> {
                // Headless sweater version of yourself sitting
                // Torso
                drawScope.drawRect(
                    color = Color(0xFF607D8B).copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.15f, topY + spriteHeight * 0.45f),
                    size = Size(spriteWidth * 0.7f, spriteHeight * 0.55f)
                )
                // Empty darkness rising above collar
                drawScope.drawCircle(
                    color = Color(0x33000000),
                    center = Offset(centerX, topY + spriteHeight * 0.32f),
                    radius = headRadius
                )
            }
            "doll" -> {
                // A doll in blue dress, single colossal eye in flower face!
                drawScope.drawRect(
                    color = Color(0xFF1565C0).copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.25f, topY + spriteHeight * 0.45f),
                    size = Size(spriteWidth * 0.5f, spriteHeight * 0.55f)
                )
                // Petals
                val bounce = (sin(System.currentTimeMillis() / 250f) * 4f).toFloat()
                drawScope.drawCircle(
                    color = Color(0xFFE1BEE7).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.28f),
                    radius = (headRadius * 1.3f) + bounce
                )
                // Core eye
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.28f),
                    radius = headRadius * 0.7f
                )
                // Huge unblinking pupil (eerie glowing cyan-blue)
                drawScope.drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.28f),
                    radius = headRadius * 0.4f
                )
            }
            "face_demon" -> {
                // The colossal Skull Demon with a wide split grin
                val redFlicker = if (Math.random() > 0.9) Color(0xFFFF1744) else Color(0x33000000)
                drawScope.drawCircle(
                    color = redFlicker,
                    center = Offset(centerX, topY + spriteHeight / 2f),
                    radius = spriteWidth * 0.9f
                )
                // Shifting darkness body
                drawScope.drawRect(
                    color = Color.Black.copy(alpha = opacity),
                    topLeft = Offset(leftX + spriteWidth * 0.1f, topY + spriteHeight * 0.4f),
                    size = Size(spriteWidth * 0.8f, spriteHeight * 0.6f)
                )
                // Pale giant skull head
                drawScope.drawCircle(
                    color = Color(0xFFECEFF1).copy(alpha = opacity),
                    center = Offset(centerX, topY + spriteHeight * 0.3f),
                    radius = headRadius * 1.4f
                )
                // Hollow black eye sockets
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = opacity),
                    center = Offset(centerX - headRadius * 0.5f, topY + spriteHeight * 0.28f),
                    radius = headRadius * 0.35f
                )
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = opacity),
                    center = Offset(centerX + headRadius * 0.5f, topY + spriteHeight * 0.28f),
                    radius = headRadius * 0.35f
                )
                // Glowing central red pupils inside
                drawScope.drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = opacity),
                    center = Offset(centerX - headRadius * 0.5f, topY + spriteHeight * 0.28f),
                    radius = headRadius * 0.12f
                )
                drawScope.drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = opacity),
                    center = Offset(centerX + headRadius * 0.5f, topY + spriteHeight * 0.28f),
                    radius = headRadius * 0.12f
                )
                // Massive split smile grin
                drawScope.drawRect(
                    color = Color.Black.copy(alpha = opacity),
                    topLeft = Offset(centerX - headRadius * 0.8f, topY + spriteHeight * 0.38f),
                    size = Size(headRadius * 1.6f, headRadius * 0.15f)
                )
            }
        }
    }
}
