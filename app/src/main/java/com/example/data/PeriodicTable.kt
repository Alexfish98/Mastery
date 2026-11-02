package com.example.data

import kotlin.math.abs
import kotlin.math.roundToInt

data class ChemicalElement(
    val symbol: String,
    val name: String,
    val mass: Double,
    val z: Int
)

data class MolarMassResult(
    val totalMass: Double,
    val composition: Map<String, Int>,
    val massFractions: Map<String, Double>
)

data class BalancedReactionResult(
    val equation: String,
    val reactants: List<String>,
    val products: List<String>,
    val coefficients: List<Int>,
    val atomBalances: List<AtomBalanceRow>
)

data class AtomBalanceRow(
    val symbol: String,
    val reactantCount: Int,
    val productCount: Int,
    val isBalanced: Boolean
)

object PeriodicTable {

    val elements: List<ChemicalElement> = listOf(
        ChemicalElement("H", "Hydrogen", 1.008, 1),
        ChemicalElement("He", "Helium", 4.0026, 2),
        ChemicalElement("Li", "Lithium", 6.94, 3),
        ChemicalElement("Be", "Beryllium", 9.0122, 4),
        ChemicalElement("B", "Boron", 10.81, 5),
        ChemicalElement("C", "Carbon", 12.011, 6),
        ChemicalElement("N", "Nitrogen", 14.007, 7),
        ChemicalElement("O", "Oxygen", 15.999, 8),
        ChemicalElement("F", "Fluorine", 18.998, 9),
        ChemicalElement("Ne", "Neon", 20.180, 10),
        ChemicalElement("Na", "Sodium", 22.990, 11),
        ChemicalElement("Mg", "Magnesium", 24.305, 12),
        ChemicalElement("Al", "Aluminium", 26.982, 13),
        ChemicalElement("Si", "Silicon", 28.085, 14),
        ChemicalElement("P", "Phosphorus", 30.974, 15),
        ChemicalElement("S", "Sulphur", 32.06, 16),
        ChemicalElement("Cl", "Chlorine", 35.45, 17),
        ChemicalElement("Ar", "Argon", 39.948, 18),
        ChemicalElement("K", "Potassium", 39.098, 19),
        ChemicalElement("Ca", "Calcium", 40.078, 20),
        ChemicalElement("Sc", "Scandium", 44.956, 21),
        ChemicalElement("Ti", "Titanium", 47.867, 22),
        ChemicalElement("V", "Vanadium", 50.942, 23),
        ChemicalElement("Cr", "Chromium", 51.996, 24),
        ChemicalElement("Mn", "Manganese", 54.938, 25),
        ChemicalElement("Fe", "Iron", 55.845, 26),
        ChemicalElement("Co", "Cobalt", 58.933, 27),
        ChemicalElement("Ni", "Nickel", 58.693, 28),
        ChemicalElement("Cu", "Copper", 63.546, 29),
        ChemicalElement("Zn", "Zinc", 65.38, 30),
        ChemicalElement("Ga", "Gallium", 69.723, 31),
        ChemicalElement("Ge", "Germanium", 72.630, 32),
        ChemicalElement("As", "Arsenic", 74.922, 33),
        ChemicalElement("Se", "Selenium", 78.971, 34),
        ChemicalElement("Br", "Bromine", 79.904, 35),
        ChemicalElement("Kr", "Krypton", 83.798, 36),
        ChemicalElement("Rb", "Rubidium", 85.468, 37),
        ChemicalElement("Sr", "Strontium", 87.62, 38),
        ChemicalElement("Y", "Yttrium", 88.906, 39),
        ChemicalElement("Zr", "Zirconium", 91.224, 40),
        ChemicalElement("Nb", "Niobium", 92.906, 41),
        ChemicalElement("Mo", "Molybdenum", 95.95, 42),
        ChemicalElement("Tc", "Technetium", 98.0, 43),
        ChemicalElement("Ru", "Ruthenium", 101.07, 44),
        ChemicalElement("Rh", "Rhodium", 102.91, 45),
        ChemicalElement("Pd", "Palladium", 106.42, 46),
        ChemicalElement("Ag", "Silver", 107.8682, 47),
        ChemicalElement("Cd", "Cadmium", 112.414, 48),
        ChemicalElement("In", "Indium", 114.818, 49),
        ChemicalElement("Sn", "Tin", 118.710, 50),
        ChemicalElement("Sb", "Antimony", 121.760, 51),
        ChemicalElement("Te", "Tellurium", 127.60, 52),
        ChemicalElement("I", "Iodine", 126.90447, 53),
        ChemicalElement("Xe", "Xenon", 131.293, 54),
        ChemicalElement("Cs", "Caesium", 132.90545, 55),
        ChemicalElement("Ba", "Barium", 137.327, 56),
        ChemicalElement("La", "Lanthanum", 138.90547, 57),
        ChemicalElement("Ce", "Cerium", 140.116, 58),
        ChemicalElement("Pr", "Praseodymium", 140.90766, 59),
        ChemicalElement("Nd", "Neodymium", 144.242, 60),
        ChemicalElement("Pm", "Promethium", 145.0, 61),
        ChemicalElement("Sm", "Samarium", 150.36, 62),
        ChemicalElement("Eu", "Europium", 151.964, 63),
        ChemicalElement("Gd", "Gadolinium", 157.25, 64),
        ChemicalElement("Tb", "Terbium", 158.92535, 65),
        ChemicalElement("Dy", "Dysprosium", 162.500, 66),
        ChemicalElement("Ho", "Holmium", 164.93033, 67),
        ChemicalElement("Er", "Erbium", 167.259, 68),
        ChemicalElement("Tm", "Thulium", 168.93422, 69),
        ChemicalElement("Yb", "Ytterbium", 173.045, 70),
        ChemicalElement("Lu", "Lutetium", 174.9668, 71),
        ChemicalElement("Hf", "Hafnium", 178.49, 72),
        ChemicalElement("Ta", "Tantalum", 180.94788, 73),
        ChemicalElement("W", "Tungsten", 183.84, 74),
        ChemicalElement("Re", "Rhenium", 186.207, 75),
        ChemicalElement("Os", "Osmium", 190.23, 76),
        ChemicalElement("Ir", "Iridium", 192.217, 77),
        ChemicalElement("Pt", "Platinum", 195.084, 78),
        ChemicalElement("Au", "Gold", 196.96657, 79),
        ChemicalElement("Hg", "Mercury", 200.592, 80),
        ChemicalElement("Tl", "Thallium", 204.38, 81),
        ChemicalElement("Pb", "Lead", 207.2, 82),
        ChemicalElement("Bi", "Bismuth", 208.98040, 83),
        ChemicalElement("Po", "Polonium", 209.0, 84),
        ChemicalElement("At", "Astatine", 210.0, 85),
        ChemicalElement("Rn", "Radon", 222.0, 86),
        ChemicalElement("Fr", "Francium", 223.0, 87),
        ChemicalElement("Ra", "Radium", 226.0, 88),
        ChemicalElement("Ac", "Actinium", 227.0, 89),
        ChemicalElement("Th", "Thorium", 232.0377, 90),
        ChemicalElement("Pa", "Protactinium", 231.03588, 91),
        ChemicalElement("U", "Uranium", 238.02891, 92),
        ChemicalElement("Np", "Neptunium", 237.0, 93),
        ChemicalElement("Pu", "Plutonium", 244.0, 94),
        ChemicalElement("Am", "Americium", 243.0, 95),
        ChemicalElement("Cm", "Curium", 247.0, 96),
        ChemicalElement("Bk", "Berkelium", 247.0, 97),
        ChemicalElement("Cf", "Californium", 251.0, 98),
        ChemicalElement("Es", "Einsteinium", 252.0, 99),
        ChemicalElement("Fm", "Fermium", 257.0, 100),
        ChemicalElement("Md", "Mendelevium", 258.0, 101),
        ChemicalElement("No", "Nobelium", 259.0, 102),
        ChemicalElement("Lr", "Lawrencium", 266.0, 103),
        ChemicalElement("Rf", "Rutherfordium", 267.0, 104),
        ChemicalElement("Db", "Dubnium", 268.0, 105),
        ChemicalElement("Sg", "Seaborgium", 269.0, 106),
        ChemicalElement("Bh", "Bohrium", 270.0, 107),
        ChemicalElement("Hs", "Hassium", 277.0, 108),
        ChemicalElement("Mt", "Meitnerium", 278.0, 109),
        ChemicalElement("Ds", "Darmstadtium", 281.0, 110),
        ChemicalElement("Rg", "Roentgenium", 282.0, 111),
        ChemicalElement("Cn", "Copernicium", 285.0, 112),
        ChemicalElement("Nh", "Nihonium", 286.0, 113),
        ChemicalElement("Fl", "Flerovium", 289.0, 114),
        ChemicalElement("Mc", "Moscovium", 290.0, 115),
        ChemicalElement("Lv", "Livermorium", 293.0, 116),
        ChemicalElement("Ts", "Tennessine", 294.0, 117),
        ChemicalElement("Og", "Oganesson", 294.0, 118)
    )

