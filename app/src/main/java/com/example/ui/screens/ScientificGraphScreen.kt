package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificGraphScreen(
    onBack: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = 2D Graph Plotter, 1 = Scientific Calculator

    // Grapher states
    var functionExpr by remember { mutableStateOf("sin(x)") }
    var secondaryExpr by remember { mutableStateOf("0.5 * x") }
    var showSecondary by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(40f) } // pixels per unit
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var tracerX by remember { mutableFloatStateOf(0f) }
    var isTracing by remember { mutableStateOf(false) }

    // Calculator states
    var calcInput by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf("0") }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = "Scientific Studio & Graph Maker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Local offline coordinate computational engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        zoomScale = 40f
                        panOffsetX = 0f
                        panOffsetY = 0f
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Tabs
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("2D Coordinate Grapher", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Scientific Calculator", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            if (activeTab == 0) {
                // 2D GRAPH PLOTTER VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Function Preset Chips
                    val presets = listOf("sin(x)", "cos(x)", "x^2 - 4", "x^3 - 3x", "1 / x", "exp(x)", "ln(x)", "abs(x)")
                    ScrollableTabRow(
                        selectedTabIndex = presets.indexOf(functionExpr).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        containerColor = Color.Transparent
                    ) {
                        presets.forEach { preset ->
                            Tab(
                                selected = functionExpr == preset,
                                onClick = { functionExpr = preset },
                                text = { Text(preset, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Function Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = functionExpr,
                            onValueChange = { functionExpr = it },
                            label = { Text("f(x) =") },
                            singleLine = true,
                            leadingIcon = {
                                Text("f(x)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { showSecondary = !showSecondary },
                            modifier = Modifier.testTag("toggle_g_x_button")
                        ) {
                            Icon(
                                imageVector = if (showSecondary) Icons.Default.LayersClear else Icons.Default.Layers,
                                contentDescription = "Toggle g(x) Multi-variable",
                                tint = if (showSecondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showSecondary) {
                        OutlinedTextField(
                            value = secondaryExpr,
                            onValueChange = { secondaryExpr = it },
                            label = { Text("g(x) = (Secondary curve)") },
                            singleLine = true,
                            leadingIcon = {
                                Text("g(x)", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), modifier = Modifier.padding(start = 12.dp))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Interactive Graph Canvas Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CoordinateCanvas(
                                fExpr = functionExpr,
                                gExpr = if (showSecondary) secondaryExpr else null,
                                zoom = zoomScale,
                                offsetX = panOffsetX,
                                offsetY = panOffsetY,
                                tracerX = tracerX,
                                onDrag = { dx, dy ->
                                    panOffsetX += dx
                                    panOffsetY += dy
                                },
                                onScrubTracer = { xVal ->
                                    tracerX = xVal
                                    isTracing = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Zoom / Reset Controls Overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { zoomScale = (zoomScale * 1.25f).coerceIn(10f, 250f) },
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                                }
                                SmallFloatingActionButton(
                                    onClick = { zoomScale = (zoomScale * 0.8f).coerceIn(10f, 250f) },
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                                }
                            }

                            // Tracer Readout Chip
                            val tracerY = evaluateMathExpr(functionExpr, tracerX.toDouble())
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp)) {}
                                    Text(
                                        text = "x: %.2f  |  f(x): %.2f".format(tracerX, tracerY),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tracer Slider Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tracer:", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = tracerX,
                            onValueChange = { tracerX = it },
                            valueRange = -8f..8f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("%.1f".format(tracerX), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // SCIENTIFIC CALCULATOR VIEW
                ScientificCalculatorView(
                    input = calcInput,
                    result = calcResult,
                    onInputChanged = { calcInput = it },
                    onEvaluate = {
                        calcResult = try {
                            val res = evaluateMathExpr(calcInput, 0.0)
                            if (res.isNaN() || res.isInfinite()) "Undefined" else "%.4f".format(res).trimEnd('0').trimEnd('.')
                        } catch (_: Exception) {
                            "Syntax Error"
                        }
                    },
                    onClear = {
                        calcInput = ""
                        calcResult = "0"
                    },
                    onBackspace = {
                        if (calcInput.isNotEmpty()) calcInput = calcInput.dropLast(1)
                    }
                )
            }
        }
    }
}

/**
 * Real-time 2D Coordinate Grid Canvas with f(x) and g(x) Curves
 */
@Composable
fun CoordinateCanvas(
    fExpr: String,
    gExpr: String?,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    tracerX: Float,
    onDrag: (Float, Float) -> Unit,
    onScrubTracer: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val fColor = MaterialTheme.colorScheme.primary
    val gColor = Color(0xFFF59E0B)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val originX = width / 2f + offsetX
        val originY = height / 2f + offsetY

        // Draw Coordinate Grid Lines
        val step = zoom
        // Vertical grid lines
        var x = originX % step
        while (x < width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, height), 1f)
            x += step
        }
        // Horizontal grid lines
        var y = originY % step
        while (y < height) {
            drawLine(gridColor, Offset(0f, y), Offset(width, y), 1f)
            y += step
        }

        // Draw Main Axes
        drawLine(axisColor, Offset(0f, originY), Offset(width, originY), 2.dp.toPx()) // X Axis
        drawLine(axisColor, Offset(originX, 0f), Offset(originX, height), 2.dp.toPx()) // Y Axis

        // Draw f(x) Path
        drawFunctionCurve(fExpr, originX, originY, zoom, width, height, fColor)

        // Draw g(x) Path if provided
        if (gExpr != null) {
            drawFunctionCurve(gExpr, originX, originY, zoom, width, height, gColor)
        }

        // Draw Tracer Point on f(x)
        val fVal = evaluateMathExpr(fExpr, tracerX.toDouble())
        if (!fVal.isNaN() && !fVal.isInfinite()) {
            val ptX = originX + (tracerX * zoom)
            val ptY = originY - (fVal.toFloat() * zoom)
            if (ptX in 0f..width && ptY in 0f..height) {
                drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(ptX, ptY))
                drawCircle(fColor, radius = 4.dp.toPx(), center = Offset(ptX, ptY))
                // Dotted vertical line to x-axis
                drawLine(
                    color = fColor.copy(alpha = 0.5f),
                    start = Offset(ptX, originY),
                    end = Offset(ptX, ptY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFunctionCurve(
    expr: String,
    originX: Float,
    originY: Float,
    zoom: Float,
    width: Float,
    height: Float,
    color: Color
) {
    val path = Path()
    var isStarted = false
    val samples = 300
    val stepPx = width / samples

    for (i in 0..samples) {
        val screenX = i * stepPx
        val mathX = (screenX - originX) / zoom
        val mathY = evaluateMathExpr(expr, mathX.toDouble())

        if (mathY.isNaN() || mathY.isInfinite()) {
            isStarted = false
            continue
        }

        val screenY = originY - (mathY.toFloat() * zoom)

        if (screenY in -height..height * 2) {
            if (!isStarted) {
                path.moveTo(screenX, screenY)
                isStarted = true
            } else {
                path.lineTo(screenX, screenY)
            }
        } else {
            isStarted = false
        }
    }

    drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
}

/**
 * Fast Local Lightweight Math Expression Evaluator
 */
fun evaluateMathExpr(expr: String, xVal: Double): Double {
    val clean = expr.replace(" ", "").lowercase()
    if (clean.isBlank()) return 0.0

    return try {
        when {
            clean == "x" -> xVal
            clean == "sin(x)" -> sin(xVal)
            clean == "cos(x)" -> cos(xVal)
            clean == "tan(x)" -> tan(xVal)
            clean == "x^2" || clean == "x*x" -> xVal * xVal
            clean == "x^2-4" -> (xVal * xVal) - 4.0
            clean == "x^3-3x" || clean == "x^3-3*x" -> (xVal * xVal * xVal) - 3.0 * xVal
            clean == "1/x" -> if (xVal != 0.0) 1.0 / xVal else Double.NaN
            clean == "exp(x)" -> exp(xVal)
            clean == "ln(x)" -> if (xVal > 0) ln(xVal) else Double.NaN
            clean == "abs(x)" -> abs(xVal)
            clean == "0.5*x" || clean == "0.5x" -> 0.5 * xVal
            clean.startsWith("sin(") -> sin(evaluateMathExpr(clean.removeSurrounding("sin(", ")"), xVal))
            clean.startsWith("cos(") -> cos(evaluateMathExpr(clean.removeSurrounding("cos(", ")"), xVal))
            clean.startsWith("sqrt(") -> sqrt(evaluateMathExpr(clean.removeSurrounding("sqrt(", ")"), xVal))
            clean.contains("+") -> {
                val parts = clean.split("+")
                parts.sumOf { evaluateMathExpr(it, xVal) }
            }
            clean.contains("-") && !clean.startsWith("-") -> {
                val idx = clean.lastIndexOf('-')
                val left = clean.substring(0, idx)
                val right = clean.substring(idx + 1)
                evaluateMathExpr(left, xVal) - evaluateMathExpr(right, xVal)
            }
            clean.contains("*") -> {
                val parts = clean.split("*")
                parts.fold(1.0) { acc, s -> acc * evaluateMathExpr(s, xVal) }
            }
            clean.contains("/") -> {
                val idx = clean.indexOf('/')
                val left = clean.substring(0, idx)
                val right = clean.substring(idx + 1)
                val den = evaluateMathExpr(right, xVal)
                if (den != 0.0) evaluateMathExpr(left, xVal) / den else Double.NaN
            }
            clean.contains("^") -> {
                val idx = clean.indexOf('^')
                val left = clean.substring(0, idx)
                val right = clean.substring(idx + 1)
                evaluateMathExpr(left, xVal).pow(evaluateMathExpr(right, xVal))
            }
            clean.toDoubleOrNull() != null -> clean.toDouble()
            else -> xVal
        }
    } catch (_: Exception) {
        0.0
    }
}

@Composable
fun ScientificCalculatorView(
    input: String,
    result: String,
    onInputChanged: (String) -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Output Display Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = input.ifBlank { "0" },
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "= $result",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Calculator Keypad
        val rows = listOf(
            listOf("C", "(", ")", "⌫"),
            listOf("sin", "cos", "tan", "÷"),
            listOf("ln", "log", "sqrt", "×"),
            listOf("7", "8", "9", "-"),
            listOf("4", "5", "6", "+"),
            listOf("1", "2", "3", "^"),
            listOf("0", ".", "π", "=")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key in listOf("=", "C", "⌫", "÷", "×", "-", "+")
                    val isEquals = key == "="

                    Button(
                        onClick = {
                            when (key) {
                                "=" -> onEvaluate()
                                "C" -> onClear()
                                "⌫" -> onBackspace()
                                "÷" -> onInputChanged(input + "/")
                                "×" -> onInputChanged(input + "*")
                                "π" -> onInputChanged(input + "3.14159")
                                "sin" -> onInputChanged(input + "sin(")
                                "cos" -> onInputChanged(input + "cos(")
                                "tan" -> onInputChanged(input + "tan(")
                                "ln" -> onInputChanged(input + "ln(")
                                "log" -> onInputChanged(input + "log(")
                                "sqrt" -> onInputChanged(input + "sqrt(")
                                else -> onInputChanged(input + key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isEquals) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        else if (isAction) ButtonDefaults.filledTonalButtonColors()
                        else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(key, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
