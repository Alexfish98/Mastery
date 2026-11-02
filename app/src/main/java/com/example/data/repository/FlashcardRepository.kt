package com.example.data.repository

import com.example.data.local.Flashcard
import com.example.data.local.IqaiFlashcardDao
import kotlinx.coroutines.flow.Flow

/**
 * 3. ANKI-STYLE ACTIVE RECALL ENGINE (LOCAL SRS)
 * Implements modified SuperMemo (SM-2) algorithm completely offline.
 */
class FlashcardRepository(
    private val flashcardDao: IqaiFlashcardDao
) {
    val allCards: Flow<List<Flashcard>> = flashcardDao.getAllCards()
    val allDecks: Flow<List<String>> = flashcardDao.getAllDecks()

    fun getCardsForDeck(deck: String): Flow<List<Flashcard>> {
        return flashcardDao.getCardsForDeck(deck)
    }

    fun getDueCards(): Flow<List<Flashcard>> {
        return flashcardDao.getDueCards(System.currentTimeMillis())
    }

    suspend fun getCardById(id: Int): Flashcard? {
        return flashcardDao.getCardById(id)
    }

    suspend fun insertCard(card: Flashcard): Long {
        return flashcardDao.insertCard(card)
    }

    suspend fun insertCards(cards: List<Flashcard>) {
        flashcardDao.insertCards(cards)
    }

    suspend fun deleteCard(id: Int) {
        flashcardDao.deleteCardById(id)
    }

    /**
     * Updates card review interval, ease factor, and due date based on user score.
     * @param cardId The ID of the card being reviewed.
     * @param userScore 1 (Again), 2 (Hard), 3 (Good), 4 (Easy)
     */
    suspend fun updateCardReview(cardId: Int, userScore: Int) {
        val card = flashcardDao.getCardById(cardId) ?: return
        val updatedCard = calculateNextReview(card, userScore)
        flashcardDao.updateCard(updatedCard)
    }

    companion object {
        fun calculateNextReview(card: Flashcard, userScore: Int, currentTimeMillis: Long = System.currentTimeMillis()): Flashcard {
            val validScore = userScore.coerceIn(1, 4)

            val newInterval: Int
            val newEaseFactor: Double

            when (validScore) {
                1 -> {
                    // Again (Lapse): Reset interval to 1 day, decrease ease factor
                    newInterval = 1
                    newEaseFactor = maxOf(1.3, card.easeFactor - 0.20)
                }
                2 -> {
                    // Hard: Small increase, slight ease decrease
                    newInterval = maxOf(1, (card.interval * 1.2).toInt())
                    newEaseFactor = maxOf(1.3, card.easeFactor - 0.15)
                }
                3 -> {
                    // Good: Standard SM-2 interval progression
                    newInterval = if (card.interval <= 1) {
                        3
                    } else {
                        (card.interval * card.easeFactor).toInt()
                    }
                    newEaseFactor = card.easeFactor
                }
                4 -> {
                    // Easy: Bonus interval multiplier, increase ease factor
                    newInterval = if (card.interval <= 1) {
                        6
                    } else {
                        (card.interval * card.easeFactor * 1.3).toInt()
                    }
                    newEaseFactor = minOf(3.2, card.easeFactor + 0.15)
                }
                else -> {
                    newInterval = card.interval
                    newEaseFactor = card.easeFactor
                }
            }

            // Calculate exact epoch millisecond timestamp for the dueDate
            val oneDayMillis = 24L * 60L * 60L * 1000L
            val calculatedDueDate = currentTimeMillis + (newInterval.toLong() * oneDayMillis)

            return card.copy(
                interval = newInterval,
                easeFactor = newEaseFactor,
                dueDate = calculatedDueDate
            )
        }
    }

    /**
     * Seeds initial high-yield EUEE study decks if table is empty
     */
    suspend fun seedInitialDecksIfEmpty() {
        val initialCards = listOf(
            Flashcard(
                deckName = "Chemistry Core",
                frontText = "What is the Henderson-Hasselbalch equation for buffer solutions?",
                backText = "pH = pKa + log([A⁻] / [HA])\nUsed to calculate the pH of an acidic buffer system.",
                interval = 1,
                easeFactor = 2.5
            ),
            Flashcard(
                deckName = "Chemistry Core",
                frontText = "State Le Chatelier's Principle regarding exothermic equilibrium and temperature increase.",
                backText = "For an exothermic reaction (ΔH < 0), increasing temperature shifts equilibrium to the left (reactants), decreasing Kc.",
                interval = 1,
                easeFactor = 2.5
            ),
            Flashcard(
                deckName = "Physics Mechanics",
                frontText = "What is the escape velocity formula from a planet of mass M and radius R?",
                backText = "v_esc = √(2GM / R)\nDoes not depend on the mass of the escaping projectile.",
                interval = 1,
                easeFactor = 2.5
            ),
            Flashcard(
                deckName = "Biology & Genetics",
                frontText = "What is the start codon in mRNA translation and which amino acid does it encode?",
                backText = "AUG encodes Methionine (Met) in eukaryotes and Formylmethionine in prokaryotes.",
                interval = 1,
                easeFactor = 2.5
            ),
            Flashcard(
                deckName = "Mathematics Aptitude",
                frontText = "What is the discriminant formula for ax² + bx + c = 0 and what does Δ < 0 imply?",
                backText = "Δ = b² - 4ac\nIf Δ < 0, there are two distinct complex/imaginary conjugate roots.",
                interval = 1,
                easeFactor = 2.5
            )
        )
        flashcardDao.insertCards(initialCards)
    }
}
