package com.example.data

import com.example.model.Question
import com.example.model.QuestionBank

object EUEEQuestionBanks {

    val mathQuestions: List<Question> = listOf(
        Question(
            id = "math_q1",
            topic = "Calculus",
            prompt = "What is the derivative of f(x) = 3x⁴ - 5x² + 7x - 9 with respect to x?",
            answers = listOf("12x³ - 10x + 7", "12x^3 - 10x + 7"),
            options = listOf("12x³ - 10x + 7", "12x³ - 5x + 7", "3x³ - 10x + 7", "12x⁴ - 10x² + 7"),
            difficulty = "medium",
            type = "formula",
            explanation = "Using the power rule d/dx[xⁿ] = n·xⁿ⁻¹: d/dx[3x⁴] = 12x³, d/dx[-5x²] = -10x, d/dx[7x] = 7, d/dx[-9] = 0. Therefore, f'(x) = 12x³ - 10x + 7.",
            memoryTip = "Power rule: multiply by the exponent, then subtract 1 from the power."
        ),
        Question(
            id = "math_q2",
            topic = "Calculus",
            prompt = "Evaluate the definite integral ∫₀² (3x² + 2x) dx.",
            answers = listOf("12"),
            options = listOf("12", "14", "10", "16"),
            difficulty = "medium",
            type = "number",
            explanation = "Antiderivative F(x) = x³ + x². Evaluating from 0 to 2: F(2) = 2³ + 2² = 8 + 4 = 12. F(0) = 0. Thus, 12 - 0 = 12.",
            memoryTip = "Add 1 to the power and divide by the new power: ∫xⁿ dx = xⁿ⁺¹/(n+1)."
        ),
        Question(
            id = "math_q3",
            topic = "Matrices and Determinants",
            prompt = "If matrix A = [[2, 3], [1, 4]], what is the determinant det(A)?",
            answers = listOf("5"),
            options = listOf("5", "11", "-5", "8"),
            difficulty = "easy",
            type = "number",
            explanation = "For a 2x2 matrix [[a, b], [c, d]], det = ad - bc. Here det(A) = (2)(4) - (3)(1) = 8 - 3 = 5.",
            memoryTip = "Main diagonal product minus anti-diagonal product: ad - bc."
        ),
        Question(
            id = "math_q4",
            topic = "Vectors",
            prompt = "Find the dot product of vectors u = (3, -2, 4) and v = (2, 5, -1).",
            answers = listOf("-8"),
            options = listOf("-8", "6", "10", "-4"),
            difficulty = "easy",
            type = "number",
            explanation = "u · v = (3)(2) + (-2)(5) + (4)(-1) = 6 - 10 - 4 = -8.",
            memoryTip = "Multiply corresponding components and sum them up."
        ),
        Question(
            id = "math_q5",
            topic = "Sequences and Series",
            prompt = "In an arithmetic sequence where the first term a₁ = 4 and common difference d = 3, what is the 15th term a₁₅?",
            answers = listOf("46"),
            options = listOf("46", "49", "45", "42"),
            difficulty = "easy",
            type = "number",
            explanation = "Formula for the n-th term of an AP: aₙ = a₁ + (n - 1)d. a₁₅ = 4 + (15 - 1)(3) = 4 + 42 = 46.",
            memoryTip = "aₙ = a₁ + (n - 1)d."
        ),
        Question(
            id = "math_q6",
            topic = "Probability",
            prompt = "Two standard fair 6-sided dice are rolled. What is the probability that the sum of the numbers is 7?",
            answers = listOf("1/6", "6/36", "0.167"),
            options = listOf("1/6", "1/12", "7/36", "5/36"),
            difficulty = "medium",
            type = "formula",
            explanation = "There are 36 total outcomes. Pairs summing to 7 are (1,6), (2,5), (3,4), (4,3), (5,2), (6,1) — 6 outcomes. Probability = 6/36 = 1/6.",
            memoryTip = "7 is the most probable sum with two dice because it has the most combinations (6 out of 36)."
        ),
        Question(
            id = "math_q7",
            topic = "Coordinate Geometry",
            prompt = "What is the distance between the points P(2, -3) and Q(6, 0) in the Cartesian plane?",
            answers = listOf("5"),
            options = listOf("5", "7", "√7", "25"),
            difficulty = "easy",
            type = "number",
            explanation = "Distance d = √[(x₂ - x₁)² + (y₂ - y₁)²] = √[(6 - 2)² + (0 - (-3))²] = √[4² + 3²] = √[16 + 9] = √25 = 5.",
            memoryTip = "Recognize the 3-4-5 Pythagorean right triangle."
        ),
        Question(
            id = "math_q8",
            topic = "Trigonometry",
            prompt = "What is the exact value of sin(150°)?",
            answers = listOf("1/2", "0.5"),
            options = listOf("1/2", "-1/2", "√3/2", "-√3/2"),
            difficulty = "medium",
            type = "formula",
            explanation = "150° is in the second quadrant where sine is positive. Reference angle = 180° - 150° = 30°. sin(150°) = sin(30°) = 1/2.",
            memoryTip = "ASTC: Sine is positive in Quadrant II. Reference angle is 180° - θ."
        )
    )

