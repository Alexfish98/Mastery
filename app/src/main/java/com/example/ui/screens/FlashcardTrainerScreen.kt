package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StandaloneFlashcard
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardTrainerScreen(
    viewModel: ChemistryViewModel
) {
    val flashcards by viewModel.flashcards.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedSubject by remember { mutableStateOf("All") }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Seed initial high-yield flashcards if empty
    LaunchedEffect(flashcards.size) {
        if (flashcards.isEmpty()) {
            val starterCards = listOf(
                StandaloneFlashcard("fc_chem1", "What is the principle commercial ore of Aluminium?", "Bauxite (hydrated aluminium oxide)", "Chemistry"),
                StandaloneFlashcard("fc_chem2", "Industrial process for manufacturing Sulphuric Acid (H₂SO₄)?", "Contact Process (using V₂O₅ catalyst)", "Chemistry"),
                StandaloneFlashcard("fc_math1", "Derivative of sin(2x) with respect to x?", "2 cos(2x) (using chain rule)", "Mathematics"),
                StandaloneFlashcard("fc_math2", "Formula for the sum of an infinite geometric series (|r| < 1)?", "S_∞ = a / (1 - r)", "Mathematics"),
                StandaloneFlashcard("fc_phys1", "What is the SI unit of electric capacitance?", "Farad (F)", "Physics"),
                StandaloneFlashcard("fc_phys2", "Newton's Second Law of Motion formula?", "F_net = m · a (Force = mass × acceleration)", "Physics"),
                StandaloneFlashcard("fc_bio1", "Which organelle is the site of aerobic cellular respiration?", "Mitochondria", "Biology"),
                StandaloneFlashcard("fc_eng1", "Meaning of 'Ephemeral'?", "Lasting for a very short time; transitory", "English")
            )
            starterCards.forEach { viewModel.addFlashcard(it.front, it.back, it.subject) }
        }
    }

    val filteredCards = remember(flashcards, selectedSubject) {
        if (selectedSubject == "All") flashcards
        else flashcards.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }

    val activeCard = filteredCards.getOrNull(currentCardIndex.coerceIn(0, (filteredCards.size - 1).coerceAtLeast(0)))

    // Flip rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "CardFlip"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Deck filter chips
            val subjects = listOf("All", "Chemistry", "Mathematics", "Physics", "Biology", "English", "General")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { subj ->
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = {
                            selectedSubject = subj
                            currentCardIndex = 0
                            isFlipped = false
                        },
                        label = { Text(subj, fontSize = 12.sp) }
                    )
                }
            }

            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No flashcards found in this subject. Tap '+' to create one!")
                }
            } else if (activeCard != null) {
                // Header status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card ${currentCardIndex + 1} of ${filteredCards.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = activeCard.subject,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Interactive 3D Flip Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .graphicsLayer {
                                if (rotation > 90f) rotationY = 180f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isFlipped) "💡 ANSWER" else "❓ QUESTION / PROMPT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (isFlipped) activeCard.back else activeCard.front,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (!isFlipped) "(Tap anywhere to flip card)" else "(Tap to see question)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // SRS Evaluation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.reviewFlashcard(activeCard.id, false)
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % filteredCards.size
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Again", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.reviewFlashcard(activeCard.id, true)
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % filteredCards.size
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Good", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.reviewFlashcard(activeCard.id, true)
                            isFlipped = false
                            currentCardIndex = (currentCardIndex + 1) % filteredCards.size
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Mastered", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var frontText by remember { mutableStateOf("") }
        var backText by remember { mutableStateOf("") }
        var subjectText by remember { mutableStateOf("Chemistry") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Study Flashcard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = frontText,
                        onValueChange = { frontText = it },
                        label = { Text("Front (Question or Concept)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = backText,
                        onValueChange = { backText = it },
                        label = { Text("Back (Answer or Explanation)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subjectText,
                        onValueChange = { subjectText = it },
                        label = { Text("Subject (Math, Physics, Chemistry...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (frontText.isNotBlank() && backText.isNotBlank()) {
                            viewModel.addFlashcard(
                                front = frontText.trim(),
                                back = backText.trim(),
                                subject = subjectText.trim().ifBlank { "General" }
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
