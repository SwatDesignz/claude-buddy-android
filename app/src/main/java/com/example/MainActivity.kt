package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.db.AchievementEntity
import com.example.data.db.AppDatabase
import com.example.data.db.LogEntity
import com.example.data.db.PetEntity
import com.example.data.db.ZRepository
import com.example.data.model.AchievementRegistry
import com.example.data.model.SpeciesData
import com.example.data.model.SpeciesSpec
import com.example.data.model.lifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BugGame
import com.example.ui.viewmodel.LuckySpin
import com.example.ui.viewmodel.SimonGame
import com.example.ui.viewmodel.ShinySparkle
import com.example.ui.viewmodel.WildEncounter
import com.example.ui.viewmodel.ZXDigitalPetView
import com.example.ui.viewmodel.ZXDigitalPetViewFactory
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Build Database Instance
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "z_xbuddy_database"
        ).fallbackToDestructiveMigration(true).build()

        val repository = ZRepository(database.petDao(), database.logDao(), database.achievementDao())
        val viewModelFactory = ZXDigitalPetViewFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: ZXDigitalPetView = viewModel(factory = viewModelFactory)
                ZBuddyTerminalApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZBuddyTerminalApp(viewModel: ZXDigitalPetView) {
    val activePet by viewModel.activePet.collectAsStateWithLifecycle()
    val allPets by viewModel.allPets.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingResponse.collectAsStateWithLifecycle()
    val currentInputText by viewModel.currentInputText.collectAsStateWithLifecycle()
    
    val currentTheme by viewModel.terminalColorTheme.collectAsStateWithLifecycle()
    val enableScanlines by viewModel.isScanlineOverlayEnabled.collectAsStateWithLifecycle()
    
    // Pairing diagnostics states
    val isBLEScanning by viewModel.isBLEScanning.collectAsStateWithLifecycle()
    val isBLEConnected by viewModel.isBLEConnected.collectAsStateWithLifecycle()
    val showBLEPairDialog by viewModel.showBLEPairDialog.collectAsStateWithLifecycle()

    // Wild encounter states
    val wildEncounter by viewModel.wildEncounter.collectAsStateWithLifecycle()
    val wildEncounterLog by viewModel.wildEncounterLog.collectAsStateWithLifecycle()
    val wildCaptures by viewModel.wildCaptures.collectAsStateWithLifecycle()

    // Simon Says game states
    val simonGame by viewModel.simonGame.collectAsStateWithLifecycle()
    val simonHighScore by viewModel.simonHighScore.collectAsStateWithLifecycle()

    // Achievement states
    val allAchievements by viewModel.allAchievements.collectAsStateWithLifecycle()
    val unlockedCount by viewModel.unlockedCount.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()

    // Screen State tab - UI controlled locally for ultra-fast, robust response times
    var activeTab by remember { mutableStateOf("CONSOLE") } // CONSOLE, HATCHERY, BLE, THEMES, REVIEW, WILD, GAME, ACHIEVE
    var gameSubTab by remember { mutableStateOf("SIMON") } // SIMON, BUG, SPIN — sub-tabs within F7:GAME

    // Map theme names to cathode ray phosphoresces colors
    val themeColor = remember(currentTheme) {
        when (currentTheme) {
            "Elegant Dark" -> Color(0xFFD0BCFF)
            "Amber Glow" -> Color(0xFFFFB300)
            "Commodore Blue" -> Color(0xFF00E5FF)
            "Cyberpunk Pink" -> Color(0xFFFF007F)
            "Classic White" -> Color(0xFFE2E8F0)
            else -> Color(0xFF00FF66) // Matrix Green
        }
    }

    val themeBackground = remember(currentTheme) {
        when (currentTheme) {
            "Elegant Dark" -> Color(0xFF1C1B1F)
            "Amber Glow" -> Color(0xFF140D02)
            "Commodore Blue" -> Color(0xFF000030)
            "Cyberpunk Pink" -> Color(0xFF16001A)
            "Classic White" -> Color(0xFF111215)
            else -> Color(0xFF060B08) // Matrix Green
        }
    }

    val glowStyle = remember(themeColor) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = themeColor,
            shadow = Shadow(
                color = themeColor.copy(alpha = 0.8f),
                blurRadius = 8f
            )
        )
    }

    // Live Clock Ticker for Terminal Output
    var timeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            timeString = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            delay(10)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF1C1B1F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1C1B1F))
                .padding(totalPadding())
        ) {
            // Retro Terminal Title bar - Styled with Elegant Dark aesthetic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2930))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "ZIG" Badge from the Design HTML
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFD0BCFF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ZXB",
                            color = Color(0xFF381E72),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ZXBuddy",
                        color = Color(0xFFE6E1E5),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Pulse LED indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isBLEConnected) Color(0xFF27C93F) else Color(0xFFFFBD2E),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "v0.16.2",
                        color = Color(0xFFBFCBAD),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(6.dp))
                            .background(Color(0xFF1C1B1F))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "⚙️",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { viewModel.setTerminalTheme("Elegant Dark") }
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CRT Cathode Ray Tube Main Simulated monitor bounds
            CRTScanlineBox(
                modifier = Modifier
                    .weight(if (isWideScreen()) 1.2f else 1f)
                    .fillMaxWidth(),
                themeColor = themeColor,
                bgColor = themeBackground,
                enableScanlines = enableScanlines
            ) {
                when (activeTab) {
                    "CONSOLE" -> {
                        PetConsoleView(
                            activePet = activePet,
                            viewModel = viewModel,
                            glowStyle = glowStyle,
                            themeColor = themeColor,
                            isGenerating = isGenerating
                        )
                    }
                    "HATCHERY" -> {
                        HatcheryView(
                            viewModel = viewModel,
                            allPets = allPets,
                            glowStyle = glowStyle,
                            themeColor = themeColor,
                            onHatchSuccess = { activeTab = "CONSOLE" }
                        )
                    }
                    "BLE" -> {
                        BluetoothSyncView(
                            viewModel = viewModel,
                            isScanning = isBLEScanning,
                            isConnected = isBLEConnected,
                            glowStyle = glowStyle,
                            themeColor = themeColor
                        )
                    }
                    "THEMES" -> {
                        TerminalThemesView(
                            viewModel = viewModel,
                            currentTheme = currentTheme,
                            enableScanlines = enableScanlines,
                            glowStyle = glowStyle,
                            themeColor = themeColor
                        )
                    }
                    "REVIEW" -> {
                        CodeReviewView(
                            viewModel = viewModel,
                            glowStyle = glowStyle,
                            themeColor = themeColor
                        )
                    }
                    "WILD" -> {
                        WildEncounterView(
                            viewModel = viewModel,
                            activePet = activePet,
                            wildEncounter = wildEncounter,
                            encounterLog = wildEncounterLog,
                            wildCaptures = wildCaptures,
                            isGenerating = isGenerating,
                            glowStyle = glowStyle,
                            themeColor = themeColor
                        )
                    }
                    "GAME" -> {
                        // Sub-tab bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                GameSubTabButton("SIMON", gameSubTab, themeColor, Modifier.fillMaxWidth()) { gameSubTab = "SIMON" }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                GameSubTabButton("BUG", gameSubTab, themeColor, Modifier.fillMaxWidth()) { gameSubTab = "BUG" }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                GameSubTabButton("SPIN", gameSubTab, themeColor, Modifier.fillMaxWidth()) { gameSubTab = "SPIN" }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        when (gameSubTab) {
                            "SIMON" -> SimonSaysView(
                                viewModel = viewModel,
                                activePet = activePet,
                                simonGame = simonGame,
                                highScore = simonHighScore,
                                glowStyle = glowStyle,
                                themeColor = themeColor
                            )
                            "BUG" -> BugSaysView(
                                viewModel = viewModel,
                                activePet = activePet,
                                glowStyle = glowStyle,
                                themeColor = themeColor
                            )
                            "SPIN" -> LuckySpinView(
                                viewModel = viewModel,
                                activePet = activePet,
                                glowStyle = glowStyle,
                                themeColor = themeColor
                            )
                        }
                    }
                    "ACHIEVE" -> {
                        AchievementsView(
                            allAchievements = allAchievements,
                            unlockedCount = unlockedCount,
                            totalCount = totalCount,
                            glowStyle = glowStyle,
                            themeColor = themeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time scrolling diagnostics monitor (bottom area) - Styled with Elegant Dark cards and header
            Column(
                modifier = Modifier
                    .height(130.dp)
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2930))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Header of the terminal diagnostics card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL ACTIVITY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF938F99)
                    )
                    Text(
                        text = "Seed: 0x4A7F92",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFFD0BCFF)
                    )
                }
                
                // Thin partition line to reflect nested card layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF49454F))
                )

                // Scrollable messages viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F).copy(alpha = 0.4f)) // translucent inner fill
                        .padding(8.dp)
                ) {
                    val scrollState = rememberScrollState()
                    LaunchedEffect(recentLogs.size) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        if (recentLogs.isEmpty()) {
                            Text(
                                text = "[SYSTEM] Initializing console logs...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        } else {
                            recentLogs.asReversed().map { log ->
                                val logColor = when (log.tag) {
                                    "ERROR" -> Color(0xFFFF5F56)
                                    "SYSTEM" -> Color(0xFFD0BCFF)
                                    "BLE" -> Color(0xFFBFCBAD)
                                    "HATCH" -> Color(0xFFD0BCFF)
                                    "CARE" -> Color(0xFFBFCBAD)
                                    "INTERACT" -> Color(0xFFD0BCFF)
                                    "AI_PENDING" -> Color(0xFFFFBD2E)
                                    else -> themeColor.copy(alpha = 0.6f)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "[${SimpleDateFormat("HH:mm:ss", Locale.US).format(log.timestamp)}] [${log.tag}]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = logColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.message,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 10.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Text Command Input (z-xbuddy contact CLI console) - Styled with Elegant Dark parameters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentInputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    placeholder = {
                        Text(
                            text = "Send direct command to buddy...",
                            color = Color(0xFF938F99),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_input_field"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2B2930),
                        unfocusedContainerColor = Color(0xFF1C1B1F),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFF938F99)
                    ),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    singleLine = true,
                    enabled = activePet != null && !isGenerating
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendChatMessage() },
                    modifier = Modifier
                        .height(52.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("submit_cli_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = activePet != null && !isGenerating && currentInputText.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF381E72))
                    } else {
                        Text("RUN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab bar styled as classic function buttons [F1] [F2] [F3] [F4]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TerminalTabButton(
                    label = "F1:CONSOLE",
                    isActive = activeTab == "CONSOLE",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_console"),
                    onClick = { activeTab = "CONSOLE" }
                )
                TerminalTabButton(
                    label = "F2:HATCHERY",
                    isActive = activeTab == "HATCHERY",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_hatchery"),
                    onClick = { activeTab = "HATCHERY" }
                )
                TerminalTabButton(
                    label = "F3:DESKTOP",
                    isActive = activeTab == "BLE",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_ble"),
                    onClick = { activeTab = "BLE" }
                )
                TerminalTabButton(
                    label = "F4:THEMES",
                    isActive = activeTab == "THEMES",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_themes"),
                    onClick = { activeTab = "THEMES" }
                )
                TerminalTabButton(
                    label = "F5:REVIEW",
                    isActive = activeTab == "REVIEW",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_review"),
                    onClick = { activeTab = "REVIEW" }
                )
                TerminalTabButton(
                    label = "F6:WILD",
                    isActive = activeTab == "WILD",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_wild"),
                    onClick = { activeTab = "WILD" }
                )
                TerminalTabButton(
                    label = "F7:GAME",
                    isActive = activeTab == "GAME",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_game"),
                    onClick = { activeTab = "GAME" }
                )
                TerminalTabButton(
                    label = "F8:ACHV",
                    isActive = activeTab == "ACHIEVE",
                    activeColor = themeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_achieve"),
                    onClick = { activeTab = "ACHIEVE" }
                )
            }
        }
    }

    // Modal popup requesting desktop authorization for BLE sync emulator
    if (showBLEPairDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelPairing() },
            title = {
                Text(
                    text = "PAIR REQ RECEIVED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            },
            text = {
                Column {
                    Text(
                        text = "Authorize BLE pairing with 'Claude Desktop Client'?",
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Target Service UUID:\n6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
                        fontFamily = FontFamily.Monospace,
                        color = themeColor,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This allows the active pet (${activePet?.name ?: "N/A"}) to pipe telemetry, diagnostic updates, and stats variables directly to your code CLI.",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.approvePairing() },
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColor)
                ) {
                    Text("AUTHORIZE[Y]", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelPairing() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("DENY[N]", fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = Color(0xFF14171E),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// Retro Terminal Styled Composable Tab buttons - Elegant Dark Edition
@Composable
fun TerminalTabButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(38.dp)
            .border(1.dp, if (isActive) activeColor.copy(alpha = 0.5f) else Color(0xFF49454F), RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else Color(0xFF2B2930),
            contentColor = if (isActive) Color(0xFF381E72) else Color(0xFF938F99)
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Master CRT layout wrapper that draws scanline cathode tube styling
@Composable
fun CRTScanlineBox(
    modifier: Modifier = Modifier,
    themeColor: Color,
    bgColor: Color,
    enableScanlines: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(bgColor)
            .border(2.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .drawWithContent {
                drawContent()
                if (enableScanlines) {
                    val strokeWidth = 1.dp.toPx()
                    val gap = 3.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = themeColor.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                        y += gap
                    }
                    // Radial gradient to replicate CRT phosphoresce vignette shadow
                    val radialGradient = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width.coerceAtLeast(size.height)
                    )
                    drawRect(brush = radialGradient)
                }
            }
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
fun PetConsoleView(
    activePet: PetEntity?,
    viewModel: ZXDigitalPetView,
    glowStyle: TextStyle,
    themeColor: Color,
    isGenerating: Boolean
) {
    val currentViewFrame by viewModel.currentViewFrame.collectAsStateWithLifecycle()

    if (activePet == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "=== [ NO ACTIVE TERMINAL BUDDY ] ===",
                style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ZXBuddy virtual core needs initializing.\nHatch an algorithmic pet companion module to compile logic parameters.",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "GOTO [F2:HATCHERY] TAB TO BEGIN DETECTORS",
                style = glowStyle.copy(fontSize = 11.sp),
                modifier = Modifier
                    .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    } else {
        val spec = remember(activePet.species) { SpeciesData.getByName(activePet.species) }
        val (frameA, frameB) = remember(activePet.level) {
            when (activePet.lifecycle) {
                "Teen" -> Pair(spec.frame3, spec.frame4)
                "Adult" -> Pair(spec.frame5, spec.frame6)
                else -> Pair(spec.frame1, spec.frame2)
            }
        }
        val petFrame = if (currentViewFrame == 1) frameA else frameB

        // Support Adaptive display - side-by-side if we have tablets / wide layouts
        if (isWideScreen()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left: ASCII Renderer Box
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Badge indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "[LVL ${activePet.level}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "[${activePet.rarity.uppercase()}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = getRarityColor(activePet.rarity),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, getRarityColor(activePet.rarity).copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            if (activePet.isShiny) {
                                Text(
                                    text = "[⭐⭐ SHINY ⭐⭐]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = petFrame,
                            style = glowStyle.copy(fontSize = 18.sp, lineHeight = 20.sp),
                            textAlign = TextAlign.Center,
                            color = if (activePet.isShiny) SpeciesData.getShinyColor(activePet.species) else themeColor
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "==[ ${activePet.name.uppercase()} (${activePet.species}) ]==",
                            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                        if (isGenerating) {
                            Text(
                                text = ">> COMPILING GENERATIVE FEEDBACK...",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = themeColor.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                // Right: Interactive diagnostics details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = ">> DIAGNOSTICS TELEMETRY",
                            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PetStatusBar("Energy node", activePet.energy, themeColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        PetStatusBar("Hunger load", activePet.hunger, Color.Yellow)
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = ">> PERSONALITY CORES",
                            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        StatRow("Debugging Power", "${activePet.debugging}/100")
                        StatRow("Patience Threshold", "${activePet.patience}/100")
                        StatRow("Chaos Parameter", "${activePet.chaos}/100")
                        StatRow("Wisdom Constant", "${activePet.wisdom}/100")
                        StatRow("Snark Coeffecient", "${activePet.snark}/100")
                        Spacer(modifier = Modifier.height(12.dp))
                        BuddyProfilePanel(activePet = activePet, themeColor = themeColor)
                    }

                    // Direct care payload injectors (Action triggers)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActionButton("inject_feed", "FEED", themeColor, onClick = { viewModel.feedPet() })
                        ActionButton("inject_patch", "PATCH", themeColor, onClick = { viewModel.patchCodePet() })
                        ActionButton("inject_poke", "POKE", themeColor, onClick = { viewModel.pokePet() })
                    }
                }
            }
        } else {
            // Mobile portrait nested scrolling viewport
            Column(modifier = Modifier.fillMaxSize()) {
                // Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "LVL ${activePet.level}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activePet.rarity.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = getRarityColor(activePet.rarity),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, getRarityColor(activePet.rarity).copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    if (activePet.isShiny) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHINY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Centered ASCII viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = petFrame,
                            style = glowStyle.copy(fontSize = 18.sp, lineHeight = 20.sp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "== ${activePet.name.uppercase()} (${activePet.species}) ==",
                            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        )
                    }
                }

                HorizontalDivider(color = themeColor.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

                // Statistics metrics readouts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            PetStatusBar("Energy node", activePet.energy, themeColor)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            PetStatusBar("Hunger load", activePet.hunger, Color.Yellow)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SPECS: DBG:${activePet.debugging} | SLW:${activePet.patience} | CHS:${activePet.chaos} | WIS:${activePet.wisdom} | SNK:${activePet.snark}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    BuddyProfilePanel(activePet = activePet, themeColor = themeColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActionButton("inject_feed", "FEED PET", themeColor, modifier = Modifier.weight(1f), onClick = { viewModel.feedPet() })
                        ActionButton("inject_patch", "PATCH XP", themeColor, modifier = Modifier.weight(1f), onClick = { viewModel.patchCodePet() })
                        ActionButton("inject_poke", "POKE COILS", themeColor, modifier = Modifier.weight(1f), onClick = { viewModel.pokePet() })
                    }
                }
            }
        }
    }
}

@Composable
fun BuddyProfilePanel(activePet: PetEntity, themeColor: Color) {
    val focusScore = ((activePet.debugging + activePet.wisdom + activePet.energy) / 3).coerceIn(0, 100)
    val mood = when {
        activePet.energy < 25 -> "SLEEPY"
        activePet.hunger > 75 -> "HUNGRY"
        activePet.chaos > 75 -> "CHAOTIC"
        activePet.patience > 75 -> "ZEN"
        else -> "READY"
    }
    val mission = when (mood) {
        "SLEEPY" -> "Recharge before patching."
        "HUNGRY" -> "Feed nutrition blocks."
        "CHAOTIC" -> "Run a tiny safe experiment."
        "ZEN" -> "Review architecture calmly."
        else -> "Compile the next task."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .background(Color(0xFF191B22).copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = ">> BUDDY PROFILE",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatRow("Mood", mood)
            StatRow("Focus", "$focusScore%")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "MISSION: $mission",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFFBFCBAD)
        )
        Text(
            text = "DEV STATS: streak ${activePet.level}d | session xp ${activePet.xp}/100 | commands ${activePet.level + activePet.xp / 10}",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF938F99)
        )
    }
}

@Composable
fun ActionButton(
    tag: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .testTag(tag)
            .height(38.dp)
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2B2930),
            contentColor = if (color == Color.Yellow) Color(0xFFBFCBAD) else color
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PetStatusBar(label: String, percent: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.Gray
            )
            Text(
                text = "$percent%",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        // Simulated block segmented loading bar
        LinearProgressIndicator(
            progress = { percent.toFloat() / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = color,
            trackColor = Color(0xFF191B22)
        )
    }
}

@Composable
fun HatcheryView(
    viewModel: ZXDigitalPetView,
    allPets: List<PetEntity>,
    glowStyle: TextStyle,
    themeColor: Color,
    onHatchSuccess: () -> Unit
) {
    val hatchName by viewModel.hatchNameText.collectAsStateWithLifecycle()
    val rawSpecies by viewModel.selectedHatchSpecies.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingResponse.collectAsStateWithLifecycle()

    var showHistoryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "=== [ BUDDY HATCHERY SYSTEM ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Roll dynamic seeds to cultivate a custom ZXBuddy simulation instance. 18 species, 5 rarities, and 1% secret shiny variants are possible.",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Grid for selection side-by-side or stacked
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Species grid left / top
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = ">> SELECT PET MODULE SPECIES",
                    style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(SpeciesData.list) { spec ->
                        val isSelected = spec.name == rawSpecies
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isSelected) themeColor else Color(0xFF49454F),
                                    RoundedCornerShape(8.dp)
                                )
                                .background(if (isSelected) themeColor.copy(alpha = 0.15f) else Color(0xFF2B2930))
                                .clickable { viewModel.updateHatchSpecies(spec.name) }
                                .padding(vertical = 10.dp)
                                .testTag("species_cell_${spec.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = spec.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isSelected) themeColor else Color(0xFF938F99),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Description column right/bottom
            val currentSpec = remember(rawSpecies) { SpeciesData.getByName(rawSpecies) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = ">> SELECTED SPECIFICATION",
                    style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Draw species mini ascii preview
                Text(
                    text = currentSpec.frame1,
                    fontFamily = FontFamily.Monospace,
                    color = themeColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = currentSpec.description,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "BASE METRICS PROFILE:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                SpecMiniRow("Debugging", currentSpec.baseDebugging)
                SpecMiniRow("Patience", currentSpec.basePatience)
                SpecMiniRow("Chaos", currentSpec.baseChaos)
                SpecMiniRow("Wisdom", currentSpec.baseWisdom)
                SpecMiniRow("Snark", currentSpec.baseSnark)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom controller actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hatchName,
                onValueChange = { viewModel.updateHatchNameText(it) },
                placeholder = {
                    Text(
                        text = "Type moniker name...",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("hatch_moniker_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2B2930),
                    unfocusedContainerColor = Color(0xFF1C1B1F),
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = Color(0xFF49454F),
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFF938F99)
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.hatchNewPet()
                    onHatchSuccess()
                },
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("initiate_hatch_button"),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color(0xFF381E72)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isGenerating && hatchName.isNotBlank()
            ) {
                Text(
                    text = "zig build compile",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Option to switch between different adopted pets - Beautifully styled with Elegant Dark accent borders
        OutlinedButton(
            onClick = { showHistoryDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .testTag("profile_selector_dropdown"),
            border = BorderStroke(1.dp, Color(0xFF49454F)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF))
        ) {
            Text(
                text = "SWAP BACKUP PROFILES (${allPets.size} AVAILABLE)",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text(
                    text = "SELECT ACTIVE PET SEGMENT",
                    fontFamily = FontFamily.Monospace,
                    color = themeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (allPets.isEmpty()) {
                        Text(
                            text = "No hatched profiles stored. Core database empty.",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else {
                        Box(modifier = Modifier.height(200.dp)) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(allPets) { pet ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectPet(pet.id)
                                                showHistoryDialog = false
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = pet.name,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (pet.isActive) themeColor else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                if (pet.isShiny) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "✨",
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${pet.rarity} ${pet.species} (Lvl ${pet.level})",
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deletePet(pet) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete pet profile",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFF1C1E24))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("CLOSE", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF14171D),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun SpecMiniRow(statName: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = statName, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
        Text(text = "$value", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.LightGray)
    }
}

@Composable
fun BluetoothSyncView(
    viewModel: ZXDigitalPetView,
    isScanning: Boolean,
    isConnected: Boolean,
    glowStyle: TextStyle,
    themeColor: Color
) {
    val aiConnectionStatus by viewModel.aiConnectionStatus.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val providerConfigs by viewModel.providerConfigs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── BLE Section ──
        Text(
            text = "=== [ CLAUDE DESKTOP BLE BRIDGING ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Service Profile target UUID:\n6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
            fontFamily = FontFamily.Monospace,
            color = themeColor.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Sync radar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "Radar sync")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = 1.0f,
                animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                label = "radar scale"
            )
            val radarAlpha by infiniteTransition.animateFloat(
                initialValue = 0.8f, targetValue = 0.0f,
                animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                label = "radar alpha"
            )
            Canvas(modifier = Modifier.size(80.dp)) {
                if (isScanning) drawCircle(color = themeColor, radius = size.width / 2f * scale, alpha = radarAlpha, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                drawCircle(color = if (isConnected) themeColor else Color.Gray.copy(alpha = 0.3f), radius = 24.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
            }
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Search,
                contentDescription = "Bluetooth Status",
                tint = if (isConnected) themeColor else Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isConnected) "🟢 BRIDGE ONLINE" else if (isScanning) "🟡 SCANNING..." else "🔴 DISCONNECTED",
            fontFamily = FontFamily.Monospace, color = if (isConnected) Color(0xFF27C93F) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { if (isConnected) viewModel.disconnectBLE() else viewModel.scanBLE() },
            colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) Color.Red.copy(alpha = 0.8f) else themeColor, contentColor = if (isConnected) Color.White else Color(0xFF381E72)),
            shape = RoundedCornerShape(8.dp), enabled = !isScanning,
            modifier = Modifier.align(Alignment.CenterHorizontally).height(34.dp)
        ) {
            Text(
                if (isConnected) "DISCONNECT" else if (isScanning) "SCANNING..." else "SCAN",
                fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFF49454F))
        Spacer(modifier = Modifier.height(12.dp))

        // ── AI Provider Status Section ──
        Text(
            text = "=== [ AI PROVIDER STATUS ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        com.example.data.model.AiProviderType.entries.forEach { provider ->
            val isActive = selectedProvider == provider
            val cfg = providerConfigs[provider] ?: return@forEach
            val hasKey = cfg.apiKey.isNotBlank()
            val nameStr = provider.displayName

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF1A2E1A) else Color(0xFF1C1B1F)),
                border = BorderStroke(1.dp, if (isActive) Color(0xFF27C93F).copy(alpha = 0.4f) else Color(0xFF49454F)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    Box(modifier = Modifier.size(10.dp).background(
                        when {
                            isActive && hasKey -> Color(0xFF27C93F)
                            hasKey -> Color(0xFFFFBD2E)
                            else -> Color(0xFFFF5F56)
                        },
                        RoundedCornerShape(5.dp)
                    ))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = nameStr, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) Color(0xFF27C93F) else Color(0xFFE6E1E5))
                        Text(
                            text = if (hasKey) "🔑 Key configured" else "⚠️ No API key",
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray
                        )
                    }
                    if (isActive) {
                        Text(text = "● ACTIVE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF27C93F))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Provider quick-test button
        Button(
            onClick = { viewModel.testAIConnection() },
            modifier = Modifier.fillMaxWidth().height(36.dp).border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color(0xFF381E72)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("TEST CURRENT PROVIDER", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // Connection status
        Spacer(modifier = Modifier.height(6.dp))
        val statusText = when (val s = aiConnectionStatus) {
            is com.example.ui.viewmodel.AiStatus.Idle -> "Idle — tap TEST to ping ${selectedProvider.displayName}"
            is com.example.ui.viewmodel.AiStatus.Testing -> "Testing connectivity..."
            is com.example.ui.viewmodel.AiStatus.Connected -> "✅ Connected (${s.latencyMs}ms)"
            is com.example.ui.viewmodel.AiStatus.Failed -> "❌ ${s.message}"
        }
        val statusColor = when (aiConnectionStatus) {
            is com.example.ui.viewmodel.AiStatus.Connected -> Color(0xFF27C93F)
            is com.example.ui.viewmodel.AiStatus.Failed -> Color(0xFFFF5F56)
            is com.example.ui.viewmodel.AiStatus.Testing -> Color(0xFFFFBD2E)
            else -> Color.Gray
        }
        Text(
            text = statusText,
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = statusColor,
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1B1F), RoundedCornerShape(4.dp)).padding(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Provider switching quick commands
        Text(
            text = ">> QUICK SWITCH",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                com.example.data.model.AiProviderType.Sandbox to "SANDBOX",
                com.example.data.model.AiProviderType.Gemini to "GEMINI",
                com.example.data.model.AiProviderType.OpenRouter to "OPENROUTER"
            ).forEach { (provider, label) ->
                Button(
                    onClick = { viewModel.updateSelectedProvider(provider) },
                    modifier = Modifier.weight(1f).height(32.dp).border(
                        1.dp,
                        if (selectedProvider == provider) Color(0xFF27C93F).copy(alpha = 0.5f) else Color(0xFF49454F),
                        RoundedCornerShape(6.dp)
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedProvider == provider) Color(0xFF27C93F).copy(alpha = 0.15f) else Color(0xFF2B2930),
                        contentColor = if (selectedProvider == provider) Color(0xFF27C93F) else Color(0xFF938F99)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TerminalThemesView(
    viewModel: ZXDigitalPetView,
    currentTheme: String,
    enableScanlines: Boolean,
    glowStyle: TextStyle,
    themeColor: Color
) {
    var showConfirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ CATHODE TUBE INTERFACES ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Swap custom terminal phosphoresces overlays and filter indices.",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Themes Swapper Grid
        Text(
            text = ">> SELECT CRT PHOSPHOR PALETTE",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val themePresets = listOf(
            Triple("Elegant Dark", Color(0xFFD0BCFF), "Our signature warm luxury lavender styling preset."),
            Triple("Matrix Green", Color(0xFF00FF66), "A gorgeous binary terminal layout preset."),
            Triple("Amber Glow", Color(0xFFFFB300), "Comfortable warm orange cathode hue for late nights."),
            Triple("Commodore Blue", Color(0xFF00E5FF), "Retro blue glowing phosphor hue inspired by old computers."),
            Triple("Cyberpunk Pink", Color(0xFFFF007F), "A bright, neon glowing magenta design."),
            Triple("Classic White", Color(0xFFE2E8F0), "Monochromatic minimalist system aesthetic.")
        )

        themePresets.map { (themeName, color, desc) ->
            val isSelected = themeName == currentTheme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        1.dp,
                        if (isSelected) themeColor else Color(0xFF49454F),
                        RoundedCornerShape(8.dp)
                    )
                    .background(if (isSelected) themeColor.copy(alpha = 0.15f) else Color(0xFF2B2930))
                    .clickable { viewModel.setTerminalTheme(themeName) }
                    .padding(10.dp)
                    .testTag("theme_row_${themeName.lowercase().replace(" ", "_")}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = themeName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isSelected) themeColor else Color(0xFFE6E1E5),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = desc,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF938F99),
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Intercept Overlay toggles
        Text(
            text = ">> ELECTRONIC OVERLAYS CONTROLS",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                .clickable { viewModel.toggleScanlines() }
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cathode Raster Scanlines",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFE6E1E5)
                )
                Text(
                    text = "Applies 120Hz phosphor screen lines overlay.",
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF938F99),
                    fontSize = 9.sp
                )
            }
            Checkbox(
                checked = enableScanlines,
                onCheckedChange = { viewModel.toggleScanlines() },
                colors = CheckboxDefaults.colors(
                    checkedColor = themeColor,
                    checkmarkColor = Color(0xFF381E72),
                    uncheckedColor = Color(0xFF938F99)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System factory reset purge - Styled with Elegant Dark warning curves
        Button(
            onClick = { showConfirmReset = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .testTag("master_purge_database_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "zig build-system clean-profiles",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = {
                Text(
                    text = "CONFIRM DETECTOR ROOT CLEAN",
                    fontFamily = FontFamily.Monospace,
                    color = Color.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to clean database caches? All virtual companions and diagnostic log files will be recursively deleted.",
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showConfirmReset = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("YES, PURGE DATA", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("ABORT", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF14171E),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun CodeReviewView(
    viewModel: ZXDigitalPetView,
    glowStyle: TextStyle,
    themeColor: Color
) {
    val activePet by viewModel.activePet.collectAsStateWithLifecycle()
    val codeInput by viewModel.codeReviewInput.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingResponse.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ CODE REVIEW SUBSYSTEM ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Paste code below for ${activePet?.name ?: "your buddy"}'s review.",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        if (activePet == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⚠️ No active pet to review code. Hatch one in F2:HATCHERY first!",
                color = Color(0xFFFF5F56),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Code input area — multi-line terminal style
        OutlinedTextField(
            value = codeInput,
            onValueChange = { viewModel.updateCodeReviewInput(it) },
            placeholder = {
                Text(
                    text = "Paste your code here for review...",
                    color = Color(0xFF938F99),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("code_review_input"),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C1B1F),
                unfocusedContainerColor = Color(0xFF1C1B1F),
                focusedBorderColor = themeColor,
                unfocusedBorderColor = Color(0xFF49454F),
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFE6E1E5)
            ),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            enabled = activePet != null && !isGenerating
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.reviewCode() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .testTag("code_review_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColor,
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = activePet != null && !isGenerating && codeInput.isNotBlank()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF381E72))
            } else {
                Text("ANALYZE SOURCE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = ">> REVIEW TIPS",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val tips = buildList {
            add("The review tone depends on your pet's personality stats.")
            activePet?.let { p ->
                if (p.snark > 65) add("High snark → expect roasting of variable names and formatting.")
                if (p.wisdom > 70) add("High wisdom → architectural insight and philosophy.")
                if (p.chaos > 75) add("High chaos → unhinged suggestions and risky optimizations.")
                if (p.debugging > 75) add("High debugging → detailed bug-hunting analysis.")
            }
            add("Set a Gemini API key for AI-powered reviews instead of sandbox mode.")
        }
        for (tip in tips) {
            Text(
                text = "• $tip",
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFBFCBAD),
                fontSize = 9.sp
            )
        }
    }
}

// ── Wild Encounter Tab ────────────────────────────────────────────────────────
@Composable
fun WildEncounterView(
    viewModel: ZXDigitalPetView,
    activePet: PetEntity?,
    wildEncounter: WildEncounter,
    encounterLog: String,
    wildCaptures: Int,
    isGenerating: Boolean,
    glowStyle: TextStyle,
    themeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ WILD ENCOUNTER SUBSYSTEM ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (activePet == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⚠️ No active pet to go hunting. Hatch one in F2:HATCHERY first!",
                color = Color(0xFFFF5F56),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            return
        }

        val zxPoints by viewModel.zxPoints.collectAsStateWithLifecycle()
        val shinySparkles by viewModel.shinySparkles.collectAsStateWithLifecycle()
        val isShinyAnimating by viewModel.isShinyAnimating.collectAsStateWithLifecycle()
        Text(
            text = "Captures this session: $wildCaptures | ZX Points: $zxPoints",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Encounter display area
        when (val enc = wildEncounter) {
            is WildEncounter.None -> {
                // Show idle state with hunt prompt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🌿",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No wild pets in range.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Use /hunt or press the button below to search.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF938F99)
                        )
                    }
                }
            }
            is WildEncounter.Hunting -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = themeColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🌿 ${activePet.name} is searching...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = themeColor
                        )
                    }
                }
            }
            is WildEncounter.Found -> {
                // Wild pet display with shiny effects
                val shinyColor = SpeciesData.getShinyColor(enc.speciesName)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (enc.isShiny) shinyColor.copy(alpha = 0.8f)
                            else getRarityColor(enc.rarity).copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier) {
                        // Main content
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Shiny sparkle header
                            if (enc.isShiny) {
                                Text(
                                    text = "⭐⭐ SHINY ⭐⭐",
                                    color = shinyColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            // ASCII art
                            Text(
                                text = enc.asciiArt,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = if (enc.isShiny) shinyColor else getRarityColor(enc.rarity),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${enc.speciesName} [${enc.rarity}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (enc.isShiny) shinyColor else getRarityColor(enc.rarity)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Capture chance: ${(enc.captureDifficulty * 100).toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }

                        // Shiny sparkle overlay
                        if (enc.isShiny && isShinyAnimating) {
                            ShinySparkleOverlay(
                                sparkles = shinySparkles,
                                shinyColor = shinyColor
                            )
                        }
                    }
                }
            }
            is WildEncounter.Captured -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(1.dp, Color(0xFF27C93F), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✅ CAPTURED!",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF27C93F)
                    )
                }
            }
            is WildEncounter.Escaped -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(1.dp, Color(0xFFFF5F56), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💨 It got away!",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5F56)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Encounter log
        if (encounterLog.isNotBlank()) {
            Text(
                text = encounterLog,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFE6E1E5),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1B1F), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (wildEncounter is WildEncounter.None || wildEncounter is WildEncounter.Escaped || wildEncounter is WildEncounter.Captured) {
                Button(
                    onClick = { viewModel.startWildEncounter() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("wild_hunt_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isGenerating
                ) {
                    Text("🌿 HUNT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            if (wildEncounter is WildEncounter.Found) {
                Button(
                    onClick = { viewModel.attemptCapture() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, Color(0xFF27C93F).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("wild_catch_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF27C93F),
                        contentColor = Color(0xFF0D0D0D)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isGenerating
                ) {
                    Text("🎯 CATCH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.fleeWildEncounter() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, Color(0xFFFF5F56).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("wild_flee_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2B2930),
                        contentColor = Color(0xFFFF5F56)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("👋 FLEE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tips section
        Text(
            text = ">> HUNTING TIPS",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val wildTips = listOf(
            "Higher Wisdom and Patience improve capture chances.",
            "Higher Debugging provides a small capture bonus.",
            "Rarer pets have lower base capture rates.",
            "Shiny pets are extremely rare (~0.4%) and harder to catch.",
            "Successfully caught pets award XP and ZX Points.",
            "Legendary catches award 50 XP + 100 ZX Points!"
        )
        for (tip in wildTips) {
            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                Text(
                    text = "• ",
                    color = themeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = tip,
                    color = Color(0xFF938F99),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// Game sub-tab selector button — call inside a Row to use weight()
@Composable
fun GameSubTabButton(
    label: String,
    currentTab: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isActive = currentTab == label
    Button(
        onClick = onClick,
        modifier = modifier
            .height(30.dp)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.5f) else Color(0xFF49454F),
                RoundedCornerShape(6.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else Color(0xFF2B2930),
            contentColor = if (isActive) Color(0xFF381E72) else Color(0xFF938F99)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Simon Says Memory Game Tab ──────────────────────────────────────────────
@Composable
fun SimonSaysView(
    viewModel: ZXDigitalPetView,
    activePet: PetEntity?,
    simonGame: SimonGame,
    highScore: Int,
    glowStyle: TextStyle,
    themeColor: Color
) {
    val simonColors = listOf(
        Color(0xFFFF4444), // Red 0
        Color(0xFF4488FF), // Blue 1
        Color(0xFF44CC44), // Green 2
        Color(0xFFFFCC00)  // Yellow 3
    )
    val colorNames = listOf("RED", "BLUE", "GREEN", "YELLOW")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ SIMON SAYS ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (activePet == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⚠️ No active pet to play with. Hatch one in F2:HATCHERY first!",
                color = Color(0xFFFF5F56),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            return
        }

        // Score display
        val zxPoints by viewModel.zxPoints.collectAsStateWithLifecycle()
        Text(
            text = "High Score: $highScore | ZX Points: $zxPoints",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Game state display
        when (val game = simonGame) {
            is SimonGame.Idle -> {
                // Welcome screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎮", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Simon Says!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Watch the sequence, then repeat it.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Each round adds a new step.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            is SimonGame.ShowingSequence -> {
                // Show all 4 buttons with current highlight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Round ${game.round} — Watch!",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFFFCC00),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // 2x2 grid of colored buttons
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..1) {
                                val idx = row * 2 + col
                                val isLit = game.highlightIndex == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.3f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isLit) simonColors[idx]
                                            else simonColors[idx].copy(alpha = 0.3f)
                                        )
                                        .border(
                                            2.dp,
                                            if (isLit) Color.White
                                            else simonColors[idx].copy(alpha = 0.5f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLit) {
                                        Text(
                                            text = "●",
                                            fontSize = 24.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            is SimonGame.WaitingInput -> {
                // Player's turn — interactive buttons
                val progress = "${game.playerInput.size}/${game.sequence.size}"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF27C93F).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Round ${game.round} — Your turn! ($progress)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF27C93F),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // 2x2 grid of tappable color buttons
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..1) {
                                val idx = row * 2 + col
                                Button(
                                    onClick = { viewModel.handleSimonInput(idx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.3f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag("simon_btn_$idx"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = simonColors[idx],
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = colorNames[idx],
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            is SimonGame.GameOver -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF5F56), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💥 GAME OVER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            color = Color(0xFFFF5F56),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Score: ${game.score} | Rounds: ${game.roundsCompleted}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = themeColor
                        )
                        Text(
                            text = "+${game.pointsAwarded} ZX Points!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFFFCC00)
                        )
                        if (game.score > 0 && game.score == highScore) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏆 NEW HIGH SCORE!",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        when (simonGame) {
            is SimonGame.Idle, is SimonGame.GameOver -> {
                Button(
                    onClick = { viewModel.startSimonGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("simon_start_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (simonGame is SimonGame.Idle) "🎮 START GAME"
                        else "🔄 PLAY AGAIN",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is SimonGame.ShowingSequence, is SimonGame.WaitingInput -> {
                Button(
                    onClick = { viewModel.resetSimonGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, Color(0xFFFF5F56).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("simon_quit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2B2930),
                        contentColor = Color(0xFFFF5F56)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("QUIT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tips section
        Text(
            text = ">> SIMON SAYS TIPS",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val simonTips = listOf(
            "Watch the sequence carefully before tapping.",
            "Each round adds one more step to the pattern.",
            "Score is based on how many correct taps you make.",
            "Earn ZX Points for each game played.",
            "Can you beat your high score?"
        )
        for (tip in simonTips) {
            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                Text(
                    text = "• ",
                    color = themeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = tip,
                    color = Color(0xFF938F99),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Also available via: /simon or /memory in the console.",
            color = Color(0xFF938F99),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp
        )
    }
}

// ── Whack-a-Bug Mini Game ──────────────────────────────────────────────────
@Composable
fun BugSaysView(
    viewModel: ZXDigitalPetView,
    activePet: PetEntity?,
    glowStyle: TextStyle,
    themeColor: Color
) {
    val bugGame by viewModel.bugGame.collectAsStateWithLifecycle()
    val bugHighScore by viewModel.bugHighScore.collectAsStateWithLifecycle()
    val zxPoints by viewModel.zxPoints.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ WHACK-A-BUG ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (activePet == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⚠️ No active pet to play with. Hatch one in F2:HATCHERY first!",
                color = Color(0xFFFF5F56),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            return
        }

        // Score display
        Text(
            text = "High Score: $bugHighScore | ZX Points: $zxPoints",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (val game = bugGame) {
            is BugGame.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🪳", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Whack-a-Bug!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bugs pop up — tap them to squish!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "30 seconds. Go!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            is BugGame.Playing -> {
                // Game grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏱ ${game.timeLeftSec}s | Score: ${game.score}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (game.timeLeftSec <= 10) Color(0xFFFF5F56) else themeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 3x3 grid
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0..2) {
                                    val idx = row * 3 + col
                                    val hasBug = game.bugs.getOrElse(idx) { false }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.2f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (hasBug) Color(0xFF4A2C0D)
                                                else Color(0xFF1C1B1F)
                                            )
                                            .border(
                                                1.dp,
                                                if (hasBug) Color(0xFFCC7832) else Color(0xFF49454F),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = hasBug) { viewModel.squishBug(idx) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasBug) {
                                            Text(text = "🪳", fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
            is BugGame.GameOver -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF5F56), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏰ TIME'S UP!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            color = Color(0xFFFF5F56),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Squished: ${game.squished} bugs",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = themeColor
                        )
                        Text(
                            text = "+${game.pointsAwarded} ZX Points!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFFFCC00)
                        )
                        if (game.score > 0 && game.score == bugHighScore) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏆 NEW HIGH SCORE!",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        when (bugGame) {
            is BugGame.Idle, is BugGame.GameOver -> {
                Button(
                    onClick = { viewModel.startBugGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (bugGame is BugGame.Idle) "🪳 START GAME"
                        else "🔄 PLAY AGAIN",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is BugGame.Playing -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tips
        Text(
            text = ">> WHACK-A-BUG TIPS",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val tips = listOf(
            "Tap bugs quickly before they escape!",
            "Bugs spawn randomly across the 3×3 grid.",
            "Higher score = more ZX Points earned.",
            "Score 20+ in one game for the Whack Master achievement!",
            "Can you beat your high score?"
        )
        for (tip in tips) {
            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "• ", color = themeColor, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(text = tip, color = Color(0xFF938F99), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}

// ── Lucky Spin Slot Machine ────────────────────────────────────────────────
@Composable
fun LuckySpinView(
    viewModel: ZXDigitalPetView,
    activePet: PetEntity?,
    glowStyle: TextStyle,
    themeColor: Color
) {
    val luckySpin by viewModel.luckySpin.collectAsStateWithLifecycle()
    val zxPoints by viewModel.zxPoints.collectAsStateWithLifecycle()
    val symbols = listOf("🍀", "⚡", "💎")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ LUCKY SPIN ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (activePet == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⚠️ No active pet to gamble with. Hatch one in F2:HATCHERY first!",
                color = Color(0xFFFF5F56),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            return
        }

        Text(
            text = "ZX Points: $zxPoints",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (val spin = luckySpin) {
            is LuckySpin.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎰", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lucky Spin!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bet ZX Points and try your luck!",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Bet buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 25).forEach { bet ->
                        Button(
                            onClick = { viewModel.startLuckySpin(bet) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = zxPoints >= bet
                        ) {
                            Text(
                                "$bet ZX",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            is LuckySpin.Spinning -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = themeColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎰 Spinning...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = themeColor
                        )
                    }
                }
            }
            is LuckySpin.Result -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (spin.winnings > 0) Color(0xFFFFD700) else Color(0xFF49454F),
                            RoundedCornerShape(12.dp)
                        )
                        .background(Color(0xFF0D0D0D), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            spin.reels.forEach { idx ->
                                Text(
                                    text = symbols.getOrElse(idx) { "?" },
                                    fontSize = 36.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (spin.isJackpot) {
                            Text(
                                text = "🎰 JACKPOT! 🎰",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        } else if (spin.winnings > 0) {
                            Text(
                                text = "WINNER!",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = Color(0xFF27C93F),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "No luck!",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = Color(0xFFFF5F56)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (spin.winnings > 0) "+${spin.winnings} ZX Points!"
                            else "Lost ${spin.bet} ZX",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (spin.winnings > 0) Color(0xFFFFCC00) else Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.resetLuckySpin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🔄 SPIN AGAIN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rules
        Text(
            text = ">> LUCKY SPIN RULES",
            style = glowStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val rules = listOf(
            "🍀🍀🍀 Three matching = JACKPOT! (5x your bet)",
            "Two matching = Small win (2x your bet)",
            "No match = Lose your bet",
            "Win your first spin for the Lucky Dev achievement!"
        )
        for (rule in rules) {
            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "• ", color = themeColor, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(text = rule, color = Color(0xFF938F99), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}

// ── Shiny Sparkle Overlay (Pokemon Go style) ─────────────────────────────────
@Composable
fun ShinySparkleOverlay(
    sparkles: List<ShinySparkle>,
    shinyColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        for (sparkle in sparkles) {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(sparkle.delay.toInt() + 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            val offsetX by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (if (sparkle.id % 2 == 0) 10f else -10f),
                animationSpec = infiniteRepeatable(
                    animation = tween(sparkle.delay.toInt() + 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600 + sparkle.delay.toInt(), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Text(
                text = "✨",
                fontSize = (12 * sparkle.size).sp,
                modifier = Modifier
                    .offset(
                        x = (sparkle.x * 200).dp + offsetX.dp,
                        y = (sparkle.y * 180).dp + offsetY.dp
                    )
                    .graphicsLayer { this.alpha = alpha },
                color = shinyColor
            )
        }
    }
}

// ── Achievements Tab ─────────────────────────────────────────────────────────
@Composable
fun AchievementsView(
    allAchievements: List<AchievementEntity>,
    unlockedCount: Int,
    totalCount: Int,
    glowStyle: TextStyle,
    themeColor: Color
) {
    val defs = AchievementRegistry.all

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "=== [ ACHIEVEMENTS ] ===",
            style = glowStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Unlocked $unlockedCount / $totalCount achievements",
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        val progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = themeColor,
            trackColor = Color(0xFF2B2930),
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (defs.isEmpty()) {
            Text(
                text = "No achievements defined.",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            return
        }

        for (def in defs) {
            val entity = allAchievements.find { it.id == def.id }
            val isUnlocked = entity != null && entity.unlockedAt > 0L
            val currentProgress = entity?.progress ?: 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) Color(0xFF1A2E1A) else Color(0xFF1C1B1F)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUnlocked) Color(0xFF27C93F).copy(alpha = 0.4f) else Color(0xFF49454F)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnlocked) def.icon else "🔒",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = def.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color(0xFF27C93F) else Color(0xFF938F99)
                        )
                        Text(
                            text = def.description,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                    if (!isUnlocked && def.maxProgress > 1) {
                        Text(
                            text = "$currentProgress/${def.maxProgress}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF938F99)
                        )
                    }
                }
            }
        }
    }
}

// Helper stat readouts
@Composable
fun StatRow(label: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
        Text(text = valStr, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
    }
}

// Utility mapper for color styling rarity tiers
fun getRarityColor(rarity: String): Color {
    return when (rarity.lowercase()) {
        "legendary" -> Color(0xFFFF8500) // Beautiful bright amber-orange
        "epic" -> Color(0xFFCE00FF)      // Epic bright purple
        "rare" -> Color(0xFF00A2FF)      // Rare system cyan
        "uncommon" -> Color(0xFFA6FF00)  // Uncommon lime green
        else -> Color(0xFF90A4AE)        // Custom slate/gray for common
    }
}

// Quick layout helpers for responsive dimensions
@Composable
fun totalPadding() = if (isWideScreen()) 14.dp else 10.dp

@Composable
fun isWideScreen(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= 600
}
