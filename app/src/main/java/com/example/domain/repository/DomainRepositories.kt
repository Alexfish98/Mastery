package com.example.domain.repository

import com.example.domain.model.DictionaryTerm
import com.example.domain.model.QuestionItem
import com.example.domain.model.StudyTrackMetric
import com.example.domain.model.TimetableSlot
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository Interfaces for Clean Architecture Decoupling.
 */

interface ITimetableRepository {
    fun getAllSlots(): Flow<List<TimetableSlot>>
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>>
    suspend fun getSlotById(id: Int): TimetableSlot?
    suspend fun saveSlot(slot: TimetableSlot): Long
    suspend fun deleteSlot(id: Int)
    suspend fun toggleAlarm(id: Int, isEnabled: Boolean)
}

interface IAnalyticsRepository {
    fun getAllMetrics(): Flow<List<StudyTrackMetric>>
    suspend fun addMetric(metric: StudyTrackMetric): Long
    suspend fun updateMetric(metric: StudyTrackMetric)
    suspend fun deleteMetric(id: Int)
    fun getMetricsBySubject(subject: String): Flow<List<StudyTrackMetric>>
}

interface IDictionaryRepository {
    fun getAllTerms(): Flow<List<DictionaryTerm>>
    fun searchTerms(query: String): Flow<List<DictionaryTerm>>
    fun getTermsBySubject(subject: String): Flow<List<DictionaryTerm>>
    suspend fun insertTerm(term: DictionaryTerm): Long
    suspend fun insertTerms(terms: List<DictionaryTerm>): List<Long>
    suspend fun updateTerm(term: DictionaryTerm)
    suspend fun deleteTerm(id: Int)
    suspend fun importJsonData(rawJson: String): Int
    suspend fun importFromStream(inputStream: java.io.InputStream): Int
}

interface IQuestionBankRepository {
    fun getAllQuestions(): Flow<List<QuestionItem>>
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionItem>>
    suspend fun saveQuestion(question: QuestionItem): String
    suspend fun saveQuestions(questions: List<QuestionItem>)
    suspend fun updateQuestion(question: QuestionItem)
    suspend fun deleteQuestion(id: String)
    suspend fun seedDefaultQuestionsIfEmpty()
    suspend fun importQuestionsFromJson(rawJson: String, defaultSubject: String = "Chemistry", defaultGroup: String = "EUEE Questions"): Int
    suspend fun importQuestionsFromStream(
        inputStream: java.io.InputStream,
        defaultSubject: String = "Chemistry",
        defaultGroup: String = "EUEE Questions",
        onProgress: ((Int, String) -> Unit)? = null
    ): Int
}
