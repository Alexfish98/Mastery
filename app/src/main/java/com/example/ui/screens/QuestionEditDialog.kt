package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Question
import com.example.model.QuestionBank
import com.example.viewmodel.ChemistryViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditDialog(
    question: Question?,
    currentBank: QuestionBank,
    allBanks: List<QuestionBank>,
    viewModel: ChemistryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isNew = question == null

    var prompt by remember { mutableStateOf(question?.prompt ?: "") }
    var answersText by remember { mutableStateOf(question?.answers?.joinToString("\n") ?: "") }
    var optionsText by remember { mutableStateOf(question?.options?.joinToString("\n") ?: "") }
    var topic by remember { mutableStateOf(question?.topic ?: "General") }
    var difficulty by remember { mutableStateOf(question?.difficulty ?: "medium") }
    var type by remember { mutableStateOf(question?.type ?: "text") }
    var tolerance by remember { mutableStateOf(question?.tol?.toString() ?: "") }
    var explanation by remember { mutableStateOf(question?.explanation ?: "") }
    var memoryTip by remember { mutableStateOf(question?.memoryTip ?: "") }
    var hintsText by remember { mutableStateOf(question?.hints?.joinToString("\n") ?: "") }
    var selectedBankId by remember { mutableStateOf(currentBank.id) }
    var alsoReverse by remember { mutableStateOf(false) }
    var imageUriOrPath by remember { mutableStateOf(question?.img) }

    var cameraCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            cameraCapturedBitmap = bitmap
            try {
                val file = File(context.filesDir, "chem_img_${System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
                out.close()
                imageUriOrPath = file.absolutePath
            } catch (_: Exception) {}
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUriOrPath = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New Question" else "Edit Question", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Question prompt (use [[word]] for cloze blank)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = answersText,
                    onValueChange = { answersText = it },
                    label = { Text("Accepted answers (one per line)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = optionsText,
                    onValueChange = { optionsText = it },
                    label = { Text("Choices for Multiple Choice (one per line)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Topic") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = { Text("Difficulty (easy/med/hard)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type (text/number/formula)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tolerance,
                        onValueChange = { tolerance = it },
                        label = { Text("Tolerance (e.g. 0.01)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memoryTip,
                    onValueChange = { memoryTip = it },
                    label = { Text("Memory tip") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hintsText,
                    onValueChange = { hintsText = it },
                    label = { Text("Hints (one per line)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Diagram image section with Camera and Photo Picker!
                Text("Diagram / Chemical Scheme Image", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (cameraCapturedBitmap != null) {
                    Image(
                        bitmap = cameraCapturedBitmap!!.asImageBitmap(),
                        contentDescription = "Diagram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                } else if (!imageUriOrPath.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUriOrPath,
                        contentDescription = "Diagram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery")
                    }
                }

                if (isNew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = alsoReverse, onCheckedChange = { alsoReverse = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Create reverse card (answer → question)")
                    }
                }

                if (question != null) {
                    TextButton(
                        onClick = {
                            viewModel.deleteQuestion(question)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete this question")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val answersList = answersText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val optionsList = optionsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val hintsList = hintsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

                    if (prompt.isBlank() || answersList.isEmpty()) {
                        viewModel.showToast("Prompt and at least one answer are required.")
                        return@Button
                    }

                    viewModel.saveQuestion(
                        existing = question,
                        prompt = prompt.trim(),
                        answers = answersList,
                        options = optionsList,
                        topic = topic.trim(),
                        difficulty = difficulty.trim().lowercase(),
                        type = type.trim().lowercase(),
                        tol = tolerance.toDoubleOrNull(),
                        explanation = explanation.trim().ifEmpty { null },
                        memoryTip = memoryTip.trim().ifEmpty { null },
                        hints = hintsList,
                        img = imageUriOrPath,
                        targetBankId = selectedBankId,
                        alsoReverse = alsoReverse
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