    private val elementMap: Map<String, ChemicalElement> = elements.associateBy { it.symbol }

    fun getElement(symbol: String): ChemicalElement? = elementMap[symbol]

    /**
     * Parses chemical formula into element counts (e.g. Ca(OH)2, CuSO4·5H2O, 2Fe2O3)
     */
    fun parseFormula(rawFormula: String): Map<String, Int> {
        val trimmed = rawFormula.trim().replace("\\s+".toRegex(), "")
        if (trimmed.isEmpty()) throw IllegalArgumentException("Enter a chemical formula.")

        val parts = trimmed.split(Regex("[·⋅.]"))
        val totalCounts = mutableMapOf<String, Int>()

        for (part in parts) {
            if (part.isEmpty()) throw IllegalArgumentException("Invalid hydrate dot in formula.")

            // Check leading multiplier
            val leadMatch = Regex("^(\\d+)").find(part)
            val multiplier = leadMatch?.value?.toIntOrNull() ?: 1
            val body = if (leadMatch != null) part.substring(leadMatch.value.length) else part
            if (body.isEmpty()) throw IllegalArgumentException("Formula expected after multiplier.")

            var idx = 0
            fun parseGroup(expectedClose: Char?): Map<String, Int> {
                val groupCounts = mutableMapOf<String, Int>()
                var tokenCount = 0

                while (idx < body.length) {
                    val c = body[idx]
                    if (c == ')' || c == ']') {
                        if (expectedClose == null || c != expectedClose) {
                            throw IllegalArgumentException("Mismatched closing bracket '$c'.")
                        }
                        idx++
                        if (tokenCount == 0) throw IllegalArgumentException("Empty brackets in formula.")
                        return groupCounts
                    }

                    if (c == '(' || c == '[') {
                        idx++
                        val close = if (c == '(') ')' else ']'
                        val inner = parseGroup(close)
                        // Read subscript
                        val subStart = idx
                        while (idx < body.length && body[idx].isDigit()) idx++
                        val sub = if (subStart < idx) body.substring(subStart, idx).toInt() else 1

                        inner.forEach { (elem, count) ->
                            groupCounts[elem] = (groupCounts[elem] ?: 0) + count * sub
                        }
                        tokenCount++
                        continue
                    }

                    // Element symbol: 1 uppercase + optional lowercase
                    val symMatch = Regex("^[A-Z][a-z]?").find(body.substring(idx))
                        ?: throw IllegalArgumentException("Unexpected character '${body[idx]}'.")
                    val symbol = symMatch.value
                    idx += symbol.length

                    if (!elementMap.containsKey(symbol)) {
                        throw IllegalArgumentException("Unknown element symbol '$symbol'.")
                    }

                    // Read subscript
                    val subStart = idx
                    while (idx < body.length && body[idx].isDigit()) idx++
                    val sub = if (subStart < idx) body.substring(subStart, idx).toInt() else 1

                    groupCounts[symbol] = (groupCounts[symbol] ?: 0) + sub
                    tokenCount++
                }

                if (expectedClose != null) {
                    throw IllegalArgumentException("Unclosed bracket '$expectedClose'.")
                }
                if (tokenCount == 0) throw IllegalArgumentException("Enter at least one element.")
                return groupCounts
            }

            val partCounts = parseGroup(null)
            partCounts.forEach { (elem, count) ->
                totalCounts[elem] = (totalCounts[elem] ?: 0) + count * multiplier
            }
        }

        return totalCounts
    }