    val physicsQuestions: List<Question> = listOf(
        Question(
            id = "phys_q1",
            topic = "Kinematics",
            prompt = "A car starts from rest and accelerates uniformly at 2.5 m/s² for 8 seconds. What distance does it cover?",
            answers = listOf("80", "80 m", "80m"),
            options = listOf("80 m", "40 m", "100 m", "20 m"),
            difficulty = "medium",
            type = "number",
            unit = "m",
            explanation = "Using s = ut + 0.5at²: since u = 0, s = 0.5 × 2.5 × 8² = 0.5 × 2.5 × 64 = 80 m.",
            memoryTip = "When starting from rest, initial velocity u = 0, so distance simplifies to s = 1/2 at²."
        ),
        Question(
            id = "phys_q2",
            topic = "Dynamics",
            prompt = "A constant net force of 60 N acts on an object of mass 12 kg. What is the acceleration produced?",
            answers = listOf("5", "5 m/s²"),
            options = listOf("5 m/s²", "720 m/s²", "0.2 m/s²", "48 m/s²"),
            difficulty = "easy",
            type = "number",
            unit = "m/s²",
            explanation = "Newton's second law states F = ma. Therefore, a = F / m = 60 N / 12 kg = 5 m/s².",
            memoryTip = "F = m · a: Force in Newtons, mass in kg, acceleration in m/s²."
        ),
        Question(
            id = "phys_q3",
            topic = "Electricity and Magnetism",
            prompt = "Three resistors of 6 Ω, 3 Ω, and 2 Ω are connected in parallel. What is their equivalent resistance?",
            answers = listOf("1", "1 Ω"),
            options = listOf("1 Ω", "11 Ω", "2 Ω", "0.5 Ω"),
            difficulty = "medium",
            type = "number",
            unit = "Ω",
            explanation = "1/R_eq = 1/6 + 1/3 + 1/2 = 1/6 + 2/6 + 3/6 = 6/6 = 1. Therefore R_eq = 1 Ω.",
            memoryTip = "Equivalent resistance of parallel resistors is always smaller than the smallest individual resistor."
        ),
        Question(
            id = "phys_q4",
            topic = "Work, Energy and Power",
            prompt = "An electric motor lifts a 200 kg load vertically through a height of 15 m in 10 seconds. Taking g = 10 m/s², what is the power output?",
            answers = listOf("3000", "3000 W", "3 kW"),
            options = listOf("3000 W", "1500 W", "300 W", "20000 W"),
            difficulty = "medium",
            type = "number",
            unit = "W",
            explanation = "Work done W = mgh = 200 kg × 10 m/s² × 15 m = 30,000 J. Power P = W / t = 30,000 J / 10 s = 3000 W (3 kW).",
            memoryTip = "Power is the rate of doing work: P = W/t = mgh/t."
        ),
        Question(
            id = "phys_q5",
            topic = "Waves and Optics",
            prompt = "A sound wave traveling in air has a frequency of 500 Hz and a speed of 340 m/s. What is its wavelength?",
            answers = listOf("0.68", "0.68 m"),
            options = listOf("0.68 m", "1.47 m", "170 m", "0.34 m"),
            difficulty = "easy",
            type = "number",
            unit = "m",
            explanation = "Using the wave equation v = f · λ, we have λ = v / f = 340 m/s / 500 Hz = 0.68 m.",
            memoryTip = "v = fλ : velocity = frequency × wavelength."
        ),
        Question(
            id = "phys_q6",
            topic = "Thermodynamics",
            prompt = "Which thermodynamic process occurs at constant pressure?",
            answers = listOf("isobaric", "isobaric process"),
            options = listOf("Isobaric", "Isochoric", "Isothermal", "Adiabatic"),
            difficulty = "easy",
            explanation = "An isobaric process is one in which the pressure of the system remains constant (ΔP = 0). Isochoric is constant volume; Isothermal is constant temperature; Adiabatic is no heat exchange (Q = 0).",
            memoryTip = "Bar is a unit of pressure → Isobaric = constant pressure."
        )
    )

