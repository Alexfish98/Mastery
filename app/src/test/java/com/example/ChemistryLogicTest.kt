package com.example

import com.example.data.PeriodicTable
import com.example.data.SampleData
import com.example.logic.ChemistryLogic
import com.example.model.Question
import com.example.model.SrsSchedule
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ChemistryLogicTest {

    @Test
    fun testNormalization() {
        assertEquals("hello world", ChemistryLogic.norm("  Hello,  World!  "))
        assertEquals("h2so4", ChemistryLogic.norm("H₂SO₄"))
    }

    @Test
    fun testPeriodicTableCount() {
        assertEquals(118, PeriodicTable.elements.size)
        val uniqueSymbols = PeriodicTable.elements.map { it.symbol }.distinct()
        assertEquals(118, uniqueSymbols.size)
    }

    @Test
    fun testMolarMassCalculation() {
        val caOh2 = PeriodicTable.calculateMolarMass("Ca(OH)2")
        // Ca (40.078) + 2*O (31.998) + 2*H (2.016) = 74.092
        assertTrue(abs(caOh2.totalMass - 74.092) < 0.1)

        val hydrate = PeriodicTable.calculateMolarMass("CuSO4·5H2O")
        // CuSO4 (159.609) + 5*H2O (90.076) = ~249.68
        assertTrue(abs(hydrate.totalMass - 249.68) < 0.2)
    }

    @Test
    fun testEquationBalancing() {
        val water = PeriodicTable.balanceEquation("H2 + O2 -> H2O")
        assertEquals(listOf(2, 1, 2), water.coefficients)
        assertTrue(water.atomBalances.all { it.isBalanced })

        val propane = PeriodicTable.balanceEquation("C3H8 + O2 -> CO2 + H2O")
        assertEquals(listOf(1, 5, 3, 4), propane.coefficients)
        assertTrue(propane.atomBalances.all { it.isBalanced })

        val alHcl = PeriodicTable.balanceEquation("Al + HCl -> AlCl3 + H2")
        assertEquals(listOf(2, 6, 2, 3), alHcl.coefficients)
        assertTrue(alHcl.atomBalances.all { it.isBalanced })
    }

    @Test
    fun testAnswerChecking() {
        val qSymbol = Question(
            id = "q_test",
            prompt = "What is the symbol for aluminium?",
            answers = listOf("Al"),
            type = "formula"
        )
        assertTrue(ChemistryLogic.okOf(qSymbol, "Al"))
        assertFalse(ChemistryLogic.okOf(qSymbol, "al")) // formula is case-sensitive

        val qNumber = Question(
            id = "q_calc",
            prompt = "What is the molar mass of water?",
            answers = listOf("18.015"),
            type = "number",
            tol = 0.01
        )
        assertTrue(ChemistryLogic.okOf(qNumber, "18.015"))
        assertTrue(ChemistryLogic.okOf(qNumber, "18.0"))
        assertFalse(ChemistryLogic.okOf(qNumber, "25.0"))
    }

    @Test
    fun testSrsScheduler() {
        val initial = SrsSchedule(box = 0, due = 0L)
        val afterCorrect = ChemistryLogic.srsNext(initial, true, 3)
        assertEquals(1, afterCorrect.box)

        val afterWrong = ChemistryLogic.srsNext(afterCorrect, false, 2)
        assertEquals(0, afterWrong.box)
    }

    @Test
    fun testFlexibleGrading() {
        val qExtraction = Question(
            id = "q1",
            prompt = "What is the process of extracting a metal from its ore called?",
            answers = listOf("extraction", "metal extraction"),
            type = "text"
        )
        assertTrue(ChemistryLogic.okOf(qExtraction, "extraction"))
        assertTrue(ChemistryLogic.okOf(qExtraction, "the answer is extraction"))
        assertTrue(ChemistryLogic.okOf(qExtraction, "about extraction"))

        val qFormula = Question(
            id = "q_acid",
            prompt = "What is the formula of sulphuric acid?",
            answers = listOf("H2SO4", "H₂SO₄"),
            type = "formula"
        )
        assertTrue(ChemistryLogic.okOf(qFormula, "H2SO4"))
        assertTrue(ChemistryLogic.okOf(qFormula, "H₂SO₄"))
        assertTrue(ChemistryLogic.okOf(qFormula, " H₂SO₄ "))
    }
}