    /**
     * Calculates molar mass and composition percentages.
     */
    fun calculateMolarMass(formula: String): MolarMassResult {
        val counts = parseFormula(formula)
        var total = 0.0
        val massMap = mutableMapOf<String, Double>()

        counts.forEach { (sym, count) ->
            val elem = elementMap[sym] ?: throw IllegalArgumentException("Unknown element $sym")
            val m = elem.mass * count
            massMap[sym] = m
            total += m
        }

        val fractions = massMap.mapValues { (_, mass) ->
            if (total > 0) (mass / total) * 100.0 else 0.0
        }

        return MolarMassResult(totalMass = total, composition = counts, massFractions = fractions)
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = y
            y = x % y
            x = t
        }
        return if (x == 0L) 1L else x
    }

    private fun lcm(a: Long, b: Long): Long = (abs(a * b)) / gcd(a, b)

    private fun toFraction(x: Double): Pair<Long, Long> {
        for (den in 1..1000) {
            val num = (x * den).roundToInt()
            if (abs(x - num.toDouble() / den) < 1e-6) {
                return Pair(num.toLong(), den.toLong())
            }
        }
        return Pair((x * 1000000).toLong(), 1000000L)
    }

    /**
     * Balances chemical reactions such as:
     * "H2 + O2 -> H2O" or "Al + HCl -> AlCl3 + H2" or "C3H8 + O2 = CO2 + H2O"
     */
    fun balanceEquation(raw: String): BalancedReactionResult {
        val trimmed = raw.trim()
        val arrowMatch = Regex("^(.*?)\\s*(?:<=>|⇌|↔|->|=>|→|=)\\s*(.*?)$").find(trimmed)
            ?: throw IllegalArgumentException("Use an arrow (-> or =) between reactants and products, e.g. H2 + O2 -> H2O")

        val leftStr = arrowMatch.groupValues[1].trim()
        val rightStr = arrowMatch.groupValues[2].trim()

        if (leftStr.isEmpty() || rightStr.isEmpty()) {
            throw IllegalArgumentException("Equation must have both reactants and products.")
        }

        data class Species(val formula: String, val atoms: Map<String, Int>)

        fun parseSide(sideText: String): List<Species> {
            return sideText.split("+").map { term ->
                val compact = term.trim().replace("\\s+".toRegex(), "")
                val cleanFormula = compact.trimStart { it.isDigit() }
                if (cleanFormula.isEmpty()) throw IllegalArgumentException("Empty reaction term.")
                Species(cleanFormula, parseFormula(cleanFormula))
            }
        }

        val left = parseSide(leftStr)
        val right = parseSide(rightStr)
        val species = left + right
        val n = species.size

        if (left.isEmpty() || right.isEmpty() || n < 2 || n > 12) {
            throw IllegalArgumentException("Supported for reactions with between 2 and 12 total species.")
        }

        val symbols = species.flatMap { it.atoms.keys }.distinct().sortedBy { sym ->
            elementMap[sym]?.z ?: 999
        }

        // Build matrix where left side is +count, right side is -count
        val matrix = symbols.map { sym ->
            species.mapIndexed { idx, sp ->
                (if (idx < left.size) 1.0 else -1.0) * (sp.atoms[sym] ?: 0)
            }.toDoubleArray()
        }.toTypedArray()

        // Gaussian elimination to RREF
        val mRows = matrix.size
        var r = 0
        val pivots = mutableListOf<Int>()

        for (c in 0 until n) {
            if (r >= mRows) break
            var p = r
            for (k in r + 1 until mRows) {
                if (abs(matrix[k][c]) > abs(matrix[p][c])) p = k
            }
            if (abs(matrix[p][c]) < 1e-9) continue

            val temp = matrix[r]
            matrix[r] = matrix[p]
            matrix[p] = temp

            val pv = matrix[r][c]
            for (j in 0 until n) matrix[r][j] /= pv

            for (k in 0 until mRows) {
                if (k != r) {
                    val factor = matrix[k][c]
                    if (abs(factor) > 1e-9) {
                        for (j in 0 until n) matrix[k][j] -= factor * matrix[r][j]
                    }
                }
            }
            pivots.add(c)
            r++
        }

        // Check for inconsistency
        for (k in r until mRows) {
            if (matrix[k].any { abs(it) > 1e-8 }) {
                throw IllegalArgumentException("These formulas do not form a valid balanced reaction.")
            }
        }

        val freeVars = (0 until n).filter { !pivots.contains(it) }
        if (freeVars.size != 1) {
            throw IllegalArgumentException("No unique single-ratio balance found. Verify formulas.")
        }

        val freeCol = freeVars[0]
        val solution = DoubleArray(n)
        solution[freeCol] = 1.0

        for (k in pivots.indices.reversed()) {
            val pivotCol = pivots[k]
            solution[pivotCol] = -matrix[k][freeCol]
        }

        if (solution.all { it < 0 }) {
            for (i in solution.indices) solution[i] = -solution[i]
        }

        if (solution.any { it <= 1e-7 }) {
            throw IllegalArgumentException("No positive integer coefficient set found.")
        }

        val fractions = solution.map { toFraction(it) }
        val commonDenominator = fractions.fold(1L) { acc, frac -> lcm(acc, frac.second) }
        var integerCoeffs = fractions.map { ((it.first * commonDenominator) / it.second).toInt() }

        val commonGcd = integerCoeffs.map { it.toLong() }.reduce { a, b -> gcd(a, b) }
        integerCoeffs = integerCoeffs.map { (it / commonGcd).toInt() }

        // Build formatted string with unicode subscript
        fun formatSide(list: List<Species>, coeffs: List<Int>): String {
            return list.mapIndexed { i, sp ->
                val c = coeffs[i]
                val prefix = if (c == 1) "" else "$c "
                prefix + toSubscript(sp.formula)
            }.joinToString(" + ")
        }

        val leftCoeffs = integerCoeffs.subList(0, left.size)
        val rightCoeffs = integerCoeffs.subList(left.size, n)
        val balancedString = "${formatSide(left, leftCoeffs)} → ${formatSide(right, rightCoeffs)}"

        val rows = symbols.map { sym ->
            val reactantTotal = left.indices.sumOf { i -> leftCoeffs[i] * (left[i].atoms[sym] ?: 0) }
            val productTotal = right.indices.sumOf { i -> rightCoeffs[i] * (right[i].atoms[sym] ?: 0) }
            AtomBalanceRow(
                symbol = sym,
                reactantCount = reactantTotal,
                productCount = productTotal,
                isBalanced = reactantTotal == productTotal
            )
        }

        return BalancedReactionResult(
            equation = balancedString,
            reactants = left.map { it.formula },
            products = right.map { it.formula },
            coefficients = integerCoeffs,
            atomBalances = rows
        )
    }

    fun toSubscript(formula: String): String {
        return formula.replace(Regex("(\\d+)")) { match ->
            match.value.map { ch ->
                when (ch) {
                    '0' -> '₀'
                    '1' -> '₁'
                    '2' -> '₂'
                    '3' -> '₃'
                    '4' -> '₄'
                    '5' -> '₅'
                    '6' -> '₆'
                    '7' -> '₇'
                    '8' -> '₈'
                    '9' -> '₉'
                    else -> ch
                }
            }.joinToString("")
        }
    }
}
