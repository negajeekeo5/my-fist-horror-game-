package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.*
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val engine = remember { GameEngine() }

    // Heartbeat breath pulsing for immersion vignettes
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000 + (1000 * (1f - state.scaredLevel)).toInt(), easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HorrorCoal)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Main split layout: Header summary, 3D Canvas, Controls Dock & Inventory
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER TITLE WITH SURREAL CHRONIC ILLUSION DESIGNS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HorrorAsh.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
                    .border(1.dp, HorrorRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "WAS IT REAL?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HorrorGlowRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = state.map.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HorrorMist,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // HP Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health",
                        tint = HorrorGlowRed,
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${state.hp.toInt()}%",
                        color = if (state.hp < 40f) HorrorGlowRed else HorrorWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 3D RAYCASTING VIEWPORT WRAPPED IN RETRO VINTAGE TELEVISION BOX
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .border(4.dp, HorrorAsh, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                // Raycasting pseudo-3D engine Canvas
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    engine.renderPseudo3D(
                        drawScope = this,
                        map = state.map,
                        playerX = state.playerX,
                        playerY = state.playerY,
                        playerAngle = state.playerAngle,
                        sprites = state.activeSprites,
                        scaredLevel = state.scaredLevel
                    )
                }

                // Scared Vignette pulse (Red heartbeat borders)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = (12.dp * state.scaredLevel * pulseScale),
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Transparent, HorrorRed.copy(alpha = 0.42f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                )

                // Render dynamic text on the center when events happen
                if (state.notification.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .border(1.dp, HorrorGlowRed, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.notification,
                            color = HorrorGlowRed,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // CRT screen static indicator
                if (state.scaredLevel > 0.65f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.04f))
                    )
                }
            }

            // 2.5 Boss Health Bar if Combat is active
            if (state.isCombatActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "THE DREAM MAKER HP",
                        color = HorrorGlowRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { state.bossHp / state.maxBossHp },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.dp, HorrorGlowRed, RoundedCornerShape(5.dp)),
                        color = HorrorGlowRed,
                        trackColor = HorrorCoal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. CONTROLS NAVIGATION DOCK & INVENTORY GRID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A. MOVEMENT PAD
                Box(
                    modifier = Modifier
                        .size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Up Arrow
                    IconButton(
                        onClick = { viewModel.handleMove("FWD") },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(46.dp)
                            .background(HorrorAsh, CircleShape)
                            .border(2.dp, HorrorRed, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Forward", tint = HorrorWhite)
                    }

                    // Left turn Arrow
                    IconButton(
                        onClick = { viewModel.handleMove("LEFT") },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(46.dp)
                            .background(HorrorAsh, CircleShape)
                            .border(2.dp, HorrorRed, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Turn Left", tint = HorrorWhite)
                    }

                    // Action center button
                    Button(
                        onClick = { viewModel.performInteraction() },
                        colors = ButtonDefaults.buttonColors(containerColor = HorrorRed),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .border(1.dp, HorrorGlowRed, CircleShape),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "ACT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorrorWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Right turn Arrow
                    IconButton(
                        onClick = { viewModel.handleMove("RIGHT") },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(46.dp)
                            .background(HorrorAsh, CircleShape)
                            .border(2.dp, HorrorRed, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Turn Right", tint = HorrorWhite)
                    }

                    // Down Arrow
                    IconButton(
                        onClick = { viewModel.handleMove("BACK") },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(46.dp)
                            .background(HorrorAsh, CircleShape)
                            .border(2.dp, HorrorRed, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Backward", tint = HorrorWhite)
                    }
                }

                // B. SIDE ACTIONS & INVENTORY ITEMS
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Combat Strafe Buttons + Attack Button
                    if (state.isCombatActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.handleMove("SLIDE_LEFT") },
                                colors = ButtonDefaults.buttonColors(containerColor = HorrorAsh),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("<- DODGE", fontSize = 10.sp, color = HorrorWhite)
                            }

                            Button(
                                onClick = { viewModel.handleMove("SLIDE_RIGHT") },
                                colors = ButtonDefaults.buttonColors(containerColor = HorrorAsh),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("DODGE ->", fontSize = 10.sp, color = HorrorWhite)
                            }
                        }

                        Button(
                            onClick = { viewModel.attackDreamMaker() },
                            colors = ButtonDefaults.buttonColors(containerColor = HorrorGlowRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "THROW FIRE-POKER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        // Regular Inventory title
                        Text(
                            text = "INVENTORY BINDING",
                            fontSize = 11.sp,
                            color = HorrorMist,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // 3 Slot Inventory List
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(HorrorAsh.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(1.dp, HorrorRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (slot in 0..2) {
                                val item = state.inventory.getOrNull(slot)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(HorrorCoal, RoundedCornerShape(4.dp))
                                        .border(1.dp, if (item != null) HorrorGlowRed else HorrorAsh, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item != null) {
                                        Text(
                                            text = item.icon,
                                            fontSize = 20.sp,
                                            modifier = Modifier.clickable {
                                                // Explain item or trigger event
                                                if (item.id == "vhs_tape") {
                                                    viewModel.playVhsTape()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Use [ACT] nearby entities or items.",
                            fontSize = 9.sp,
                            color = HorrorMist,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. OVERLAYS & SEQUENCES (2D VISUAL NOVEL STORY READER)
        AnimatedVisibility(
            visible = state.dialogueLines.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { viewModel.nextDialogue() } // Tap anywhere to advance dialogue line
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HorrorCoal, RoundedCornerShape(12.dp))
                        .border(2.dp, HorrorGlowRed, RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = state.speaker.uppercase(Locale.ROOT),
                        color = HorrorGlowRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = state.dialogueLines.getOrNull(state.dialogueIndex) ?: "",
                        color = HorrorWhite,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "▶ CLICK ANYWHERE TO CONTINUE...",
                        color = HorrorMist,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. VHS TAPES ARCHIVE SCREEN (THE REVELATION LOG)
        AnimatedVisibility(
            visible = state.showTapeMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(HorrorAsh, RoundedCornerShape(8.dp))
                        .border(1.dp, HorrorGlowRed, RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VHS PLAYER RECONSTITUTION",
                        color = HorrorGlowRed,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "You found: VHS Tape #5.\nInsert it into the television to reconstitution the truth.",
                        color = HorrorWhite,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.playVhsTape() },
                        colors = ButtonDefaults.buttonColors(containerColor = HorrorRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶ INSERT VIDEOTAPE #5", color = Color.White, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CANCEL",
                        color = HorrorMist,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { viewModel.dismissTapeMenu() }
                            .padding(8.dp)
                    )
                }
            }
        }

        // 6. SCRATCH WALLPAPER WALL EVENT (EPILOGUE SWEET REVELATION)
        AnimatedVisibility(
            visible = state.showScratchWallpaper,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            var scratchCount by remember { mutableStateOf(0) }
            val completed = scratchCount >= 5

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(HorrorAsh, RoundedCornerShape(12.dp))
                        .border(1.dp, HorrorGlowRed, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SCRATCH THE WALLPAPER",
                        color = HorrorGlowRed,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Part of the wall beside the message has been freshly painted over. Scratch it away to find what has been hidden.",
                        color = HorrorWhite,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )

                    // Touch Area for Scratch effect simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(if (completed) Color(0x33831A1A) else Color.DarkGray, RoundedCornerShape(8.dp))
                            .border(1.dp, HorrorGlowRed, RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    if (scratchCount < 5) {
                                        scratchCount += 1
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (completed) {
                            Text(
                                text = "DON'T GO OUT",
                                color = HorrorGlowRed,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        } else {
                            Text(
                                text = "SWIPE SCREEN REPEATEDLY\nTO SCRATCH AWAY PAINT\n($scratchCount/5)",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (completed) {
                        Button(
                            onClick = { viewModel.scratchWallpaper() },
                            colors = ButtonDefaults.buttonColors(containerColor = HorrorRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("FACE WHAT'S OUTSIDE...", color = Color.White)
                        }
                    }
                }
            }
        }

        // 7. END GAME CREDITS SCREEN (EPILOGUE END)
        AnimatedVisibility(
            visible = state.showCredits,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "WAS IT REAL\nOR A LONG WEIRD DREAM",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = HorrorGlowRed,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Adapted from the brilliant novel by\nNOSSAIR ZAIDI",
                        color = HorrorWhite,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Text(
                        text = "Developer Notes:\nYou have faced the trapped rats of Give, scaled the blizzard to free the Baker, danced in Beetalony, entered the upside down house, and learned that some nightmares wait quietly beneath reality.",
                        color = HorrorMist,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 40.dp)
                    )

                    Button(
                        onClick = { viewModel.escapeRatsLevel() }, // Replay!
                        colors = ButtonDefaults.buttonColors(containerColor = HorrorRed)
                    ) {
                        Text("PLAY AGAIN", color = Color.White)
                    }
                }
            }
        }
    }
}
