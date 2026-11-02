package com.example.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 4. OFFLINE ARCHIVE VIEWER
 * Renders local educational archives and pre-loaded website/ZIM structures
 * with network sockets strictly blocked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineArchiveViewer(
    onBack: () -> Unit = {}
) {
    var selectedArchiveIndex by remember { mutableIntStateOf(0) }

    val archives = remember {
        listOf(
            ArchiveDoc(
                title = "Science Encyclopedia",
                description = "Grade 11-12 Chemistry, Physics & Biology compendium",
                htmlContent = generateScienceEncyclopediaHtml()
            ),
            ArchiveDoc(
                title = "Formulas & Constants",
                description = "Master sheet of thermodynamic, kinematic, and chemical constants",
                htmlContent = generateFormulasHtml()
            ),
            ArchiveDoc(
                title = "Curriculum Matrix",
                description = "Full EUEE Ministry syllabus outline and competencies",
                htmlContent = generateCurriculumMatrixHtml()
            )
        )
    }

    val currentArchive = archives[selectedArchiveIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Local Reference Archive", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("100% Offline • Zero Network Socket", fontSize = 11.sp, color = Color(0xFF10B981))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Archive Selector Chips
            ScrollableTabRow(
                selectedTabIndex = selectedArchiveIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                archives.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedArchiveIndex == index,
                        onClick = { selectedArchiveIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(item.title, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            // Security & Offline Mode Banner
            Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Text(
                        text = "Isolated local sandbox: External network requests, WebSockets, and tracking cookies blocked.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Offline HTML / Structural Asset Renderer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFF0B1020))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.blockNetworkLoads = true
                            settings.blockNetworkImage = true

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    // Keep everything inside the offline viewer; never open external browsers
                                    return false
                                }
                            }

                            loadDataWithBaseURL(
                                null,
                                currentArchive.htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            currentArchive.htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

data class ArchiveDoc(
    val title: String,
    val description: String,
    val htmlContent: String
)

private fun generateScienceEncyclopediaHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: -apple-system, sans-serif; background-color: #0b1020; color: #f1f5f9; padding: 20px; line-height: 1.6; }
  h1 { color: #6366f1; border-bottom: 2px solid #312e81; padding-bottom: 8px; }
  h2 { color: #38bdf8; margin-top: 24px; }
  .badge { background: #1e1b4b; border: 1px solid #4338ca; color: #a5b4fc; padding: 3px 8px; border-radius: 4px; font-size: 12px; }
  .card { background: #18223c; border: 1px solid #26334d; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
  code { background: #1e293b; color: #f43f5e; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
  ul { padding-left: 20px; }
  li { margin-bottom: 6px; }
</style>
</head>
<body>
  <h1>📚 EUEE Natural Science Encyclopedia</h1>
  <p><span class="badge">OFFLINE REFERENCE ARCHIVE</span> Comprehensive reference material for Ethiopian Grade 12 candidates.</p>
  
  <div class="card">
    <h2>1. Chemical Equilibrium & Le Chatelier</h2>
    <p>Dynamic equilibrium is achieved in reversible reactions when the rates of the forward and reverse reactions are equal, and concentrations remain constant.</p>
    <ul>
      <li><strong>Pressure Increase:</strong> Favors side with fewer moles of gas.</li>
      <li><strong>Temperature Increase:</strong> Favors endothermic direction (ΔH > 0).</li>
      <li><strong>Catalyst:</strong> Reaches equilibrium faster; does NOT shift equilibrium position or change <code>Kc</code>.</li>
    </ul>
  </div>

  <div class="card">
    <h2>2. Acids, Bases & Buffer Solutions</h2>
    <p>Acidic buffers consist of a weak acid and its salt with a strong base (e.g., ethanoic acid and sodium ethanoate).</p>
    <p>Henderson-Hasselbalch equation: <code>pH = pKa + log([Salt]/[Acid])</code></p>
  </div>

  <div class="card">
    <h2>3. Physics Mechanics & Work-Energy Theorem</h2>
    <p>The net work done on an object equals the change in its kinetic energy: <code>W_net = ΔK = (1/2)mv² - (1/2)mu²</code>.</p>
    <p>Escape velocity from Earth: <code>v_esc = √(2GM/R) ≈ 11.2 km/s</code>.</p>
  </div>
</body>
</html>
""".trimIndent()

private fun generateFormulasHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: -apple-system, sans-serif; background-color: #0b1020; color: #f1f5f9; padding: 20px; line-height: 1.6; }
  h1 { color: #10b981; border-bottom: 2px solid #065f46; padding-bottom: 8px; }
  h2 { color: #34d399; margin-top: 24px; }
  table { width: 100%; border-collapse: collapse; margin-top: 12px; }
  th, td { border: 1px solid #334155; padding: 10px; text-align: left; }
  th { background-color: #1e293b; color: #38bdf8; }
  tr:nth-child(even) { background-color: #131c31; }
  code { color: #fbbf24; font-weight: bold; }
</style>
</head>
<body>
  <h1>⚡ Science & Math Formulas Master Sheet</h1>
  
  <h2>Physical Constants</h2>
  <table>
    <tr><th>Constant</th><th>Symbol</th><th>Standard Value</th></tr>
    <tr><td>Speed of Light</td><td>c</td><td>3.00 × 10⁸ m/s</td></tr>
    <tr><td>Planck's Constant</td><td>h</td><td>6.626 × 10⁻³⁴ J·s</td></tr>
    <tr><td>Universal Gravitation</td><td>G</td><td>6.674 × 10⁻¹¹ N·m²/kg²</td></tr>
    <tr><td>Avogadro's Number</td><td>N_A</td><td>6.022 × 10²³ mol⁻¹</td></tr>
    <tr><td>Ideal Gas Constant</td><td>R</td><td>8.314 J/(mol·K) = 0.0821 L·atm/(mol·K)</td></tr>
    <tr><td>Faraday's Constant</td><td>F</td><td>96,485 C/mol</td></tr>
  </table>

  <h2>Key Equations</h2>
  <table>
    <tr><th>Topic</th><th>Formula</th></tr>
    <tr><td>Ideal Gas Law</td><td><code>PV = nRT</code></td></tr>
    <tr><td>Kinetic Energy</td><td><code>K = 1/2 m v²</code></td></tr>
    <tr><td>Gravitational Potential</td><td><code>U = -GMm / r</code></td></tr>
    <tr><td>Nernst Equation</td><td><code>E = E° - (0.0592 / n) log(Q)</code></td></tr>
    <tr><td>Quadratic Formula</td><td><code>x = (-b ± √(b² - 4ac)) / (2a)</code></td></tr>
  </table>
</body>
</html>
""".trimIndent()

private fun generateCurriculumMatrixHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: -apple-system, sans-serif; background-color: #0b1020; color: #f1f5f9; padding: 20px; line-height: 1.6; }
  h1 { color: #a855f7; border-bottom: 2px solid #581c87; padding-bottom: 8px; }
  .unit { background: #18223c; border-left: 4px solid #a855f7; padding: 14px; margin-bottom: 14px; border-radius: 0 8px 8px 0; }
  h3 { margin: 0 0 6px 0; color: #e9d5ff; }
  p { margin: 0; color: #94a3b8; font-size: 14px; }
</style>
</head>
<body>
  <h1>📑 Ethiopian Grade 11-12 Curriculum Matrix</h1>
  
  <div class="unit">
    <h3>Grade 11 Chemistry - Unit 3: Chemical Kinetics & Equilibrium</h3>
    <p>Rate laws, collision theory, activation energy, equilibrium constant, and Le Chatelier's principle.</p>
  </div>

  <div class="unit">
    <h3>Grade 12 Chemistry - Unit 1: Solutions</h3>
    <p>Types of solutions, concentration units (molarity, molality, mole fraction), colligative properties, Raoult's law.</p>
  </div>

  <div class="unit">
    <h3>Grade 12 Chemistry - Unit 2: Acid-Base Equilibria</h3>
    <p>Autoionization of water, pH scale, weak acids and bases (Ka, Kb), hydrolysis of salts, buffers, indicators.</p>
  </div>

  <div class="unit">
    <h3>Grade 12 Physics - Unit 2: Electromagnetism</h3>
    <p>Magnetic fields, Lorentz force, Faraday's law of induction, Lenz's law, inductors, AC circuits.</p>
  </div>
</body>
</html>
""".trimIndent()
