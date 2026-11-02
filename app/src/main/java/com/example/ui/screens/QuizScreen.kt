package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Flashcard
import com.example.viewmodel.IqaiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 3. ANKI-STYLE ACTIVE RECALL ENGINE (LOCAL SRS)
 * Flip-animation card UI with modified SM-2 bottom action buttons (Again, Hard, Good, Easy)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: IqaiViewModel,
    onBack: () -> Unit = {}
) {
    val allCards by viewModel.allCards.collectAsState()
    val allDecks by viewModel.allDecks.collectAsState()
    val selectedDeck by viewModel.selectedDeck.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var filterDueOnly by remember { mutableStateOf(false) }

    val filteredCards = remember(allCards, selectedDeck, filterDueOnly) {
        val now = System.currentTimeMillis()
        allCards.filter { card ->
            (selectedDeck == null || card.deckName == selectedDeck) &&
                    (!filterDueOnly || card.dueDate <= now)
        }
    }

    var currentIndex by remember(filteredCards.size, selectedDeck, filterDueOnly) { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Active Recall (Anki SM-2)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${filteredCards.size} cards • ${if (filterDueOnly) "Due Only" else "All Cards"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { filterDueOnly = !filterDueOnly }) {
                        Icon(
                            if (filterDueOnly) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = "Toggle due filter",
                            tint = if (filterDueOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Flashcard")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Deck Tabs
            ScrollableTabRow(
                selectedTabIndex = if (selectedDeck == null) 0 else (allDecks.indexOf(selectedDeck) + 1).coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedDeck == null,
                    onClick = {
                        viewModel.selectDeck(null)
                        currentIndex = 0
                        isFlipped = false
                    },
                    text = { Text("All Decks (${allCards.size})") }
                )
                allDecks.forEach { deck ->
                    val deckCount = allCards.count { it.deckName == deck }
                    Tab(
                        selected = selectedDeck == deck,
                        onClick = {
                            viewModel.selectDeck(deck)
                            currentIndex = 0
                            isFlipped = false
                        },
                        text = { Text("$deck ($deckCount)") }
                    )
                }
            }

            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎉", fontSize = 32.sp)
                            }
                        }
                        Text(
                            text = if (filterDueOnly) "All due cards reviewed!" else "No cards in this deck yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap the + button to author a new flashcard or switch deck filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Flashcard")
                        }
                    }
                }
            } else {
                val currentCard = filteredCards.getOrNull(currentIndex.coerceIn(0, filteredCards.size - 1))

                if (currentCard != null) {
                    // Progress Indicator
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Card ${currentIndex + 1} of ${filteredCards.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val dueFormatted = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(currentCard.dueDate))
                            Text(
                                text = "Interval: ${currentCard.interval}d • Due: $dueFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / filteredCards.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    // 3D Flip Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FlipFlashcard(
                            card = currentCard,
                            isFlipped = isFlipped,
                            onToggleFlip = { isFlipped = !isFlipped }
                        )
                    }

                    // Action Controls
                    if (!isFlipped) {
                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reveal Answer (Space / Tap Card)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        // 4 Anki-Style Action Buttons with SM-2 Ratings
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Rate Recall Confidence (Modified SM-2):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Again (Lapse)
                                Button(
                                    onClick = {
                                        viewModel.reviewCard(currentCard.id, 1)
                                        isFlipped = false
                                        if (currentIndex < filteredCards.size - 1) {
                                            currentIndex += 1
                                        } else {
                                            currentIndex = 0
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("1 day", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                // 2. Hard
                                Button(
                                    onClick = {
                                        viewModel.reviewCard(currentCard.id, 2)
                                        isFlipped = false
                                        if (currentIndex < filteredCards.size - 1) {
                                            currentIndex += 1
                                        } else {
                                            currentIndex = 0
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Hard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val hardDays = maxOf(1, (currentCard.interval * 1.2).toInt())
                                        Text("${hardDays}d", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                // 3. Good
                                Button(
                                    onClick = {
                                        viewModel.reviewCard(currentCard.id, 3)
                                        isFlipped = false
                                        if (currentIndex < filteredCards.size - 1) {
                                            currentIndex += 1
                                        } else {
                                            currentIndex = 0
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Good", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val goodDays = if (currentCard.interval <= 1) 3 else (currentCard.interval * currentCard.easeFactor).toInt()
                                        Text("${goodDays}d", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                // 4. Easy
                                Button(
                                    onClick = {
                                        viewModel.reviewCard(currentCard.id, 4)
                                        isFlipped = false
                                        if (currentIndex < filteredCards.size - 1) {
                                            currentIndex += 1
                                        } else {
                                            currentIndex = 0
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Easy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val easyDays = if (currentCard.interval <= 1) 6 else (currentCard.interval * currentCard.easeFactor * 1.3).toInt()
                                        Text("${easyDays}d", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Flashcard Dialog
    if (showAddDialog) {
        var deckName by remember { mutableStateOf(selectedDeck ?: "Chemistry Core") }
        var frontText by remember { mutableStateOf("") }
        var backText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Active Recall Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = deckName,
                        onValueChange = { deckName = it },
                        label = { Text("Deck Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        label = { Text("Front (Prompt / Question)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = backText,
                        onValueChange = { backText = it },
                        label = { Text("Back (Answer / Solution)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (frontText.isNotBlank() && backText.isNotBlank()) {
                        viewModel.addFlashcard(deckName, frontText, backText)
                        showAddDialog = false
                    }
                }) {
                    Text("Save Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 3D Card Flip Animation using graphicsLayer
 */
@Composable
fun FlipFlashcard(
    card: Flashcard,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    val density = LocalDensity.current.density

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 380.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onToggleFlip() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            }
        ),
        border = BorderStroke(
            1.5.dp,
            if (rotation <= 90f) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            if (rotation <= 90f) {
                // Front Side
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = card.deckName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = card.frontText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Tap to reveal answer", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                // Back Side (mirrored horizontally so text renders correctly)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "SOLUTION / RECALL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = card.backText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Ease Factor: %.2f • Select confidence below".format(card.easeFactor),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
