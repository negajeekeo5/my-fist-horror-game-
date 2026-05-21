package com.example.ui.game

import androidx.compose.ui.graphics.Color

enum class ChapterType {
    INTRO,
    THE_TRAPPED_RATS,
    THE_BAKER,
    WELCOME_TO_BEETALONY,
    THE_ENDLESS_END,
    EPILOGUE
}

data class Item(
    val id: String,
    val name: String,
    val icon: String,
    val description: String
)

data class SpriteEntity(
    val id: String,
    val type: String,
    var x: Float,
    var y: Float,
    val label: String,
    val color: Color,
    val scale: Float = 1f,
    val interactionDistance: Float = 1.2f,
    var isInteractable: Boolean = true,
    var dialogue: List<String> = emptyList()
)

data class GameMap(
    val width: Int,
    val height: Int,
    val grid: List<Int>,
    val floorColor: Color,
    val ceilingColor: Color,
    val wallColors: Map<Int, Color>,
    val name: String
)

object GameData {
    val ITEMS = mapOf(
        "vhs_tape" to Item("vhs_tape", "VHS Tape #5", "📼", "Give's ultimate atrocity against Take."),
        "gasoline" to Item("gasoline", "Gasoline Can", "🛢️", "Strong smelling fuel. Perfect for burning evidence."),
        "lighter" to Item("lighter", "Old Metal Lighter", "🔥", "Flickers weakly but burns hot."),
        "pie_slice" to Item("pie_slice", "Warm Apple Pie", "🥧", "Irresistible smell. Baker's offering."),
        "ancient_book" to Item("ancient_book", "Unforgotten Pages", "📖", "Reveals that the Dream Maker's curse breaks only by tearing out his heart."),
        "shadow_blade" to Item("shadow_blade", "Iron Fire-Poke", "🗡️", "Heavy iron tool from the cabin fireplace. Sturdy and sharp."),
        "dream_heart" to Item("dream_heart", "Pulsing Crystal Heart", "🖤", "Dream Maker's glowing core, humming with terrible dark energy."),
        "flower_doll" to Item("flower_doll", "Bliinking Flower Doll", "🧸", "A doll in a blue dress with a single colossal eye in a pale flower face.")
    )

    // Grid sizes: Chapter maps
    // 0 = Empty, 1 = Solid Wall, 2 = Door/Hole, 3 = Special (Static Screens/TVs), 4 = Fireplace, 5 = Fence/Debris
    val MAP_RATS = GameMap(
        width = 10,
        height = 10,
        grid = listOf(
            1,1,1,1,1,1,1,1,1,1,
            1,0,0,0,1,0,0,0,3,1,
            1,0,1,0,1,0,1,1,0,1,
            1,0,1,0,0,0,0,1,0,1,
            1,0,1,1,1,1,0,1,0,1,
            1,0,0,0,0,1,0,0,0,1,
            1,1,1,1,0,1,1,1,0,1,
            1,0,0,0,0,0,0,1,0,1,
            1,0,1,1,1,1,0,0,0,1,
            1,1,1,1,1,1,1,1,1,1
        ),
        floorColor = Color(0xFF1E2122),
        ceilingColor = Color(0xFF090A0A),
        wallColors = mapOf(
            1 to Color(0xFF4C5254), // Moldy concrete
            3 to Color(0xFF2C7D64), // TV Static screen green
            4 to Color(0xFF8B0000)  // Smeared warning blood red
        ),
        name = "Give's Rat Maze Office"
    )

    val MAP_CABIN = GameMap(
        width = 8,
        height = 8,
        grid = listOf(
            1,1,1,1,1,1,1,1,
            1,0,0,0,0,0,4,1,
            1,0,1,0,1,1,0,1,
            1,0,1,0,0,1,0,1,
            1,0,1,1,0,0,0,1,
            1,0,0,0,0,1,0,1,
            1,0,1,1,0,1,2,1, // 2 is door to Attic
            1,1,1,1,1,1,1,1
        ),
        floorColor = Color(0xFF3E2723), // Warm wood floor
        ceilingColor = Color(0xFF1A0E0B),
        wallColors = mapOf(
            1 to Color(0xFF5D4037), // Dark log walls
            4 to Color(0xFFE64A19), // Glowing Fireplace
            2 to Color(0xFF111111)  // Dark staircase upward
        ),
        name = "The Baker's Cozy Trap"
    )

    val MAP_BEETALONY = GameMap(
        width = 10,
        height = 10,
        grid = listOf(
            1,1,1,1,1,1,1,1,1,1,
            1,0,0,0,0,0,0,0,0,1,
            1,0,1,1,0,1,1,1,0,1,
            1,0,1,3,0,0,0,1,0,1, // 3 - Glitching static wall
            1,0,1,1,1,1,0,1,0,1,
            1,0,0,0,0,1,0,1,0,1,
            1,1,1,1,0,0,0,1,0,1,
            1,0,0,1,1,1,1,1,0,1,
            1,0,0,0,0,0,0,0,0,1,
            1,1,1,1,1,1,1,1,1,1
        ),
        floorColor = Color(0xFF0D253F), // Shimmering dark river water
        ceilingColor = Color(0xFF081424), // Starry evening
        wallColors = mapOf(
            1 to Color(0xFFFFB300), // Glowing Golden Boat Railing
            3 to Color(0xFF1DE9B6)  // Glitchy sparkling static
        ),
        name = "Beetalony Lantern River"
    )

    val MAP_UPSIDEDOWN = GameMap(
        width = 10,
        height = 10,
        grid = listOf(
            1,1,1,1,1,1,1,1,1,1,
            1,0,0,0,1,0,0,0,0,1,
            1,0,1,0,1,0,1,1,0,1,
            1,0,1,0,0,0,0,1,0,1,
            1,0,1,1,1,2,0,1,0,1, // 2 = Crooked Flower Doll Door
            1,0,0,0,0,1,0,0,0,1,
            1,1,1,1,0,1,1,1,0,1,
            1,0,0,0,0,0,0,1,0,1,
            1,0,1,1,1,1,0,0,0,1,
            1,1,1,1,1,1,1,1,1,1
        ),
        floorColor = Color(0xFF4A148C), // Violet ceiling-floor
        ceilingColor = Color(0xFF1E0030),
        wallColors = mapOf(
            1 to Color(0xFF5D4037), // Distorted peeling wallpaper brown
            2 to Color(0xFFAB47BC)  // Crooked breathing tiny door
        ),
        name = "The Upside Down Residence"
    )
}
