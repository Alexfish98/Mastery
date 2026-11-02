package com.example.data

import com.example.model.Question
import com.example.model.QuestionBank

object SampleData {

    val sampleQuestions: List<Question> = listOf(
        Question(
            id = "q1",
            topic = "Metallurgy",
            prompt = "What is the process of extracting a metal from its ore called?",
            answers = listOf("extraction", "metal extraction", "extraction of metals"),
            options = listOf("Extraction", "Filtration", "Crystallisation", "Neutralisation"),
            difficulty = "easy",
            explanation = "Metallurgy covers the scientific and technological processes used to extract metals from ores and refine them.",
            memoryTip = "An ore is a naturally occurring material from which a metal or compound can be extracted economically."
        ),
        Question(
            id = "q2",
            topic = "Metallurgy",
            prompt = "Which common aluminium ore is the principal commercial source of aluminium?",
            answers = listOf("bauxite"),
            options = listOf("Bauxite", "Haematite", "Galena", "Limestone"),
            difficulty = "easy",
            explanation = "Bauxite is the principal commercial ore used to produce aluminium. It contains hydrated aluminium oxides and impurities.",
            memoryTip = "Haematite is an important iron ore; galena is a lead ore."
        ),
        Question(
            id = "q3",
            topic = "Chemical symbols",
            prompt = "Write the chemical symbol for aluminium.",
            answers = listOf("Al"),
            options = listOf("Al", "Au", "Ag", "Am"),
            difficulty = "easy",
            type = "formula",
            explanation = "The chemical symbol for aluminium is Al. Symbols are case-sensitive: the first letter is uppercase and the second letter, if present, is lowercase.",
            memoryTip = "Au is gold, Ag is silver, and Am is americium."
        ),
        Question(
            id = "q4",
            topic = "Sulphur",
            prompt = "What is the name of the process used to extract sulphur from underground deposits using superheated water?",
            answers = listOf("frasch process", "frash process"),
            options = listOf("Frasch process", "Haber process", "Hall process", "Contact process"),
            difficulty = "medium",
            explanation = "The Frasch process uses superheated water and compressed air to bring underground sulphur to the surface.",
            memoryTip = "Frasch → sulphur; Haber → ammonia; Contact → sulphuric acid."
        ),
        Question(
            id = "q5",
            topic = "Sulphur",
            prompt = "What is the chemical formula of sulphuric acid?",
            answers = listOf("H2SO4", "H₂SO₄"),
            options = listOf("H₂SO₄", "H₂SO₃", "HSO₄", "H₂S"),
            difficulty = "easy",
            type = "formula",
            explanation = "Sulphuric acid has the formula H₂SO₄: two hydrogen atoms, one sulphur atom, and four oxygen atoms per molecule.",
            memoryTip = "Sulphurous acid is H₂SO₃. Pay attention to the number of oxygen atoms."
        ),
        Question(
            id = "q6",
            topic = "Air and gases",
            prompt = "Approximately what percentage of dry air is oxygen by volume?",
            answers = listOf("21%", "21", "20.95%", "20.95", "about 21%"),
            options = listOf("21%", "50%", "78%", "1%"),
            difficulty = "easy",
            explanation = "Dry air contains approximately 20.95% oxygen by volume, commonly rounded to 21%. Nitrogen is the most abundant gas at about 78%.",
            memoryTip = "Remember approximately 78% nitrogen, 21% oxygen, and 1% other gases."
        ),
        Question(
            id = "q7",
            topic = "Industrial chemistry",
            prompt = "Which industrial process manufactures ammonia from nitrogen and hydrogen?",
            answers = listOf("haber process", "haber-bosch process", "haber bosch process"),
            options = listOf("Haber process", "Frasch process", "Contact process", "Solvay process"),
            difficulty = "medium",
            explanation = "The Haber process combines nitrogen and hydrogen reversibly to produce ammonia using a catalyst at elevated temperature and pressure.",
            memoryTip = "The balanced equation is N₂ + 3H₂ ⇌ 2NH₃."
        ),
        Question(
            id = "q8",
            topic = "Sulphur",
            prompt = "Which crystalline allotrope of sulphur is stable below approximately 95.5°C at ordinary pressure?",
            answers = listOf("rhombic sulphur", "rhombic sulfur", "alpha sulphur", "alpha sulfur", "α-sulphur", "α-sulfur"),
            options = listOf("Rhombic sulphur", "Plastic sulphur", "Graphite", "White phosphorus"),
            difficulty = "hard",
            explanation = "Rhombic (alpha) sulphur is the stable crystalline form below the transition temperature of about 95.5°C at ordinary pressure.",
            memoryTip = "Above the transition temperature, monoclinic sulphur is the stable crystalline form."
        ),
        Question(
            id = "q9",
            topic = "Phosphorus",
            prompt = "What is the molecular formula of white phosphorus?",
            answers = listOf("P4", "P₄"),
            options = listOf("P₄", "P₂", "P₆", "P₈"),
            difficulty = "medium",
            type = "formula",
            explanation = "White phosphorus consists of P₄ tetrahedral molecules. It is more reactive than red phosphorus and requires appropriate laboratory precautions.",
            memoryTip = "The subscript 4 means each molecule contains four phosphorus atoms."
        ),
        Question(
            id = "q10",
            topic = "Industrial chemistry",
            prompt = "Which industrial process is used to manufacture sulphuric acid?",
            answers = listOf("contact process"),
            options = listOf("Contact process", "Haber process", "Frasch process", "Hall process"),
            difficulty = "medium",
            explanation = "The Contact process manufactures sulphuric acid. Sulphur dioxide is oxidised to sulphur trioxide, commonly using vanadium(V) oxide as a catalyst.",
            memoryTip = "Contact process → sulphuric acid; Haber process → ammonia."
        )
    )

    fun createSampleBank(): QuestionBank {
        return QuestionBank(
            id = "sample",
            name = "10 sample chemistry questions",
            at = System.currentTimeMillis(),
            questions = sampleQuestions
        )
    }
}
