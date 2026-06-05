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
import com.example.data.db.AppDatabase
import com.example.data.db.LogEntity
import com.example.data.db.PetEntity
import com.example.data.db.ZRepository
import com.example.data.model.SpeciesData
import com.example.data.model.SpeciesSpec
import com.example.data.model.lifecycle
import com.example.ui.theme.MyApplicationTheme
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
        ).fallbackToDestructiveMigration(false).build()

        val repository = ZRepository(database.petDao(), database.logDao())
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

    // Screen State tab - UI controlled locally for ultra-fast, robust response times
    var activeTab by remember { mutableStateOf("CONSOLE") } // CONSOLE, HATCHERY, BLE, THEMES, REVIEW

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
                            textAlign = TextAlign.Center
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        // Sync radar scanning element drawing using Custom Compose Layout
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "Radar sync")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radar scale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radar alpha"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isScanning) {
                    drawCircle(
                        color = themeColor,
                        radius = size.width / 2f * scale,
                        alpha = alpha,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
                drawCircle(
                    color = if (isConnected) themeColor else Color.Gray.copy(alpha = 0.3f),
                    radius = 28.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }

            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Search,
                contentDescription = "Bluetooth Status",
                tint = if (isConnected) themeColor else Color.LightGray,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (isConnected) {
            Text(
                text = "BRIDGING PIPELINE LOGS: SYNC ONLINE [60FPS]",
                fontFamily = FontFamily.Monospace,
                color = themeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Piping live telemetry matrices to Claude Code CLI client...\nActive process bound at index [2].",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.disconnectBLE() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("disconnect_ble_button")
            ) {
                Text("DISRUPT HANDSHAKE pipeline", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        } else {
            Text(
                text = if (isScanning) ">> PROBING NEARBY CARRIERS..." else ">> BRIDGE PIPELINE DISCONNECTED",
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.scanBLE() },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color(0xFF381E72)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isScanning,
                modifier = Modifier
                    .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("scan_ble_button")
            ) {
                Text(
                    text = if (isScanning) "Compiling scanning indices..." else "zig build-sync --target=claude-cli",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
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
            if (activePet != null) {
                if (activePet!!.snark > 65) add("High snark → expect roasting of variable names and formatting.")
                if (activePet!!.wisdom > 70) add("High wisdom → architectural insight and philosophy.")
                if (activePet!!.chaos > 75) add("High chaos → unhinged suggestions and risky optimizations.")
                if (activePet!!.debugging > 75) add("High debugging → detailed bug-hunting analysis.")
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
