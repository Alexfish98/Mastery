package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BalancedReactionResult
import com.example.data.ChemicalElement
import com.example.data.MolarMassResult
import com.example.data.PeriodicTable
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChemistryToolsScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Periodic Table", "Equation Balancer", "Molar Mass", "Solution Calc")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        when (selectedTab) {
            0 -> PeriodicTableTab()
            1 -> EquationBalancerTab()
            2 -> MolarMassTab()
            3 -> SolutionCalculatorTab()
        }
    }
}

@Composable
fun PeriodicTableTab() {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        val s = search.trim().lowercase()
        if (s.isEmpty()) PeriodicTable.elements else {
            PeriodicTable.elements.filter {
                it.symbol.lowercase().contains(s) || it.name.lowercase().contains(s) || it.z.toString() == s
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Periodic Table (All 118 Elements)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search by symbol, name, or atomic number Z...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(filtered, key = { it.z }) { elem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${elem.z}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = elem.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = elem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Standard atomic weight: ${elem.mass} u",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquationBalancerTab() {
    var rawInput by remember { mutableStateOf("H2 + O2 -> H2O") }
    var result by remember { mutableStateOf<BalancedReactionResult?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun runBalance() {
        errorMsg = null
        try {
            result = PeriodicTable.balanceEquation(rawInput)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Failed to balance equation"
            result = null
        }
    }

    LaunchedEffect(Unit) {
        runBalance()
    }

    val examples = listOf(
        "H2 + O2 -> H2O",
        "Fe + O2 -> Fe2O3",
        "Al + HCl -> AlCl3 + H2",
        "C3H8 + O2 -> CO2 + H2O",
        "Ca(OH)2 + HCl -> CaCl2 + H2O"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Chemical Equation Balancer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Balance neutral chemical reactions using conservation of mass. Write un-balanced formulas separated by -> or =.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = rawInput,
            onValueChange = {
                rawInput = it
                runBalance()
            },
            label = { Text("Chemical Reaction") },
            placeholder = { Text("e.g. Fe + O2 -> Fe2O3") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("equation_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { runBalance() }) {
                Text("Balance Equation")
            }
            OutlinedButton(onClick = {
                rawInput = ""
                result = null
                errorMsg = null
            }) {
                Text("Clear")
            }
        }

        // Quick Examples
        Text("Sample reactions:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            examples.forEach { ex ->
                SuggestionChip(
                    onClick = {
                        rawInput = ex
                        runBalance()
                    },
                    label = { Text(ex) }
                )
            }
        }

        errorMsg?.let { err ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        result?.let { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessGreenBg)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Balanced Equation:",
                        style = MaterialTheme.typography.labelLarge,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = res.equation,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessGreen
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Atom Conservation Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Element", fontWeight = FontWeight.Bold)
                        Text("Reactants", fontWeight = FontWeight.Bold)
                        Text("Products", fontWeight = FontWeight.Bold)
                        Text("Status", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    res.atomBalances.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.symbol, fontWeight = FontWeight.SemiBold)
                            Text("${row.reactantCount}")
                            Text("${row.productCount}")
                            Text(if (row.isBalanced) "✓ Balanced" else "✗ Check", color = if (row.isBalanced) SuccessGreen else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MolarMassTab() {
    var formula by remember { mutableStateOf("CuSO4·5H2O") }
    var result by remember { mutableStateOf<MolarMassResult?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        errorMsg = null
        try {
            result = PeriodicTable.calculateMolarMass(formula)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Invalid formula"
            result = null
        }
    }

    LaunchedEffect(Unit) {
        calculate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Molar Mass Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Computes molecular weight and mass percentages. Supports parentheses, square brackets, and hydrate dots (e.g. Ca(OH)2 or CuSO4·5H2O).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = formula,
            onValueChange = {
                formula = it
                calculate()
            },
            label = { Text("Formula") },
            placeholder = { Text("e.g. Al2(SO4)3 or Ca(OH)2") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMsg?.let { err ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        result?.let { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Total Molar Mass", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(Locale.US, "%.3f g/mol", res.totalMass),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elemental Composition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Element", fontWeight = FontWeight.Bold)
                        Text("Atoms", fontWeight = FontWeight.Bold)
                        Text("Mass %", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    res.composition.forEach { (elem, count) ->
                        val pct = res.massFractions[elem] ?: 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(elem, fontWeight = FontWeight.SemiBold)
                            Text("$count")
                            Text(String.format(Locale.US, "%.2f%%", pct), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolutionCalculatorTab() {
    var formula by remember { mutableStateOf("H2SO4") }
    var massGrams by remember { mutableStateOf("9.8") }
    var volumeMl by remember { mutableStateOf("250") }

    var molarMass by remember { mutableDoubleStateOf(0.0) }
    var moles by remember { mutableDoubleStateOf(0.0) }
    var molarity by remember { mutableDoubleStateOf(0.0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun calcSolution() {
        errorMsg = null
        try {
            val mm = PeriodicTable.calculateMolarMass(formula).totalMass
            val m = massGrams.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid mass in grams.")
            val v = volumeMl.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid volume in mL.")
            if (v <= 0) throw IllegalArgumentException("Volume must be greater than 0.")

            molarMass = mm
            moles = m / mm
            molarity = moles / (v / 1000.0)
        } catch (e: Exception) {
            errorMsg = e.message
            molarity = 0.0
        }
    }

    LaunchedEffect(formula, massGrams, volumeMl) {
        calcSolution()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Mass-to-Concentration Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Calculate molar concentration (mol/dm³ or M) from solute mass and final solution volume.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = formula,
            onValueChange = { formula = it },
            label = { Text("Solute formula") },
            placeholder = { Text("e.g. NaCl, H2SO4, NaOH") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = massGrams,
                onValueChange = { massGrams = it },
                label = { Text("Solute mass (g)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = volumeMl,
                onValueChange = { volumeMl = it },
                label = { Text("Final volume (mL)") },
                modifier = Modifier.weight(1f)
            )
        }

        errorMsg?.let { err ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (molarity > 0.0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Calculation Results", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Solute Molar Mass:")
                        Text(String.format(Locale.US, "%.3f g/mol", molarMass), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Amount of Substance:")
                        Text(String.format(Locale.US, "%.5f mol", moles), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Molar Concentration:")
                        Text(
                            text = String.format(Locale.US, "%.4f mol/dm³", molarity),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
