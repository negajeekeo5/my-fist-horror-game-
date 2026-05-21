package com.example.ui.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class GameState(
    val currentChapter: ChapterType = ChapterType.INTRO,
    val playerX: Float = 1.5f,
    val playerY: Float = 1.5f,
    val playerAngle: Float = 0.0f, // facing south
    val map: GameMap = GameData.MAP_RATS,
    val inventory: List<Item> = emptyList(),
    val messages: List<String> = emptyList(),
    val dialogueLines: List<String> = emptyList(),
    val dialogueIndex: Int = 0,
    val speaker: String = "",
    val activeSprites: List<SpriteEntity> = emptyList(),
    val hp: Float = 100f,
    val maxHp: Float = 100f,
    val bossHp: Float = 100f,
    val maxBossHp: Float = 100f,
    val isCombatActive: Boolean = false,
    val scaredLevel: Float = 0f,
    val isStaticActive: Boolean = false,
    val showTapeMenu: Boolean = false,
    val showScratchWallpaper: Boolean = false,
    val showCredits: Boolean = false,
    val notification: String = ""
)

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private var gameLoopJob: Job? = null

    init {
        startIntro()
        startGameLoop()
    }

    private fun startIntro() {
        _state.value = _state.value.copy(
            currentChapter = ChapterType.INTRO,
            dialogueLines = listOf(
                "As night arrived carrying dreams, warmth, and fantasies for everyone else...",
                "It came to me only as suffering. Sleep was never rest. It was punishment.",
                "Faces crawled from the darkness of a past I could not remember...",
                "Memories that felt stitched into my soul by someone who had lived my childhood for me.",
                "Someone who had worn my skin before I ever truly existed.",
                "Every night I closed my eyes wondering the same thing...",
                "\"When will it finally end?\""
            ),
            dialogueIndex = 0,
            speaker = "Me"
        )
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(120)
                updateGameTick()
            }
        }
    }

    private fun updateGameTick() {
        val current = _state.value
        if (current.currentChapter == ChapterType.INTRO || current.currentChapter == ChapterType.EPILOGUE) return

        // 1. Calculate proximity of sprites to player and dynamic scare levels
        var minDistance = 999f
        for (sprite in current.activeSprites) {
            val dist = kotlin.math.sqrt(
                (sprite.x - current.playerX) * (sprite.x - current.playerX) +
                        (sprite.y - current.playerY) * (sprite.y - current.playerY)
            )
            if (dist < minDistance) {
                minDistance = dist
            }
        }

        val baseScare = if (minDistance < 3.5f) {
            (1f - (minDistance / 3.5f)).coerceIn(0f, 0.9f)
        } else {
            0f
        }

        _state.value = current.copy(
            scaredLevel = baseScare.coerceAtLeast(0.05f)
        )

        // Chapter 2: Dream Maker Boss Fight mechanics
        if (current.isCombatActive && current.currentChapter == ChapterType.THE_BAKER) {
            val updatedSprites = current.activeSprites.map { s ->
                if (s.id == "boss") {
                    // Boss moves slightly
                    val moveAngle = (System.currentTimeMillis() / 800f) % (2 * Math.PI)
                    SpriteEntity(
                        s.id, s.type,
                        x = (4.5f + cos(moveAngle) * 1.5f).toFloat(),
                        y = (1.5f + sin(moveAngle) * 0.5f).toFloat(),
                        label = s.label,
                        color = s.color,
                        scale = s.scale,
                        interactionDistance = s.interactionDistance,
                        isInteractable = s.isInteractable,
                        dialogue = s.dialogue
                    )
                } else s
            }

            // Occasional boss projectile hit if player is directly in front
            if (Math.random() > 0.88) {
                val dx = current.playerX - 4.5f
                if (Math.abs(dx) > 1.0f) {
                    showTransientMessage("Dodged bolt! Strafe using <- and -> !")
                } else {
                    _state.value = _state.value.copy(
                        hp = (current.hp - 10f).coerceAtLeast(10f)
                    )
                    showTransientMessage("Hit by Dark Energy Bolt!")
                }
            }

            _state.value = _state.value.copy(activeSprites = updatedSprites)
        }
    }

    fun nextDialogue() {
        val current = _state.value
        val nextIdx = current.dialogueIndex + 1

        if (nextIdx < current.dialogueLines.size) {
            _state.value = current.copy(dialogueIndex = nextIdx)
        } else {
            // End of dialog line sequences
            val nextChap = when (current.currentChapter) {
                ChapterType.INTRO -> {
                    startChapterOne()
                    return
                }
                ChapterType.EPILOGUE -> {
                    _state.value = current.copy(showCredits = true)
                    return
                }
                else -> current.currentChapter
            }
            _state.value = _state.value.copy(
                dialogueLines = emptyList(),
                dialogueIndex = 0,
                speaker = ""
            )
        }
    }

    private fun startChapterOne() {
        val initialSprites = listOf(
            SpriteEntity("s1", "shadow", 4.5f, 4.5f, "The Shadow", Color.Black, scale = 1.0f, dialogue = listOf(
                "A tall, black figure with enormous white eyes, empty and motionless, staring directly into me.",
                "It has no mouth, no anger, no sadness...",
                "Just deathly, suffocating silence."
            )),
            SpriteEntity("w1", "rat_head", 8.5f, 1.5f, "Rat Worker", Color.Gray, scale = 0.9f, dialogue = listOf(
                "He wears a black business suit, but has the filthy head of a rat...",
                "The creature is speaking into a phone in a tired, lifeless voice."
            )),
            SpriteEntity("tv_desk", "cheese", 5.5f, 1.5f, "Glowing Ceiling Cheese", Color.Yellow, scale = 0.8f, dialogue = listOf(
                "A gigantic piece of cheese hangs near the high ceiling, glowing with a sacred golden hue.",
                "A whisper echoes: \"You have to work for it. Work hard enough, and one day you'll get it.\""
            )),
            SpriteEntity("vhs_bench", "doll", 2.5f, 8.5f, "VHS Storage Corner", Color.Magenta, scale = 0.7f, dialogue = listOf(
                "Stacked numbered VHS tapes sit near an old television.",
                "A magnetic pull whispers to watch them..."
            ))
        )

        _state.value = _state.value.copy(
            currentChapter = ChapterType.THE_TRAPPED_RATS,
            playerX = 1.5f,
            playerY = 1.5f,
            playerAngle = 0f,
            map = GameData.MAP_RATS,
            activeSprites = initialSprites,
            dialogueLines = listOf(
                "I woke beneath a sky drowned in deep blue darkness, beautiful in a cold and lifeless way...",
                "I stumbled to splash water across my face and lifted my head to the mirror.",
                "There was no reflection.",
                "Only a shadow with enormous white eyes, empty and motionless. GET OUT was smeared on the wall.",
                "Explore the office corridors, find the VHS archives to decode this loop state!"
            ),
            dialogueIndex = 0,
            speaker = "Sinner"
        )
    }

    fun handleMove(direction: String) {
        val current = _state.value
        if (current.dialogueLines.isNotEmpty()) return

        val rotSpeed = 0.15f
        val moveSpeed = 0.25f

        var newX = current.playerX
        var newY = current.playerY
        var newAngle = current.playerAngle

        when (direction) {
            "LEFT" -> newAngle += rotSpeed
            "RIGHT" -> newAngle -= rotSpeed
            "FWD" -> {
                newX += sin(current.playerAngle) * moveSpeed
                newY += cos(current.playerAngle) * moveSpeed
            }
            "BACK" -> {
                newX -= sin(current.playerAngle) * moveSpeed
                newY -= cos(current.playerAngle) * moveSpeed
            }
            "SLIDE_LEFT" -> {
                newX -= cos(current.playerAngle) * moveSpeed
                newY += sin(current.playerAngle) * moveSpeed
            }
            "SLIDE_RIGHT" -> {
                newX += cos(current.playerAngle) * moveSpeed
                newY -= sin(current.playerAngle) * moveSpeed
            }
        }

        val map = current.map
        val nextCellX = newX.toInt()
        val nextCellY = newY.toInt()

        if (nextCellX >= 0 && nextCellX < map.width && nextCellY >= 0 && nextCellY < map.height) {
            val cellVal = map.grid[nextCellY * map.width + nextCellX]
            if (cellVal == 0 || cellVal == 2) { // Allow traversing 0 (empty) or 2 (interactive transitions)
                _state.value = current.copy(
                    playerX = newX,
                    playerY = newY,
                    playerAngle = newAngle
                )
            } else {
                showTransientMessage("Path blocked.")
            }
        }
    }

    fun performInteraction() {
        val current = _state.value
        if (current.dialogueLines.isNotEmpty()) return

        var nearest: SpriteEntity? = null
        var mDistance = 999f

        for (sprite in current.activeSprites) {
            val dx = sprite.x - current.playerX
            val dy = sprite.y - current.playerY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < mDistance) {
                mDistance = dist
                nearest = sprite
            }
        }

        if (nearest != null && mDistance <= nearest.interactionDistance) {
            handleSpriteInteraction(nearest)
        } else {
            showTransientMessage("Nothing to interact with here.")
        }
    }

    private fun handleSpriteInteraction(sprite: SpriteEntity) {
        val current = _state.value

        when (sprite.id) {
            "s1" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "The shadow looms directly before you. Its eyes look like a warning or an desperate request.",
                        "\"GET OUT...\" is smeared in thick dark blood beside it."
                    ),
                    dialogueIndex = 0,
                    speaker = "The Shadow"
                )
            }
            "w1" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "You approach the rat-headed man.",
                        "\"Has anyone ever gotten the ceiling cheese before?\" you ask.",
                        "The entire room freezes. Every worker turns toward you in deathly silence.",
                        "The young rat whispers: \"No one did.\"",
                        "Suddenly, in total synchronization, they march to the windows and JUMP into the void!",
                        "You find a left-behind VHS tape labeled #5 in the panic!"
                    ),
                    dialogueIndex = 0,
                    speaker = "Rat Employee"
                )
                // Add VHS to inventory and remove employee
                val updatedInv = current.inventory + GameData.ITEMS["vhs_tape"]!!
                val updatedSprites = current.activeSprites.filter { it.id != "w1" }
                _state.value = _state.value.copy(
                    inventory = updatedInv,
                    activeSprites = updatedSprites
                )
            }
            "tv_desk" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "The gigantic cheese shines on the ceiling, glowing with static interference.",
                        "It is unreachable. Greed turns into ashes."
                    ),
                    dialogueIndex = 0,
                    speaker = "Me"
                )
            }
            "vhs_bench" -> {
                if (current.inventory.any { it.id == "vhs_tape" }) {
                    _state.value = current.copy(showTapeMenu = true)
                } else {
                    _state.value = current.copy(
                        dialogueLines = listOf(
                            "An old huming television sits here beside a VHS deck.",
                            "You need to find a playable videotape first (Search around the workers!)."
                        ),
                        dialogueIndex = 0,
                        speaker = "Corridor Info"
                    )
                }
            }
            "baker" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "He looms impossibly tall, covered in matted white fur. He hands you a warm slice of pie.",
                        "\"Eat. Baking is what I do. Break my rule... and your soul is mine.\"",
                        "\"No soul goes upstairs into my Attic.\"",
                        "The Baker picks up his coat to go hunting in the wild storm."
                    ),
                    dialogueIndex = 0,
                    speaker = "The Baker"
                )
                val updatedSprites = current.activeSprites.filter { it.id != "baker" }
                _state.value = _state.value.copy(
                    inventory = current.inventory + GameData.ITEMS["pie_slice"]!!,
                    activeSprites = updatedSprites
                )
            }
            "attic_door" -> {
                if (current.inventory.any { s -> s.id == "gasoline" }) {
                    escapeRatsLevel()
                } else if (current.inventory.any { s -> s.id == "pie_slice" }) {
                    startCabinStealthAttic()
                } else {
                    showTransientMessage("I should speak with the Baker first.")
                }
            }
            "lily" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "A little girl lies in bed. Her chest grows forest flowers, and stone is slowly devouring her hands.",
                        "Suddenly the Baker stands behind you in the doorway!",
                        "\"What did you do to her?!\" you shout.",
                        "His voice breaks in immense pain: \"I tried everything to save her! She was my daughter Lily.\"",
                        "\"I became greedy, asked the hat-wearing Dream Maker for wealth. But every wish is a horror curse.\"",
                        "\"My wife turned into the raging snowstorm outside. To break the spell, we must scale the peak and kill him!\""
                    ),
                    dialogueIndex = 0,
                    speaker = "Me & Baker"
                )
                startCabinBossConfrontation()
            }
            "boss" -> {
                if (!current.isCombatActive) {
                    _state.value = current.copy(
                        isCombatActive = true,
                        dialogueLines = listOf(
                            "The Dream Maker sits behind a massive desk. Crimson robes, tall twisted dark shadow hat.",
                            "He looks with cruel amusement and fires bolts of dark energy!",
                            "Tap [ATTACK] repeatedly to strike him, slide <- / -> to dodge his charges!"
                        ),
                        dialogueIndex = 0,
                        speaker = "The Dream Maker"
                    )
                }
            }
            "marry" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "The beetle woman Marry smiles gracefully.",
                        "\"Welcome to the Lantern Waltz Festival of Beetalony!\"",
                        "\"The floating river barge city of dreams where no one dies alone.\"",
                        "\"Put on your matching black tuxedo. Let us dance under the amber glows!\""
                    ),
                    dialogueIndex = 0,
                    speaker = "Marry"
                )
            }
            "glitch" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "You touch the flickering glitchy wall...",
                        "CRACK. A sound of glass fracturing shatters the sky!",
                        "Marry looks at you in sheer dread: \"You don't wanna g--\"",
                        "She vanishes. Dancers erase. The entire river city collapses into terminal darkness!"
                    ),
                    dialogueIndex = 0,
                    speaker = "Wall Static"
                )
                // Transition to Final chapter on dialogue close
                viewModelScope.launch {
                    delay(8000)
                    startChapterEndlessEnd()
                }
            }
            "headless_couple" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "A headless couple is sitting on the couch facing a completely dead black TV static screen.",
                        "The headless man is wearing your old gray sweater, your wedding ring.",
                        "You sit next to them on the couch, and feel lighter... free of guilt...",
                        "You look down and realize you have no head anymore either.",
                        "Suddenly a door creaks open down the long hallway!"
                    ),
                    dialogueIndex = 0,
                    speaker = "Me & Wife"
                )
            }
            "crooked_door" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "Inside, a strange blue doll hangs by silver strings.",
                        "Where its face should be, a pale flower blooms, housing a single enormous unblinking eye staring at you.",
                        "It twitches, recognizing you, and tilts its head offering a cup of steaming hot coffee.",
                        "You slam the door shut in absolute terror!"
                    ),
                    dialogueIndex = 0,
                    speaker = "The Doll"
                )
                val updatedSprites = current.activeSprites.filter { it.id != "crooked_door" } +
                        SpriteEntity("skull_boss", "face_demon", 4.5f, 4.5f, "Void Skull", Color.White, scale = 1.3f, dialogue = listOf(
                            "A colossal demon made of shifting faces and a giant pale grinning skull appears globally!",
                            "It screams, shattering the skies!"
                        ))
                _state.value = _state.value.copy(activeSprites = updatedSprites)
            }
            "skull_boss" -> {
                _state.value = current.copy(
                    dialogueLines = listOf(
                        "The giant split grin pale skull of faces screams at you!",
                        "You close your eyes, accepting the void, falling into an endless black tunnel..."
                    ),
                    dialogueIndex = 0,
                    speaker = "Skull Shifter"
                )
                viewModelScope.launch {
                    delay(5000)
                    startEpilogue()
                }
            }
        }
    }

    fun playVhsTape() {
        _state.value = _state.value.copy(
            showTapeMenu = false,
            dialogueLines = listOf(
                "Recording quality is grainy static. Two brothers are shown: Take and Give.",
                "Give murders their parents without remorse.",
                "He chains Take to the floor and forces him to eat the flesh of his own child.",
                "Give shoots Take directly in the head. But Take does not die.",
                "Blood pouring down his face, empty of sanity, Take devours Give alive!",
                "You find gasoline. Sickened beyond words, you pour it and burn the building to ashes!"
            ),
            dialogueIndex = 0,
            speaker = "Tape Video"
        )
        // Set goal met and replace exit triggers
        val updatedInv = _state.value.inventory.filter { it.id != "vhs_tape" } + Companion.GASOLINE_ITEM
        val updatedSprites = _state.value.activeSprites + SpriteEntity("attic_door", "shadow", 8.5f, 8.5f, "Incinerate Escape", Color.Red)
        _state.value = _state.value.copy(
            inventory = updatedInv,
            activeSprites = updatedSprites
        )
        showTransientMessage("GASOLINE Can acquired. Find the exit!")
    }

    fun escapeRatsLevel() {
        startChapterBaker()
    }

    private fun startChapterBaker() {
        val cabinSprites = listOf(
            SpriteEntity("baker", "baker", 1.5f, 5.5f, "The Baker", Color.White, scale = 1.1f),
            SpriteEntity("attic_door", "headless", 6.5f, 6.5f, "Attic stairs", Color.Black, scale = 0.5f)
        )
        _state.value = _state.value.copy(
            currentChapter = ChapterType.THE_BAKER,
            playerX = 1.5f,
            playerY = 1.5f,
            playerAngle = 0f,
            map = GameData.MAP_CABIN,
            activeSprites = cabinSprites,
            hp = 100f,
            dialogueLines = listOf(
                "You collapsed into the freezing blizzard, but woke inside a wooden log cabin.",
                "It smells of smoke, wet wood, and something sweet baking in the fireplace.",
                "The storm outside claws at the walls like an angry beast.",
                "Meet the giant white furred creature baking apple pies."
            ),
            dialogueIndex = 0,
            speaker = "Narrator"
        )
    }

    private fun startCabinStealthAttic() {
        val atticSprites = listOf(
            SpriteEntity("lily", "marry", 4.5f, 4.5f, "Lily in bed", Color.Green, scale = 0.8f)
        )
        _state.value = _state.value.copy(
            playerX = 1.5f,
            playerY = 1.5f,
            activeSprites = atticSprites,
            dialogueLines = listOf(
                "He fell asleep beside the fireplace. You quietly creak climb up the narrow wooden staircase.",
                "The darkness overhead thickens with every step. You open the half-open door..."
            ),
            dialogueIndex = 0,
            speaker = "Attic climb"
        )
    }

    private fun startCabinBossConfrontation() {
        val bossSprites = listOf(
            SpriteEntity("boss", "dream_maker", 4.5f, 1.5f, "The Dream Maker", Color.Red, scale = 1.2f)
        )
        _state.value = _state.value.copy(
            playerX = 4.5f,
            playerY = 6.5f,
            playerAngle = 3.14159f, // Facing north towards boss
            activeSprites = bossSprites,
            dialogueLines = listOf(
                "Together with the Baker, you scale the freezing mountain peaks.",
                "Snow demons leap from the white-outs, but the Baker crushes them with monstrous strength.",
                "At the peak stands a breathing stone mansion. Inside sits the Dream Maker behind his desk.",
                "Prepare for battle to tear out his heart!"
            ),
            dialogueIndex = 0,
            speaker = "Peak Climb"
        )
    }

    fun attackDreamMaker() {
        val current = _state.value
        if (!current.isCombatActive) return

        val nextBossHp = (current.bossHp - 20f).coerceAtLeast(0f)
        if (nextBossHp <= 0f) {
            _state.value = current.copy(
                isCombatActive = false,
                bossHp = 0f,
                inventory = current.inventory + Companion.DREAM_HEART_ITEM,
                dialogueLines = listOf(
                    "Your Pokers plunge deep into the Dream Maker's chest. Black blood pours out.",
                    "You reach inside and tear out his pulsing dark crystal heart!",
                    "The mansion collapses. Below, the ancient village returns!",
                    "The Baker's fur fades, revealing a smiling human man reunited with Asha and Lily.",
                    "You move on, down the road of fog..."
                ),
                dialogueIndex = 0,
                speaker = "Me"
            )
            viewModelScope.launch {
                delay(12000)
                startChapterBeetalony()
            }
        } else {
            _state.value = current.copy(bossHp = nextBossHp)
            showTransientMessage("Struck Dream Maker! Boss HP: ${nextBossHp.toInt()}%")
        }
    }

    private fun startChapterBeetalony() {
        val beetalonySprites = listOf(
            SpriteEntity("marry", "marry", 2.5f, 2.5f, "Marry", Color.Cyan, scale = 1.0f),
            SpriteEntity("glitch", "tv_desk", 5.5f, 7.5f, "Flickering Wall", Color.Green, scale = 0.9f)
        )
        _state.value = _state.value.copy(
            currentChapter = ChapterType.WELCOME_TO_BEETALONY,
            playerX = 1.5f,
            playerY = 1.5f,
            playerAngle = 0f,
            map = GameData.MAP_BEETALONY,
            activeSprites = beetalonySprites,
            inventory = emptyList(), // Dream resolved
            dialogueLines = listOf(
                "You woke slowly beneath the sound of water brushing gently against wood.",
                "Ceilings are covered in hundreds of tiny amber lanterns. Smell of cinnamon and honey.",
                "You are floating in Beetalony - the spectacular city of cruise ships and glowing bridges.",
                "Find Marry near the ship prow!"
            ),
            dialogueIndex = 0,
            speaker = "Me"
        )
    }

    private fun startChapterEndlessEnd() {
        val endSprites = listOf(
            SpriteEntity("headless_couple", "headless", 3.5f, 1.5f, "The Couch Couple", Color.Gray, scale = 1.0f),
            SpriteEntity("crooked_door", "doll", 5.5f, 4.5f, "Flower Doll Room", Color.Magenta, scale = 0.8f)
        )
        _state.value = _state.value.copy(
            currentChapter = ChapterType.THE_ENDLESS_END,
            playerX = 1.5f,
            playerY = 1.5f,
            playerAngle = 0f,
            map = GameData.MAP_UPSIDEDOWN,
            activeSprites = endSprites,
            dialogueLines = listOf(
                "You woke gasping in an endless blue field under a heavy swirling green sky.",
                "Above floats an upside-down wooden house.",
                "A swaying ladder stretches down. You climb inside your home...",
                "The hallways stretch slightly too long. Shadows bend in impossible directions."
            ),
            dialogueIndex = 0,
            speaker = "Me"
        )
    }

    private fun startEpilogue() {
        _state.value = _state.value.copy(
            currentChapter = ChapterType.EPILOGUE,
            showScratchWallpaper = true,
            dialogueLines = listOf(
                "RING. RING. RING. You opened your eyes violently in your real bedroom.",
                "Morning sunlight spills gently through the window. You breathe in relief: \"A dream...\"",
                "But you look at the mirror. The shadow is inside the glass. NO WAY OUT is scratched behind it.",
                "You look at the living room. GET OUT is painted over.",
                "Scratch off the fresh wallpaper paint to read the horrifying warning..."
            ),
            dialogueIndex = 0,
            speaker = "Bedroom Mirror"
        )
    }

    fun scratchWallpaper() {
        _state.value = _state.value.copy(
            showScratchWallpaper = false,
            dialogueLines = listOf(
                "You scratch off the fresh paint with trembling bleeding fingers...",
                "Hidden beneath the words \"GET OUT\" are the real original warnings:",
                "\"DON'T GO OUT!!\"",
                "Behind you, the television buzzes softly with static despite being unplugged.",
                "A soft knock rings on the apartment front door...",
                "Once.",
                "Then again.",
                "Some stories were never meant to have endings at all."
            ),
            dialogueIndex = 0,
            speaker = "Final realization"
        )
    }

    private fun showTransientMessage(msg: String) {
        _state.value = _state.value.copy(notification = msg)
        viewModelScope.launch {
            delay(3500)
            if (_state.value.notification == msg) {
                _state.value = _state.value.copy(notification = "")
            }
        }
    }

    fun dismissTapeMenu() {
        _state.value = _state.value.copy(showTapeMenu = false)
    }

    companion object {
        val GASOLINE_ITEM = Item("gasoline", "Gaseline", "🛢️", "Fuel to burn evidence.")
        val DREAM_HEART_ITEM = Item("dream_heart", "Pulsing Crystal Heart", "🖤", "Humming dark energy core.")
    }
}
