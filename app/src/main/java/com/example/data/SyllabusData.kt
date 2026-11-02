package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

enum class SyllabusStatus(val label: String, val weight: Float) {
    NOT_STARTED("Not Started", 0.0f),
    STUDYING("Studying", 0.35f),
    NEEDS_REVISION("Needs Revision", 0.65f),
    MASTERED("Mastered", 1.0f)
}

data class SyllabusTopic(
    val id: String,
    val name: String,
    val subtopics: List<String> = emptyList()
)

data class SyllabusUnit(
    val id: String,
    val unitNumber: Int,
    val title: String,
    val topics: List<SyllabusTopic>
)

data class SubjectSyllabus(
    val subject: String,
    val grade: Int,
    val units: List<SyllabusUnit>
)

object SyllabusData {

    val curriculum: List<SubjectSyllabus> = listOf(
        // MATHEMATICS
        SubjectSyllabus(
            subject = "Mathematics",
            grade = 11,
            units = listOf(
                SyllabusUnit(
                    id = "math_11_u1",
                    unitNumber = 1,
                    title = "Further on Relations and Functions",
                    topics = listOf(
                        SyllabusTopic("m11_u1_t1", "Rational Functions and Graphs", listOf("Domain, range, and vertical/horizontal asymptotes", "Simplifying rational expressions")),
                        SyllabusTopic("m11_u1_t2", "Inverse of Functions", listOf("One-to-one criteria", "Finding inverse algebraically and graphically"))
                    )
                ),
                SyllabusUnit(
                    id = "math_11_u2",
                    unitNumber = 2,
                    title = "Coordinate Geometry",
                    topics = listOf(
                        SyllabusTopic("m11_u2_t1", "Straight Lines & Distances", listOf("Slope, collinearity, angle between lines", "Distance from point to a line")),
                        SyllabusTopic("m11_u2_t2", "Conic Sections: Circles & Parabolas", listOf("Standard equations of circles", "Focus and directrix of parabolas"))
                    )
                ),
                SyllabusUnit(
                    id = "math_11_u3",
                    unitNumber = 3,
                    title = "Matrices and Determinants",
                    topics = listOf(
                        SyllabusTopic("m11_u3_t1", "Matrix Operations", listOf("Matrix addition, scalar and matrix multiplication", "Transposition and symmetric matrices")),
                        SyllabusTopic("m11_u3_t2", "Determinants and Inverses", listOf("2x2 and 3x3 determinants", "Cramer's rule and system of linear equations"))
                    )
                ),
                SyllabusUnit(
                    id = "math_11_u4",
                    unitNumber = 4,
                    title = "Vectors in Two and Three Dimensions",
                    topics = listOf(
                        SyllabusTopic("m11_u4_t1", "Vector Algebra", listOf("Magnitude, direction, and unit vectors", "Dot product and orthogonality")),
                        SyllabusTopic("m11_u4_t2", "Applications of Vectors", listOf("Work done by constant force", "Projections of vectors"))
                    )
                )
            )
        ),
        SubjectSyllabus(
            subject = "Mathematics",
            grade = 12,
            units = listOf(
                SyllabusUnit(
                    id = "math_12_u1",
                    unitNumber = 1,
                    title = "Sequences and Series",
                    topics = listOf(
                        SyllabusTopic("m12_u1_t1", "Arithmetic & Geometric Progressions", listOf("General term formulas", "Sum of finite and infinite geometric series")),
                        SyllabusTopic("m12_u1_t2", "Sigma Notation & Applications", listOf("Properties of summation", "Mortgage, annuities and compound interest models"))
                    )
                ),
                SyllabusUnit(
                    id = "math_12_u2",
                    unitNumber = 2,
                    title = "Introduction to Calculus: Limits & Continuity",
                    topics = listOf(
                        SyllabusTopic("m12_u2_t1", "Limits of Algebraic & Trigonometric Functions", listOf("Limit laws and indeterminate forms 0/0", "Limits at infinity")),
                        SyllabusTopic("m12_u2_t2", "Continuity of Functions", listOf("One-sided limits and continuity test", "Intermediate value theorem"))
                    )
                ),
                SyllabusUnit(
                    id = "math_12_u3",
                    unitNumber = 3,
                    title = "Differential Calculus",
                    topics = listOf(
                        SyllabusTopic("m12_u3_t1", "Rules of Differentiation", listOf("Power, product, quotient, and chain rules", "Derivatives of trigonometric & exponential functions")),
                        SyllabusTopic("m12_u3_t2", "Applications of Derivatives", listOf("Tangents, normals, and rates of change", "Local extrema, concavity, and optimization problems"))
                    )
                ),
                SyllabusUnit(
                    id = "math_12_u4",
                    unitNumber = 4,
                    title = "Integral Calculus",
                    topics = listOf(
                        SyllabusTopic("m12_u4_t1", "Indefinite & Definite Integrals", listOf("Fundamental theorem of calculus", "Integration by substitution")),
                        SyllabusTopic("m12_u4_t2", "Applications of Integration", listOf("Area under and between curves", "Volumes of solids of revolution"))
                    )
                )
            )
        ),

        // PHYSICS
        SubjectSyllabus(
            subject = "Physics",
            grade = 11,
            units = listOf(
                SyllabusUnit(
                    id = "phys_11_u1",
                    unitNumber = 1,
                    title = "Measurement and Practical Work",
                    topics = listOf(
                        SyllabusTopic("p11_u1_t1", "Vectors & Scalar Quantities", listOf("Components of vectors", "Vector resolution and scalar product")),
                        SyllabusTopic("p11_u1_t2", "Errors & Significant Figures", listOf("Absolute and percentage uncertainties", "Dimensional analysis"))
                    )
                ),
                SyllabusUnit(
                    id = "phys_11_u2",
                    unitNumber = 2,
                    title = "Two-Dimensional Motion",
                    topics = listOf(
                        SyllabusTopic("p11_u2_t1", "Projectile Motion", listOf("Horizontal and angled projections", "Range, maximum height, and time of flight")),
                        SyllabusTopic("p11_u2_t2", "Uniform Circular Motion", listOf("Centripetal acceleration and centripetal force", "Banked roads and orbital motion"))
                    )
                ),
                SyllabusUnit(
                    id = "phys_11_u3",
                    unitNumber = 3,
                    title = "Work, Energy and Power",
                    topics = listOf(
                        SyllabusTopic("p11_u3_t1", "Work-Energy Theorem", listOf("Kinetic energy and potential energy", "Conservation of mechanical energy")),
                        SyllabusTopic("p11_u3_t2", "Collisions & Momentum", listOf("Linear momentum and impulse", "Elastic vs inelastic collisions"))
                    )
                )
            )
        ),
        SubjectSyllabus(
            subject = "Physics",
            grade = 12,
            units = listOf(
                SyllabusUnit(
                    id = "phys_12_u1",
                    unitNumber = 1,
                    title = "Thermodynamics",
                    topics = listOf(
                        SyllabusTopic("p12_u1_t1", "Heat & First Law of Thermodynamics", listOf("Internal energy and work done by gas", "Isobaric, isochoric, isothermal, adiabatic processes")),
                        SyllabusTopic("p12_u1_t2", "Heat Engines & Second Law", listOf("Carnot engine efficiency", "Entropy and irreversible processes"))
                    )
                ),
                SyllabusUnit(
                    id = "phys_12_u2",
                    unitNumber = 2,
                    title = "Oscillations and Waves",
                    topics = listOf(
                        SyllabusTopic("p12_u2_t1", "Simple Harmonic Motion", listOf("Displacement, velocity, acceleration in SHM", "Mass-spring and simple pendulum equations")),
                        SyllabusTopic("p12_u2_t2", "Wave Properties & Sound", listOf("Standing waves, resonance, and nodes", "Doppler effect"))
                    )
                ),
                SyllabusUnit(
                    id = "phys_12_u3",
                    unitNumber = 3,
                    title = "Electromagnetism",
                    topics = listOf(
                        SyllabusTopic("p12_u3_t1", "Electric Current & DC Circuits", listOf("Ohm's law, Kirchhoff's current and voltage rules", "Wheatstone bridge and potentiometer")),
                        SyllabusTopic("p12_u3_t2", "Magnetic Fields & Induction", listOf("Lorentz force on moving charges", "Faraday's law and Lenz's law"))
                    )
                )
            )
        ),

        // CHEMISTRY
        SubjectSyllabus(
            subject = "Chemistry",
            grade = 11,
            units = listOf(
                SyllabusUnit(
                    id = "chem_11_u1",
                    unitNumber = 1,
                    title = "Fundamental Concepts in Chemistry",
                    topics = listOf(
                        SyllabusTopic("c11_u1_t1", "Atomic Structure & Periodic Trends", listOf("Quantum numbers and electronic configuration", "Ionization energy, electron affinity, and electronegativity")),
                        SyllabusTopic("c11_u1_t2", "Chemical Bonding", listOf("Ionic, covalent, coordinate and metallic bonding", "VSEPR theory and molecular geometry"))
                    )
                ),
                SyllabusUnit(
                    id = "chem_11_u2",
                    unitNumber = 2,
                    title = "Chemical Equilibrium",
                    topics = listOf(
                        SyllabusTopic("c11_u2_t1", "Equilibrium Law and Kc/Kp", listOf("Reversible reactions and law of mass action", "Calculations of equilibrium concentrations")),
                        SyllabusTopic("c11_u2_t2", "Le Chatelier's Principle", listOf("Effect of temperature, pressure, concentration", "Industrial applications: Haber and Contact processes"))
                    )
                )
            )
        ),
        SubjectSyllabus(
            subject = "Chemistry",
            grade = 12,
            units = listOf(
                SyllabusUnit(
                    id = "chem_12_u1",
                    unitNumber = 1,
                    title = "Electrochemistry",
                    topics = listOf(
                        SyllabusTopic("c12_u1_t1", "Galvanic & Electrolytic Cells", listOf("Electrode potentials and standard hydrogen electrode", "Nernst equation and cell EMF")),
                        SyllabusTopic("c12_u1_t2", "Electrolysis & Quantitative Laws", listOf("Faraday's first and second laws of electrolysis", "Industrial extraction of Aluminium and Copper"))
                    )
                ),
                SyllabusUnit(
                    id = "chem_12_u2",
                    unitNumber = 2,
                    title = "Organic Chemistry",
                    topics = listOf(
                        SyllabusTopic("c12_u2_t1", "Hydrocarbons & Functional Groups", listOf("Alkanes, alkenes, alkynes, and aromatic compounds", "Alcohols, aldehydes, ketones, and carboxylic acids")),
                        SyllabusTopic("c12_u2_t2", "Reaction Mechanisms & Polymers", listOf("Substitution, addition, and elimination reactions", "Synthetic and natural polymers"))
                    )
                )
            )
        ),

        // BIOLOGY
        SubjectSyllabus(
            subject = "Biology",
            grade = 11,
            units = listOf(
                SyllabusUnit(
                    id = "bio_11_u1",
                    unitNumber = 1,
                    title = "The Science of Biology and Biochemical Molecules",
                    topics = listOf(
                        SyllabusTopic("b11_u1_t1", "Biological Molecules", listOf("Carbohydrates, lipids, proteins, and nucleic acids", "Enzyme structure, mechanism, and inhibition")),
                        SyllabusTopic("b11_u1_t2", "Cell Biology & Ultrastructure", listOf("Prokaryotic vs Eukaryotic organelles", "Fluid mosaic model and membrane transport"))
                    )
                ),
                SyllabusUnit(
                    id = "bio_11_u2",
                    unitNumber = 2,
                    title = "Cellular Metabolism",
                    topics = listOf(
                        SyllabusTopic("b11_u2_t1", "Aerobic & Anaerobic Respiration", listOf("Glycolysis, Krebs cycle, and electron transport chain", "ATP yield and lactic acid / alcohol fermentation")),
                        SyllabusTopic("b11_u2_t2", "Photosynthesis", listOf("Light-dependent reactions and photolysis", "Calvin-Benson cycle and carbon fixation"))
                    )
                )
            )
        ),
        SubjectSyllabus(
            subject = "Biology",
            grade = 12,
            units = listOf(
                SyllabusUnit(
                    id = "bio_12_u1",
                    unitNumber = 1,
                    title = "Genetics and Molecular Biology",
                    topics = listOf(
                        SyllabusTopic("b12_u1_t1", "Mendelian Genetics", listOf("Monohybrid and dihybrid crosses", "Sex-linked inheritance and pedigrees")),
                        SyllabusTopic("b12_u1_t2", "DNA Replication & Protein Synthesis", listOf("Transcription, mRNA processing, and translation", "Genetic code and mutations"))
                    )
                ),
                SyllabusUnit(
                    id = "bio_12_u2",
                    unitNumber = 2,
                    title = "Evolution and Ecology",
                    topics = listOf(
                        SyllabusTopic("b12_u2_t1", "Theories of Evolution", listOf("Darwinian natural selection and adaptation", "Speciation and evidence of evolution")),
                        SyllabusTopic("b12_u2_t2", "Ecosystem Dynamics", listOf("Biogeochemical cycles (Carbon, Nitrogen)", "Population growth models and conservation"))
                    )
                )
            )
        ),

        // ENGLISH
        SubjectSyllabus(
            subject = "English",
            grade = 12,
            units = listOf(
                SyllabusUnit(
                    id = "eng_12_u1",
                    unitNumber = 1,
                    title = "Advanced Grammar & Structures",
                    topics = listOf(
                        SyllabusTopic("e12_u1_t1", "Tense Concord & Conditionals", listOf("Zero, first, second, third, and mixed conditionals", "Past perfect and continuous aspects")),
                        SyllabusTopic("e12_u1_t2", "Voice and Reported Speech", listOf("Active to passive conversion in complex clauses", "Direct to indirect reported statements and questions"))
                    )
                ),
                SyllabusUnit(
                    id = "eng_12_u2",
                    unitNumber = 2,
                    title = "Reading Comprehension & Academic Vocabulary",
                    topics = listOf(
                        SyllabusTopic("e12_u2_t1", "Critical Reading Skills", listOf("Identifying main idea, tone, and author purpose", "Context clues and inference drawing")),
                        SyllabusTopic("e12_u2_t2", "Academic Word List & Idioms", listOf("High-frequency Greek/Latin roots, prefixes, suffixes", "Collocations and phrasal verbs"))
                    )
                )
            )
        ),

        // APTITUDE
        SubjectSyllabus(
            subject = "General Aptitude",
            grade = 12,
            units = listOf(
                SyllabusUnit(
                    id = "apt_12_u1",
                    unitNumber = 1,
                    title = "Quantitative & Numerical Reasoning",
                    topics = listOf(
                        SyllabusTopic("a12_u1_t1", "Number & Letter Series", listOf("Arithmetic and geometric patterns", "Alternating, double, and Fibonacci sequences")),
                        SyllabusTopic("a12_u1_t2", "Data Interpretation & Problem Solving", listOf("Percentages, ratios, profit/loss, time & work", "Tables, bar charts, and pie charts analysis"))
                    )
                ),
                SyllabusUnit(
                    id = "apt_12_u2",
                    unitNumber = 2,
                    title = "Verbal & Deductive Reasoning",
                    topics = listOf(
                        SyllabusTopic("a12_u2_t1", "Analogies & Categorization", listOf("Word relationships and classifications", "Odd one out identification")),
                        SyllabusTopic("a12_u2_t2", "Logical Deduction & Syllogisms", listOf("Statement and argument analysis", "Blood relations, coding-decoding, and direction sense"))
                    )
                )
            )
        )
    )
}