    val biologyQuestions: List<Question> = listOf(
        Question(
            id = "bio_q1",
            topic = "Cell Biology",
            prompt = "Which cell organelle is known as the powerhouse of the eukaryotic cell where ATP is generated via cellular respiration?",
            answers = listOf("mitochondria", "mitochondrion"),
            options = listOf("Mitochondria", "Ribosome", "Golgi apparatus", "Endoplasmic reticulum"),
            difficulty = "easy",
            explanation = "Mitochondria produce most of the cell's supply of ATP (adenosine triphosphate) through oxidative phosphorylation during aerobic cellular respiration.",
            memoryTip = "Mitochondria = cellular powerhouse and ATP factory."
        ),
        Question(
            id = "bio_q2",
            topic = "Genetics",
            prompt = "In a monohybrid cross between two heterozygous pea plants (Bb × Bb), what is the expected phenotypic ratio of dominant to recessive traits?",
            answers = listOf("3:1", "3 to 1"),
            options = listOf("3:1", "1:2:1", "9:3:3:1", "1:1"),
            difficulty = "medium",
            explanation = "The genotypic ratio is 1 BB : 2 Bb : 1 bb. Because BB and Bb both express the dominant phenotype, 3 display dominant and 1 displays recessive, giving a 3:1 phenotypic ratio.",
            memoryTip = "Genotypic ratio is 1:2:1; Phenotypic ratio is 3:1."
        ),
        Question(
            id = "bio_q3",
            topic = "Molecular Biology",
            prompt = "Which nitrogenous base is found in RNA molecules but is absent in DNA?",
            answers = listOf("uracil", "u"),
            options = listOf("Uracil", "Thymine", "Cytosine", "Adenine"),
            difficulty = "easy",
            explanation = "In RNA, Uracil (U) replaces Thymine (T) and pairs with Adenine (A).",
            memoryTip = "DNA has Thymine (T); RNA has Uracil (U)."
        ),
        Question(
            id = "bio_q4",
            topic = "Human Physiology",
            prompt = "Which blood vessels carry oxygenated blood from the lungs back to the left atrium of the heart?",
            answers = listOf("pulmonary veins", "pulmonary vein"),
            options = listOf("Pulmonary veins", "Pulmonary arteries", "Aorta", "Vena cava"),
            difficulty = "medium",
            explanation = "Pulmonary veins are the only veins in the adult human body that carry oxygenated blood, transporting it from the lungs to the left atrium.",
            memoryTip = "Pulmonary veins are the exception to the rule that veins carry deoxygenated blood."
        ),
        Question(
            id = "bio_q5",
            topic = "Ecology",
            prompt = "What trophic level do primary consumers occupy in an ecological food chain?",
            answers = listOf("second", "second trophic level", "trophic level 2"),
            options = listOf("Second trophic level", "First trophic level", "Third trophic level", "Fourth trophic level"),
            difficulty = "medium",
            explanation = "Producers (plants/algae) occupy the first trophic level. Herbivores (primary consumers) feed on producers and occupy the second trophic level.",
            memoryTip = "Trophic level 1 = Producers; Trophic level 2 = Primary consumers (herbivores)."
        )
    )

    val englishQuestions: List<Question> = listOf(
        Question(
            id = "eng_q1",
            topic = "Grammar and Tenses",
            prompt = "Choose the correct sentence completion: 'By the time the professor arrived, the students _____ their presentation.'",
            answers = listOf("had already finished"),
            options = listOf("had already finished", "have already finished", "already finished", "were finishing"),
            difficulty = "medium",
            explanation = "When describing an action that occurred before another action in the past, use the Past Perfect tense (had + past participle).",
            memoryTip = "Past before past = Past Perfect ('had done')."
        ),
        Question(
            id = "eng_q2",
            topic = "Conditionals",
            prompt = "Complete the third conditional sentence: 'If she had studied harder, she _____ the entrance examination.'",
            answers = listOf("would have passed"),
            options = listOf("would have passed", "will pass", "would pass", "had passed"),
            difficulty = "medium",
            explanation = "Third conditional structure: If + past perfect, would have + past participle. It expresses a hypothetical past situation and its unreal outcome.",
            memoryTip = "Third conditional: If + had done ... would have done."
        ),
        Question(
            id = "eng_q3",
            topic = "Vocabulary in Context",
            prompt = "What is the synonym of the word 'METICULOUS'?",
            answers = listOf("scrupulous", "thorough", "precise"),
            options = listOf("Scrupulous", "Careless", "Ambiguous", "Hasty"),
            difficulty = "hard",
            explanation = "'Meticulous' means showing great attention to detail; very careful and precise. 'Scrupulous' is a close synonym.",
            memoryTip = "Meticulous implies thorough, exact, and attentive to every detail."
        ),
        Question(
            id = "eng_q4",
            topic = "Voice and Speech",
            prompt = "Convert to passive voice: 'The committee will review the proposal tomorrow.'",
            answers = listOf("The proposal will be reviewed by the committee tomorrow.", "The proposal will be reviewed tomorrow by the committee."),
            options = listOf(
                "The proposal will be reviewed by the committee tomorrow.",
                "The proposal would be reviewed by the committee tomorrow.",
                "The proposal is reviewed by the committee tomorrow.",
                "The proposal will review the committee tomorrow."
            ),
            difficulty = "medium",
            explanation = "Future simple passive is formed with 'will be + past participle'.",
            memoryTip = "Active: will + verb → Passive: will be + past participle."
        )
    )

