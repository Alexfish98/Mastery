package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PeriodicTable
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceToolsScreen() {
    var selectedDomain by remember { mutableIntStateOf(0) }
    val domains = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English & Aptitude")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedDomain,
            edgePadding = 16.dp
        ) {
            domains.forEachIndexed { index, title ->
                Tab(
                    selected = selectedDomain == index,
                    onClick = { selectedDomain = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedDomain) {
            0 -> MathToolsTab()
            1 -> PhysicsToolsTab()
            2 -> ChemistryToolsTab()
            3 -> BiologyToolsTab()
            4 -> EnglishAptitudeToolsTab()
        }
    }
}

// ==========================================
// 1. MATHEMATICS TOOLS TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathToolsTab() {
    var mathToolIndex by remember { mutableIntStateOf(0) }
    val toolNames = listOf("Scientific Calc", "Quadratic Solver", "2x2 Linear System", "Matrix Determinant", "Sequences", "Statistics", "Geometry")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(selectedTabIndex = mathToolIndex, edgePadding = 12.dp) {
            toolNames.forEachIndexed { idx, name ->
                Tab(
                    selected = mathToolIndex == idx,
                    onClick = { mathToolIndex = idx },
                    text = { Text(name, fontSize = 12.sp) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (mathToolIndex) {
                0 -> ScientificCalculatorSubTab()
                1 -> QuadraticSolverSubTab()
                2 -> LinearSystemSubTab()
                3 -> MatrixDeterminantSubTab()
                4 -> SequencesSubTab()
                5 -> StatisticsSubTab()
                6 -> GeometrySubTab()
            }
        }
    }
}

@Composable
fun ScientificCalculatorSubTab() {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var isRad by remember { mutableStateOf(false) }

    fun evaluate(input: String): String {
        return try {
            val sanitized = input.trim()
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", Math.PI.toString())
                .replace("e", Math.E.toString())

            if (sanitized.isEmpty()) return "0"

            val tokens = sanitized.split("(?<=[-+*/^()])|(?=[-+*/^()])".toRegex()).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return "0"

            // Simple expression evaluator
            var current = 0.0
            var op = "+"
            var i = 0
            while (i < tokens.size) {
                val tok = tokens[i]
                if (tok in listOf("+", "-", "*", "/", "^")) {
                    op = tok
                } else {
                    var v = tok.toDoubleOrNull() ?: 0.0
                    when (op) {
                        "+" -> current += v
                        "-" -> current -= v
                        "*" -> current *= v
                        "/" -> if (v != 0.0) current /= v else return "Division by zero"
                        "^" -> current = current.pow(v)
                    }
                }
                i++
            }
            if (current.isNaN() || current.isInfinite()) "Error"
            else if (current == current.toLong().toDouble()) current.toLong().toString()
            else String.format(Locale.US, "%.6f", current).trimEnd('0').trimEnd('.')
        } catch (e: Exception) {
            "Error"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (expr.isEmpty()) "0" else expr,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "= $result",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isRad,
                    onClick = { isRad = !isRad },
                    label = { Text(if (isRad) "RAD" else "DEG") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = {
                        val num = result.toDoubleOrNull() ?: expr.toDoubleOrNull() ?: 0.0
                        val angle = if (isRad) num else Math.toRadians(num)
                        result = String.format(Locale.US, "%.4f", sin(angle))
                    }) { Text("sin") }

                    FilledTonalButton(onClick = {
                        val num = result.toDoubleOrNull() ?: expr.toDoubleOrNull() ?: 0.0
                        val angle = if (isRad) num else Math.toRadians(num)
                        result = String.format(Locale.US, "%.4f", cos(angle))
                    }) { Text("cos") }

                    FilledTonalButton(onClick = {
                        val num = result.toDoubleOrNull() ?: expr.toDoubleOrNull() ?: 0.0
                        val angle = if (isRad) num else Math.toRadians(num)
                        result = if (cos(angle) != 0.0) String.format(Locale.US, "%.4f", tan(angle)) else "Undefined"
                    }) { Text("tan") }

                    FilledTonalButton(onClick = {
                        val num = result.toDoubleOrNull() ?: expr.toDoubleOrNull() ?: 0.0
                        result = if (num >= 0) String.format(Locale.US, "%.4f", sqrt(num)) else "Error"
                    }) { Text("√") }
                }
            }
        }

        // Keypad grid
        val buttons = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        items(buttons) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { btn ->
                    Button(
                        onClick = {
                            when (btn) {
                                "C" -> {
                                    expr = ""
                                    result = "0"
                                }
                                "⌫" -> {
                                    if (expr.isNotEmpty()) expr = expr.dropLast(1)
                                }
                                "=" -> {
                                    result = evaluate(expr)
                                }
                                else -> {
                                    expr += btn
                                    result = evaluate(expr)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (btn) {
                                "=" -> MaterialTheme.colorScheme.primary
                                "C", "⌫" -> MaterialTheme.colorScheme.errorContainer
                                "+", "-", "×", "÷" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = when (btn) {
                                "=" -> MaterialTheme.colorScheme.onPrimary
                                "C", "⌫" -> MaterialTheme.colorScheme.onErrorContainer
                                "+", "-", "×", "÷" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(btn, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuadraticSolverSubTab() {
    var aStr by remember { mutableStateOf("1") }
    var bStr by remember { mutableStateOf("-5") }
    var cStr by remember { mutableStateOf("6") }

    val a = aStr.toDoubleOrNull() ?: 1.0
    val b = bStr.toDoubleOrNull() ?: -5.0
    val c = cStr.toDoubleOrNull() ?: 6.0

    val discriminant = (b * b) - (4 * a * c)
    val vertexH = -b / (2 * a)
    val vertexK = a * vertexH * vertexH + b * vertexH + c

    val rootsResult = remember(a, b, c, discriminant) {
        if (a == 0.0) {
            "Not a quadratic equation (a = 0)."
        } else if (discriminant > 0) {
            val r1 = (-b + sqrt(discriminant)) / (2 * a)
            val r2 = (-b - sqrt(discriminant)) / (2 * a)
            String.format(Locale.US, "Two real distinct roots:\nx₁ = %.4f\nx₂ = %.4f", r1, r2)
        } else if (discriminant == 0.0) {
            val r = -b / (2 * a)
            String.format(Locale.US, "One real double root:\nx = %.4f", r)
        } else {
            val realPart = -b / (2 * a)
            val imagPart = sqrt(-discriminant) / (2 * a)
            String.format(Locale.US, "Two complex conjugate roots:\nx₁ = %.4f + %.4fi\nx₂ = %.4f - %.4fi", realPart, imagPart, realPart, imagPart)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Solve ax² + bx + c = 0", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Calculates discriminant (Δ), roots, and parabola vertex", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = aStr,
                    onValueChange = { aStr = it },
                    label = { Text("a") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bStr,
                    onValueChange = { bStr = it },
                    label = { Text("b") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cStr,
                    onValueChange = { cStr = it },
                    label = { Text("c") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Solution Breakdown", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Discriminant Δ = b² - 4ac = ${String.format(Locale.US, "%.4f", discriminant)}")
                    Text("Vertex (h, k) = (${String.format(Locale.US, "%.3f", vertexH)}, ${String.format(Locale.US, "%.3f", vertexK)})")
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Text(rootsResult, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun LinearSystemSubTab() {
    var a1 by remember { mutableStateOf("2") }
    var b1 by remember { mutableStateOf("3") }
    var c1 by remember { mutableStateOf("12") }

    var a2 by remember { mutableStateOf("5") }
    var b2 by remember { mutableStateOf("-2") }
    var c2 by remember { mutableStateOf("11") }

    val D = (a1.toDoubleOrNull() ?: 2.0) * (b2.toDoubleOrNull() ?: -2.0) - (b1.toDoubleOrNull() ?: 3.0) * (a2.toDoubleOrNull() ?: 5.0)
    val Dx = (c1.toDoubleOrNull() ?: 12.0) * (b2.toDoubleOrNull() ?: -2.0) - (b1.toDoubleOrNull() ?: 3.0) * (c2.toDoubleOrNull() ?: 11.0)
    val Dy = (a1.toDoubleOrNull() ?: 2.0) * (c2.toDoubleOrNull() ?: 11.0) - (c1.toDoubleOrNull() ?: 12.0) * (a2.toDoubleOrNull() ?: 5.0)

    val solution = remember(D, Dx, Dy) {
        if (D == 0.0) {
            if (Dx == 0.0 && Dy == 0.0) "Infinitely many solutions (dependent lines)."
            else "No solution (parallel inconsistent lines)."
        } else {
            val x = Dx / D
            val y = Dy / D
            String.format(Locale.US, "Unique Solution (Cramer's Rule):\nx = %.4f\ny = %.4f", x, y)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("System of 2 Linear Equations (Cramer's Rule)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("a₁x + b₁y = c₁\na₂x + b₂y = c₂", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Text("Equation 1:", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = a1, onValueChange = { a1 = it }, label = { Text("a₁") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = b1, onValueChange = { b1 = it }, label = { Text("b₁") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = c1, onValueChange = { c1 = it }, label = { Text("c₁") }, modifier = Modifier.weight(1f))
            }
        }

        item {
            Text("Equation 2:", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = a2, onValueChange = { a2 = it }, label = { Text("a₂") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = b2, onValueChange = { b2 = it }, label = { Text("b₂") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = c2, onValueChange = { c2 = it }, label = { Text("c₂") }, modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Determinants: D = $D, Dx = $Dx, Dy = $Dy")
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    Text(solution, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MatrixDeterminantSubTab() {
    var a by remember { mutableStateOf("1") }
    var b by remember { mutableStateOf("2") }
    var c by remember { mutableStateOf("3") }
    var d by remember { mutableStateOf("4") }

    val aVal = a.toDoubleOrNull() ?: 1.0
    val bVal = b.toDoubleOrNull() ?: 2.0
    val cVal = c.toDoubleOrNull() ?: 3.0
    val dVal = d.toDoubleOrNull() ?: 4.0

    val det = (aVal * dVal) - (bVal * cVal)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("2x2 Matrix Determinant & Inverse", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Matrix A = [[a, b], [c, d]]", fontFamily = FontFamily.Monospace)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("a (row 1, col 1)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("b (row 1, col 2)") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("c (row 2, col 1)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("d (row 2, col 2)") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Determinant det(A) = ad - bc = $det", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (det != 0.0) {
                        Text("Inverse A⁻¹ exists:")
                        Text(
                            String.format(Locale.US, "[[%.3f, %.3f],\n [%.3f, %.3f]]", dVal / det, -bVal / det, -cVal / det, aVal / det),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text("Matrix is singular (det = 0, no inverse).", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SequencesSubTab() {
    var a1Str by remember { mutableStateOf("3") }
    var diffRatioStr by remember { mutableStateOf("4") }
    var nStr by remember { mutableStateOf("10") }
    var isGeometric by remember { mutableStateOf(false) }

    val a1 = a1Str.toDoubleOrNull() ?: 3.0
    val dOrR = diffRatioStr.toDoubleOrNull() ?: 4.0
    val n = nStr.toIntOrNull() ?: 10

    val nthTerm = if (!isGeometric) a1 + (n - 1) * dOrR else a1 * dOrR.pow((n - 1).toDouble())
    val sumN = if (!isGeometric) (n / 2.0) * (2 * a1 + (n - 1) * dOrR) else {
        if (dOrR == 1.0) n * a1 else a1 * (1 - dOrR.pow(n.toDouble())) / (1 - dOrR)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Arithmetic & Geometric Progression", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isGeometric, onClick = { isGeometric = false }, label = { Text("Arithmetic (AP)") })
                FilterChip(selected = isGeometric, onClick = { isGeometric = true }, label = { Text("Geometric (GP)") })
            }
        }
        item {
            OutlinedTextField(value = a1Str, onValueChange = { a1Str = it }, label = { Text("First Term a₁") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = diffRatioStr,
                onValueChange = { diffRatioStr = it },
                label = { Text(if (!isGeometric) "Common Difference d" else "Common Ratio r") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = nStr, onValueChange = { nStr = it }, label = { Text("Target Term n") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculation Results (n = $n):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("n-th Term aₙ = ${String.format(Locale.US, "%.4f", nthTerm)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sum of first n terms Sₙ = ${String.format(Locale.US, "%.4f", sumN)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StatisticsSubTab() {
    var dataInput by remember { mutableStateOf("12, 15, 12, 18, 22, 25, 30, 22, 22") }

    val numbers = remember(dataInput) {
        dataInput.split("[,\\s]+".toRegex()).mapNotNull { it.trim().toDoubleOrNull() }
    }

    val mean = if (numbers.isNotEmpty()) numbers.average() else 0.0
    val sorted = numbers.sorted()
    val median = if (sorted.isEmpty()) 0.0 else {
        if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    }
    val variance = if (numbers.size > 1) {
        val sumSquares = numbers.sumOf { (it - mean).pow(2) }
        sumSquares / (numbers.size - 1)
    } else 0.0
    val stdDev = sqrt(variance)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Statistics & Data Analyzer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Enter numbers separated by comma or space:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = dataInput,
                onValueChange = { dataInput = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Count: ${numbers.size}")
                    Text("Mean (Average): ${String.format(Locale.US, "%.4f", mean)}")
                    Text("Median: ${String.format(Locale.US, "%.4f", median)}")
                    Text("Sample Variance (s²): ${String.format(Locale.US, "%.4f", variance)}")
                    Text("Sample Standard Deviation (s): ${String.format(Locale.US, "%.4f", stdDev)}", fontWeight = FontWeight.Bold)
                    Text("Min: ${if (numbers.isNotEmpty()) numbers.min() else 0} | Max: ${if (numbers.isNotEmpty()) numbers.max() else 0}")
                }
            }
        }
    }
}

@Composable
fun GeometrySubTab() {
    var radiusStr by remember { mutableStateOf("5") }
    var heightStr by remember { mutableStateOf("10") }

    val r = radiusStr.toDoubleOrNull() ?: 5.0
    val h = heightStr.toDoubleOrNull() ?: 10.0

    val circleArea = Math.PI * r * r
    val cylinderVol = Math.PI * r * r * h
    val sphereVol = (4.0 / 3.0) * Math.PI * r.pow(3)
    val coneVol = (1.0 / 3.0) * Math.PI * r * r * h

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Geometry Area & Volume Solver", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = radiusStr, onValueChange = { radiusStr = it }, label = { Text("Radius r") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = heightStr, onValueChange = { heightStr = it }, label = { Text("Height h") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Circle Area: ${String.format(Locale.US, "%.3f", circleArea)}")
                    Text("Circle Circumference: ${String.format(Locale.US, "%.3f", 2 * Math.PI * r)}")
                    HorizontalDivider()
                    Text("Cylinder Volume: ${String.format(Locale.US, "%.3f", cylinderVol)}")
                    Text("Sphere Volume: ${String.format(Locale.US, "%.3f", sphereVol)}")
                    Text("Cone Volume: ${String.format(Locale.US, "%.3f", coneVol)}")
                }
            }
        }
    }
}

// ==========================================
// 2. PHYSICS TOOLS TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsToolsTab() {
    var physIndex by remember { mutableIntStateOf(0) }
    val physTools = listOf("Kinematics", "Projectile Motion", "Newton's F=ma", "Electricity V=IR", "Waves v=fλ", "Constants")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(selectedTabIndex = physIndex, edgePadding = 12.dp) {
            physTools.forEachIndexed { idx, name ->
                Tab(selected = physIndex == idx, onClick = { physIndex = idx }, text = { Text(name, fontSize = 12.sp) })
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (physIndex) {
                0 -> KinematicsSubTab()
                1 -> ProjectileMotionSubTab()
                2 -> DynamicsSubTab()
                3 -> ElectricitySubTab()
                4 -> WavesSubTab()
                5 -> PhysicalConstantsSubTab()
            }
        }
    }
}

@Composable
fun KinematicsSubTab() {
    var uStr by remember { mutableStateOf("0") }
    var aStr by remember { mutableStateOf("9.8") }
    var tStr by remember { mutableStateOf("4") }

    val u = uStr.toDoubleOrNull() ?: 0.0
    val a = aStr.toDoubleOrNull() ?: 9.8
    val t = tStr.toDoubleOrNull() ?: 4.0

    val v = u + a * t
    val s = u * t + 0.5 * a * t * t

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Linear Kinematics (v = u + at, s = ut + ½at²)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = uStr, onValueChange = { uStr = it }, label = { Text("Initial velocity u (m/s)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = aStr, onValueChange = { aStr = it }, label = { Text("Acceleration a (m/s²)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = tStr, onValueChange = { tStr = it }, label = { Text("Time t (s)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Final Velocity v = ${String.format(Locale.US, "%.3f", v)} m/s", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Displacement s = ${String.format(Locale.US, "%.3f", s)} m", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ProjectileMotionSubTab() {
    var v0Str by remember { mutableStateOf("25") }
    var angleDegStr by remember { mutableStateOf("45") }
    val g = 9.8

    val v0 = v0Str.toDoubleOrNull() ?: 25.0
    val theta = Math.toRadians((angleDegStr.toDoubleOrNull() ?: 45.0).coerceIn(0.0, 90.0))

    val timeOfFlight = (2 * v0 * sin(theta)) / g
    val maxHeight = (v0 * v0 * sin(theta) * sin(theta)) / (2 * g)
    val horizontalRange = (v0 * v0 * sin(2 * theta)) / g

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Projectile Motion Calculator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = v0Str, onValueChange = { v0Str = it }, label = { Text("Launch Speed v₀ (m/s)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = angleDegStr, onValueChange = { angleDegStr = it }, label = { Text("Launch Angle θ (°)") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Time of Flight T = ${String.format(Locale.US, "%.3f", timeOfFlight)} s", fontWeight = FontWeight.Bold)
                    Text("Maximum Height H = ${String.format(Locale.US, "%.3f", maxHeight)} m", fontWeight = FontWeight.Bold)
                    Text("Horizontal Range R = ${String.format(Locale.US, "%.3f", horizontalRange)} m", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DynamicsSubTab() {
    var mStr by remember { mutableStateOf("15") }
    var aStr by remember { mutableStateOf("4") }
    var vStr by remember { mutableStateOf("10") }

    val m = mStr.toDoubleOrNull() ?: 15.0
    val a = aStr.toDoubleOrNull() ?: 4.0
    val v = vStr.toDoubleOrNull() ?: 10.0

    val force = m * a
    val momentum = m * v
    val kineticEnergy = 0.5 * m * v * v

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Newtonian Dynamics & Energy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = mStr, onValueChange = { mStr = it }, label = { Text("Mass m (kg)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = aStr, onValueChange = { aStr = it }, label = { Text("Accel a (m/s²)") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = vStr, onValueChange = { vStr = it }, label = { Text("Velocity v (m/s)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Net Force F = ma: ${String.format(Locale.US, "%.2f", force)} N", fontWeight = FontWeight.Bold)
                    Text("Momentum p = mv: ${String.format(Locale.US, "%.2f", momentum)} kg·m/s")
                    Text("Kinetic Energy KE = ½mv²: ${String.format(Locale.US, "%.2f", kineticEnergy)} J")
                }
            }
        }
    }
}

@Composable
fun ElectricitySubTab() {
    var vStr by remember { mutableStateOf("220") }
    var rStr by remember { mutableStateOf("44") }

    val v = vStr.toDoubleOrNull() ?: 220.0
    val r = rStr.toDoubleOrNull() ?: 44.0

    val current = if (r != 0.0) v / r else 0.0
    val power = v * current

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Ohm's Law & Electric Power (V = IR, P = VI)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vStr, onValueChange = { vStr = it }, label = { Text("Voltage V (Volts)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = rStr, onValueChange = { rStr = it }, label = { Text("Resistance R (Ohms)") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current I = V/R: ${String.format(Locale.US, "%.3f", current)} Amperes", fontWeight = FontWeight.Bold)
                    Text("Power P = VI: ${String.format(Locale.US, "%.2f", power)} Watts (W)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WavesSubTab() {
    var freqStr by remember { mutableStateOf("440") }
    var speedStr by remember { mutableStateOf("343") }

    val f = freqStr.toDoubleOrNull() ?: 440.0
    val v = speedStr.toDoubleOrNull() ?: 343.0
    val lambda = if (f != 0.0) v / f else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Wave Velocity, Frequency & Wavelength (v = fλ)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = freqStr, onValueChange = { freqStr = it }, label = { Text("Frequency f (Hz)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = speedStr, onValueChange = { speedStr = it }, label = { Text("Wave Speed v (m/s)") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wavelength λ = v/f: ${String.format(Locale.US, "%.4f", lambda)} meters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PhysicalConstantsSubTab() {
    val constants = listOf(
        Triple("Speed of light in vacuum (c)", "2.9979 × 10⁸ m/s", "Exact speed of electromagnetic radiation"),
        Triple("Gravitational constant (G)", "6.6743 × 10⁻¹¹ N·m²/kg²", "Newton's law of universal gravitation"),
        Triple("Planck's constant (h)", "6.6261 × 10⁻³⁴ J·s", "Quantum of electromagnetic action"),
        Triple("Elementary charge (e)", "1.6022 × 10⁻¹⁹ C", "Charge of a single proton or electron"),
        Triple("Avogadro's number (N_A)", "6.0221 × 10²³ mol⁻¹", "Number of particles per mole"),
        Triple("Boltzmann constant (k_B)", "1.3806 × 10⁻²³ J/K", "Relates temperature to kinetic energy"),
        Triple("Standard gravity (g)", "9.80665 m/s²", "Nominal Earth gravitational acceleration")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Fundamental Physical Constants", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        items(constants) { (name, value, desc) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(name, fontWeight = FontWeight.Bold)
                    Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// 3. CHEMISTRY TOOLS TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemistryToolsTab() {
    var chemIndex by remember { mutableIntStateOf(0) }
    val chemTools = listOf("Ideal Gas PV=nRT", "pH / pOH Solver", "Solution Dilution", "Molar Mass")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(selectedTabIndex = chemIndex, edgePadding = 12.dp) {
            chemTools.forEachIndexed { idx, name ->
                Tab(selected = chemIndex == idx, onClick = { chemIndex = idx }, text = { Text(name, fontSize = 12.sp) })
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (chemIndex) {
                0 -> IdealGasSubTab()
                1 -> PhCalcSubTab()
                2 -> DilutionSubTab()
                3 -> QuickMolarMassSubTab()
            }
        }
    }
}

@Composable
fun IdealGasSubTab() {
    var pStr by remember { mutableStateOf("1.0") }
    var vStr by remember { mutableStateOf("22.4") }
    var nStr by remember { mutableStateOf("1.0") }
    val rConst = 0.082057 // L·atm / (mol·K)

    val p = pStr.toDoubleOrNull() ?: 1.0
    val v = vStr.toDoubleOrNull() ?: 22.4
    val n = nStr.toDoubleOrNull() ?: 1.0

    val tempK = if (n > 0) (p * v) / (n * rConst) else 0.0
    val tempC = tempK - 273.15

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Ideal Gas Law (PV = nRT)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Using R = 0.08206 L·atm/(mol·K)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = pStr, onValueChange = { pStr = it }, label = { Text("Pressure P (atm)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = vStr, onValueChange = { vStr = it }, label = { Text("Volume V (L)") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = nStr, onValueChange = { nStr = it }, label = { Text("Moles n (mol)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Calculated Temperature:", fontWeight = FontWeight.Bold)
                    Text("T = ${String.format(Locale.US, "%.2f", tempK)} Kelvin (K)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("T = ${String.format(Locale.US, "%.2f", tempC)} °Celsius", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PhCalcSubTab() {
    var phStr by remember { mutableStateOf("7.0") }
    val ph = (phStr.toDoubleOrNull() ?: 7.0).coerceIn(0.0, 14.0)

    val poh = 14.0 - ph
    val hConc = 10.0.pow(-ph)
    val ohConc = 10.0.pow(-poh)

    val nature = when {
        ph < 6.8 -> "Acidic (Excess H⁺/H₃O⁺ ions)"
        ph > 7.2 -> "Basic / Alkaline (Excess OH⁻ ions)"
        else -> "Neutral (Equal H⁺ and OH⁻)"
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("pH and pOH Acid-Base Calculator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = phStr,
                onValueChange = { phStr = it },
                label = { Text("Enter pH (0.0 to 14.0)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nature: $nature", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("pOH = 14 - pH = ${String.format(Locale.US, "%.2f", poh)}")
                    Text("Hydronium [H⁺] = ${String.format(Locale.US, "%.2e", hConc)} M", fontFamily = FontFamily.Monospace)
                    Text("Hydroxide [OH⁻] = ${String.format(Locale.US, "%.2e", ohConc)} M", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun DilutionSubTab() {
    var m1Str by remember { mutableStateOf("2.0") }
    var v1Str by remember { mutableStateOf("50") }
    var v2Str by remember { mutableStateOf("200") }

    val m1 = m1Str.toDoubleOrNull() ?: 2.0
    val v1 = v1Str.toDoubleOrNull() ?: 50.0
    val v2 = v2Str.toDoubleOrNull() ?: 200.0

    val m2 = if (v2 > 0) (m1 * v1) / v2 else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Solution Dilution Equation (M₁V₁ = M₂V₂)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = m1Str, onValueChange = { m1Str = it }, label = { Text("Stock Molarity M₁ (M)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = v1Str, onValueChange = { v1Str = it }, label = { Text("Stock Volume V₁ (mL)") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = v2Str, onValueChange = { v2Str = it }, label = { Text("Final Diluted Volume V₂ (mL)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diluted Concentration M₂ = ${String.format(Locale.US, "%.4f", m2)} M", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun QuickMolarMassSubTab() {
    var formula by remember { mutableStateOf("H2SO4") }

    val molarMass = remember(formula) {
        runCatching { PeriodicTable.calculateMolarMass(formula.trim()) }.getOrNull()
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Molecular Weight & Molar Mass", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = formula,
                onValueChange = { formula = it },
                label = { Text("Chemical Formula (e.g. H2SO4, C6H12O6, NaCl)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (molarMass != null) {
                        Text("Total Molar Mass: ${String.format(Locale.US, "%.3f", molarMass.totalMass)} g/mol", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        molarMass.composition.forEach { (sym, count) ->
                            val elem = PeriodicTable.getElement(sym)
                            val pct = molarMass.massFractions[sym] ?: 0.0
                            Text("${elem?.name ?: sym} ($sym): $count atoms · ${String.format(Locale.US, "%.1f", pct)}% mass")
                        }
                    } else {
                        Text("Enter a valid chemical formula (e.g. H2SO4, NaCl, C6H12O6).", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. BIOLOGY TOOLS TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiologyToolsTab() {
    var bioIndex by remember { mutableIntStateOf(0) }
    val bioTools = listOf("Punnett Square", "Codon Translator", "Cell Organelles", "Taxonomy Explorer")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(selectedTabIndex = bioIndex, edgePadding = 12.dp) {
            bioTools.forEachIndexed { idx, name ->
                Tab(selected = bioIndex == idx, onClick = { bioIndex = idx }, text = { Text(name, fontSize = 12.sp) })
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (bioIndex) {
                0 -> PunnettSquareSubTab()
                1 -> CodonTranslatorSubTab()
                2 -> CellOrganellesSubTab()
                3 -> TaxonomySubTab()
            }
        }
    }
}

@Composable
fun PunnettSquareSubTab() {
    var p1Allele1 by remember { mutableStateOf("B") }
    var p1Allele2 by remember { mutableStateOf("b") }
    var p2Allele1 by remember { mutableStateOf("B") }
    var p2Allele2 by remember { mutableStateOf("b") }

    val c1 = p1Allele1 + p2Allele1
    val c2 = p1Allele1 + p2Allele2
    val c3 = p1Allele2 + p2Allele1
    val c4 = p1Allele2 + p2Allele2

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Monohybrid Punnett Square (2x2)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Parent 1 Genotype: $p1Allele1$p1Allele2 × Parent 2 Genotype: $p2Allele1$p2Allele2")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = p1Allele1, onValueChange = { p1Allele1 = it.take(1) }, label = { Text("P1 Allele A") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = p1Allele2, onValueChange = { p1Allele2 = it.take(1) }, label = { Text("P1 Allele B") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = p2Allele1, onValueChange = { p2Allele1 = it.take(1) }, label = { Text("P2 Allele A") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = p2Allele2, onValueChange = { p2Allele2 = it.take(1) }, label = { Text("P2 Allele B") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Offspring Genotypes:", fontWeight = FontWeight.Bold)
                    Text("Top-Left: $c1 | Top-Right: $c2")
                    Text("Bottom-Left: $c3 | Bottom-Right: $c4")
                    Spacer(Modifier.height(8.dp))
                    Text("Expected standard ratio: 1 Homoz. Dom : 2 Heteroz : 1 Homoz. Rec (3:1 Phenotype)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun CodonTranslatorSubTab() {
    var mrnaSeq by remember { mutableStateOf("AUGUUUCCUAGU") }

    val codonMap = mapOf(
        "AUG" to "Methionine (START)", "UUU" to "Phenylalanine", "UUC" to "Phenylalanine",
        "UUA" to "Leucine", "UUG" to "Leucine", "CUU" to "Leucine",
        "CCU" to "Proline", "CCC" to "Proline", "CCA" to "Proline",
        "AGU" to "Serine", "AGC" to "Serine", "AGA" to "Arginine",
        "UAA" to "STOP", "UAG" to "STOP", "UGA" to "STOP"
    )

    val cleanSeq = mrnaSeq.uppercase().replace("T", "U").filter { it in "ACGU" }
    val codons = cleanSeq.chunked(3).filter { it.length == 3 }
    val aminoAcids = codons.map { codonMap[it] ?: "Other Amino Acid ($it)" }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("mRNA Codon to Amino Acid Translator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = mrnaSeq,
                onValueChange = { mrnaSeq = it },
                label = { Text("Enter mRNA Sequence (A, C, G, U)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Translated Polypeptide Chain:", fontWeight = FontWeight.Bold)
                    if (aminoAcids.isEmpty()) {
                        Text("Enter at least 3 RNA bases.")
                    } else {
                        aminoAcids.forEachIndexed { i, aa ->
                            Text("${i + 1}. Codon ${codons[i]} ➔ $aa", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CellOrganellesSubTab() {
    val organelles = listOf(
        Pair("Mitochondria", "Powerhouse of eukaryotic cells; site of aerobic cellular respiration & ATP synthesis via Krebs cycle and electron transport."),
        Pair("Ribosomes", "Complexes of rRNA and protein; site of protein synthesis (translation) found free in cytoplasm or on rough ER."),
        Pair("Endoplasmic Reticulum (ER)", "Rough ER with ribosomes modifies proteins; Smooth ER synthesizes lipids and detoxifies chemicals."),
        Pair("Golgi Apparatus", "Flattened membranous sacs that modify, sort, and package proteins and lipids into vesicles for secretion."),
        Pair("Chloroplasts", "Found in plant cells and algae; site of photosynthesis converting solar energy into glucose via chlorophyll."),
        Pair("Lysosomes", "Membrane-bound digestive vesicles containing acid hydrolytic enzymes for intracellular digestion.")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Cell Organelles & Ultrastructure", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        items(organelles) { (name, desc) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TaxonomySubTab() {
    val ranks = listOf(
        Pair("Domain", "Highest taxonomic rank: Archaea, Bacteria, Eukarya"),
        Pair("Kingdom", "e.g. Animalia, Plantae, Fungi, Protista"),
        Pair("Phylum", "e.g. Chordata (possessing notochord/backbone)"),
        Pair("Class", "e.g. Mammalia (warm-blooded, mammary glands)"),
        Pair("Order", "e.g. Primates (binocular vision, grasping digits)"),
        Pair("Family", "e.g. Hominidae (great apes)"),
        Pair("Genus", "e.g. Homo"),
        Pair("Species", "e.g. Homo sapiens")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Biological Taxonomic Hierarchy (Linnaean)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Mnemonic: 'Dear King Philip Came Over For Good Soup'", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        items(ranks) { (rank, example) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(rank.take(2).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(rank, fontWeight = FontWeight.Bold)
                        Text(example, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ENGLISH & APTITUDE TOOLS TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishAptitudeToolsTab() {
    var toolIndex by remember { mutableIntStateOf(0) }
    val tools = listOf("Vocab Trainer", "Number Series", "Grammar Guide", "Reading Speed")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(selectedTabIndex = toolIndex, edgePadding = 12.dp) {
            tools.forEachIndexed { idx, name ->
                Tab(selected = toolIndex == idx, onClick = { toolIndex = idx }, text = { Text(name, fontSize = 12.sp) })
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (toolIndex) {
                0 -> VocabTrainerSubTab()
                1 -> NumberSeriesSubTab()
                2 -> GrammarGuideSubTab()
                3 -> ReadingSpeedSubTab()
            }
        }
    }
}

@Composable
fun VocabTrainerSubTab() {
    val words = listOf(
        Triple("Ubiquitous", "Present, appearing, or found everywhere", "Omnipresent, pervasive"),
        Triple("Pragmatic", "Dealing with things sensibly and realistically based on practical rather than theoretical considerations", "Practical, hardheaded"),
        Triple("Ephemeral", "Lasting for a very short time; transitory", "Fleeting, momentary"),
        Triple("Benevolent", "Well meaning and kindly; generous to others", "Kindhearted, charitable"),
        Triple("Alacrity", "Brisk and cheerful readiness; eagerness", "Willingness, promptness"),
        Triple("Superfluous", "Unnecessary, especially through being more than enough", "Redundant, surplus")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("EUEE High-Yield Academic Vocabulary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        items(words) { (word, def, syn) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(word, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(def, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Synonyms: $syn", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun NumberSeriesSubTab() {
    var inputSeq by remember { mutableStateOf("2, 4, 8, 16, 32") }

    val nextPrediction = remember(inputSeq) {
        val nums = inputSeq.split("[,\\s]+".toRegex()).mapNotNull { it.trim().toDoubleOrNull() }
        if (nums.size < 2) "Need at least 2 numbers."
        else {
            // Check geometric ratio
            val r0 = if (nums[0] != 0.0) nums[1] / nums[0] else 0.0
            val isGeo = nums.zipWithNext().all { (a, b) -> if (a != 0.0) abs((b / a) - r0) < 0.001 else false }
            // Check arithmetic diff
            val d0 = nums[1] - nums[0]
            val isArith = nums.zipWithNext().all { (a, b) -> abs((b - a) - d0) < 0.001 }

            if (isGeo) {
                val nextVal = nums.last() * r0
                "Geometric Series (Ratio = $r0) ➔ Next term is $nextVal"
            } else if (isArith) {
                val nextVal = nums.last() + d0
                "Arithmetic Series (Diff = $d0) ➔ Next term is $nextVal"
            } else {
                "Pattern complex or custom. Check differences."
            }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Aptitude Number Series Pattern Solver", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = inputSeq,
                onValueChange = { inputSeq = it },
                label = { Text("Enter series (e.g. 2, 4, 8, 16)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Analyzed Pattern:", fontWeight = FontWeight.Bold)
                    Text(nextPrediction, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun GrammarGuideSubTab() {
    val rules = listOf(
        Pair("Third Conditional (Past Unreal)", "If + past perfect, would have + past participle.\nExample: If I had studied earlier, I would have scored higher."),
        Pair("Passive Voice Transformation", "Subject + verb + object ➔ Object + appropriate form of 'be' + past participle + (by subject).\nExample: 'The lab technician balanc-ed the equation' ➔ 'The equation was balanced by the lab technician.'"),
        Pair("Reported Speech Shift", "Direct: 'I am taking the exam now.' ➔ Reported: He said that he was taking the exam then.")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("EUEE English Grammar Essentials", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        items(rules) { (title, explanation) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(explanation, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ReadingSpeedSubTab() {
    var text by remember { mutableStateOf("The Ethiopian University Entrance Examination is a national standardized test assessing secondary school graduates across core sciences, mathematics, and humanities.") }

    val wordCount = remember(text) { text.split("\\s+".toRegex()).count { it.isNotBlank() } }
    val charCount = text.length
    val readingTimeSeconds = (wordCount.toDouble() / 200.0 * 60).toInt()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Reading Speed & Text Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Paste passage text here") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Words: $wordCount | Characters: $charCount", fontWeight = FontWeight.Bold)
                    Text("Estimated Reading Time (at 200 WPM): ~$readingTimeSeconds seconds")
                }
            }
        }
    }
}