class SyllabusManager(private val context: Context) {
    private val file = File(context.filesDir, "euee_syllabus_progress.json")
    private val _statusMap = MutableStateFlow<Map<String, SyllabusStatus>>(emptyMap())
    val statusMap: StateFlow<Map<String, SyllabusStatus>> = _statusMap.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext
        try {
            val json = JSONObject(file.readText())
            val map = mutableMapOf<String, SyllabusStatus>()
            json.keys().forEach { k ->
                val statusName = json.optString(k)
                val status = runCatching { SyllabusStatus.valueOf(statusName) }.getOrDefault(SyllabusStatus.NOT_STARTED)
                map[k] = status
            }
            _statusMap.value = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setStatus(topicId: String, status: SyllabusStatus) = withContext(Dispatchers.IO) {
        val updated = _statusMap.value.toMutableMap()
        updated[topicId] = status
        _statusMap.value = updated
        try {
            val json = JSONObject()
            updated.forEach { (k, v) -> json.put(k, v.name) }
            file.writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun calculateProgress(subject: String, grade: Int? = null): Float {
        val relevantSyllabus = SyllabusData.curriculum.filter {
            it.subject.equals(subject, ignoreCase = true) && (grade == null || it.grade == grade)
        }
        val allTopicIds = relevantSyllabus.flatMap { it.units.flatMap { u -> u.topics.map { t -> t.id } } }
        if (allTopicIds.isEmpty()) return 0f

        val currentMap = _statusMap.value
        val totalScore = allTopicIds.sumOf { id ->
            val status = currentMap[id] ?: SyllabusStatus.NOT_STARTED
            status.weight.toDouble()
        }
        return (totalScore / allTopicIds.size).toFloat().coerceIn(0f, 1f)
    }

    fun calculateOverallProgress(): Float {
        val allTopicIds = SyllabusData.curriculum.flatMap { it.units.flatMap { u -> u.topics.map { t -> t.id } } }
        if (allTopicIds.isEmpty()) return 0f
        val currentMap = _statusMap.value
        val totalScore = allTopicIds.sumOf { id ->
            (currentMap[id] ?: SyllabusStatus.NOT_STARTED).weight.toDouble()
        }
        return (totalScore / allTopicIds.size).toFloat().coerceIn(0f, 1f)
    }
}