    val aptitudeQuestions: List<Question> = listOf(
        Question(
            id = "apt_q1",
            topic = "Number Series",
            prompt = "Find the missing number in the sequence: 3, 7, 15, 31, 63, ____?",
            answers = listOf("127"),
            options = listOf("127", "125", "128", "95"),
            difficulty = "medium",
            type = "number",
            explanation = "Each term is obtained by multiplying the previous term by 2 and adding 1: 3×2+1=7, 7×2+1=15, 15×2+1=31, 31×2+1=63, 63×2+1=127. Alternatively, the differences double: +4, +8, +16, +32, +64.",
            memoryTip = "Look for 2n + 1 pattern or doubling differences."
        ),
        Question(
            id = "apt_q2",
            topic = "Analogies",
            prompt = "Complete the analogy: THERMOMETER is to TEMPERATURE as BAROMETER is to _____?",
            answers = listOf("atmospheric pressure", "pressure"),
            options = listOf("Atmospheric pressure", "Humidity", "Wind speed", "Rainfall"),
            difficulty = "easy",
            explanation = "A thermometer is an instrument used to measure temperature, while a barometer is used to measure atmospheric pressure.",
            memoryTip = "Barometer measures pressure; Hygrometer measures humidity; Anemometer measures wind speed."
        ),
        Question(
            id = "apt_q3",
            topic = "Logical Deduction",
            prompt = "Statement: All scientists are thinkers. Some thinkers are poets. Conclusion: (I) All poets are scientists. (II) Some poets may be thinkers. Which conclusion follows?",
            answers = listOf("only conclusion ii follows", "only ii"),
            options = listOf("Only conclusion II follows", "Only conclusion I follows", "Both I and II follow", "Neither I nor II follows"),
            difficulty = "hard",
            explanation = "From 'Some thinkers are poets', it directly follows that some poets are thinkers. However, there is no direct connection establishing that all poets are scientists. Thus only conclusion II follows.",
            memoryTip = "Convert 'Some A are B' to 'Some B are A'."
        ),
        Question(
            id = "apt_q4",
            topic = "Direction and Spatial Reasoning",
            prompt = "A student walks 20 meters North, turns right and walks 30 meters, turns right again and walks 20 meters. How far is the student from the starting point?",
            answers = listOf("30", "30 meters", "30m"),
            options = listOf("30 meters East", "30 meters West", "20 meters North", "70 meters East"),
            difficulty = "medium",
            type = "number",
            explanation = "Walking 20m North (+Y) then right 30m (+X) then right 20m (-Y) cancels the North displacement (20 - 20 = 0). The remaining displacement is 30m East.",
            memoryTip = "North and South displacements cancel out, leaving only the Eastward distance."
        )
    )

    fun createAllDefaultBanks(): List<QuestionBank> {
        val now = System.currentTimeMillis()
        return listOf(
            SampleData.createSampleBank(), // Chemistry sample bank
            QuestionBank(
                id = "bank_math",
                name = "EUEE Mathematics (Grades 11-12)",
                at = now,
                questions = mathQuestions
            ),
            QuestionBank(
                id = "bank_physics",
                name = "EUEE Physics (Grades 11-12)",
                at = now,
                questions = physicsQuestions
            ),
            QuestionBank(
                id = "bank_biology",
                name = "EUEE Biology (Grades 11-12)",
                at = now,
                questions = biologyQuestions
            ),
            QuestionBank(
                id = "bank_english",
                name = "EUEE English & Verbal",
                at = now,
                questions = englishQuestions
            ),
            QuestionBank(
                id = "bank_aptitude",
                name = "EUEE General Aptitude & Reasoning",
                at = now,
                questions = aptitudeQuestions
            )
        )
    }
}
