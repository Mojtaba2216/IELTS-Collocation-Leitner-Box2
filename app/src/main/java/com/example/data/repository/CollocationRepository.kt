package com.example.data.repository

import android.util.Log
import com.example.data.local.CollocationDao
import com.example.data.local.InitialData
import com.example.data.model.Collocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CollocationRepository(private val collocationDao: CollocationDao) {

    companion object {
        private const val STUDY_DAY_PREFIX = "study-day-"

        fun studyDayKey(day: Int): String = "$STUDY_DAY_PREFIX${day.coerceIn(1, 5)}"
    }

    suspend fun checkAndPrepopulate() {
        withContext(Dispatchers.IO) {
            val existingList = collocationDao.getAllCollocationsList()
            Log.d("CollocationRepository", "Database existing count: ${existingList.size}")

            if (existingList.isEmpty()) {
                collocationDao.insertCollocations(InitialData.list)
                return@withContext
            }

            val existingGrouped = existingList.groupBy { it.english.trim().lowercase() }
            val initialMapped = InitialData.list.associateBy { it.english.trim().lowercase() }

            val toDeleteIds = mutableListOf<Int>()
            val toUpdate = mutableListOf<Collocation>()
            val toInsert = mutableListOf<Collocation>()

            existingGrouped.forEach { (lowercaseEnglish, group) ->
                val initialMatch = initialMapped[lowercaseEnglish]
                if (initialMatch == null) {
                    group.forEach { toDeleteIds.add(it.id) }
                } else {
                    val bestEntry = if (group.size > 1) {
                        val sorted = group.sortedWith(
                            compareByDescending<Collocation> { it.boxIndex }
                                .thenByDescending { it.assignedDate != null }
                                .thenByDescending { it.id }
                        )
                        sorted.first().also {
                            sorted.drop(1).forEach { duplicate -> toDeleteIds.add(duplicate.id) }
                        }
                    } else {
                        group.first()
                    }

                    val updated = bestEntry.copy(
                        persian = initialMatch.persian,
                        pronunciation = initialMatch.pronunciation,
                        level = initialMatch.level,
                        example = initialMatch.example,
                        exampleTranslation = initialMatch.exampleTranslation,
                        category = initialMatch.category
                    )

                    if (updated != bestEntry) toUpdate.add(updated)
                }
            }

            InitialData.list.forEach { card ->
                val key = card.english.trim().lowercase()
                if (!existingGrouped.containsKey(key)) toInsert.add(card)
            }

            toDeleteIds.forEach { collocationDao.deleteCollocationById(it) }
            if (toUpdate.isNotEmpty()) collocationDao.insertCollocations(toUpdate)
            if (toInsert.isNotEmpty()) collocationDao.insertCollocations(toInsert)

            migrateLegacyDateAssignments()
        }
    }

    /**
     * Older builds saved the first batches with real dates such as 2026-07-14.
     * This safely maps those batches to study days 1..5 so existing progress is preserved.
     */
    private suspend fun migrateLegacyDateAssignments() {
        val allCards = collocationDao.getAllCollocationsList()

        allCards.groupBy { it.category }.forEach { (_, categoryCards) ->
            val legacyCards = categoryCards
                .filter { card ->
                    val assignment = card.assignedDate
                    assignment != null && !assignment.startsWith(STUDY_DAY_PREFIX)
                }
                .sortedWith(compareBy<Collocation> { it.assignedDate }.thenBy { it.id })

            if (legacyCards.isEmpty()) return@forEach

            val migrated = legacyCards.mapIndexed { index, card ->
                val targetDay = (index / 10) + 1
                if (targetDay <= 5) {
                    card.copy(assignedDate = studyDayKey(targetDay))
                } else {
                    card
                }
            }

            collocationDao.insertCollocations(migrated)
            Log.d(
                "CollocationRepository",
                "Migrated ${migrated.size} legacy assignments to study-day batches"
            )
        }
    }

    suspend fun resetReviewedTodayIfNeeded() {
        withContext(Dispatchers.IO) {
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            collocationDao.resetReviewedTodayStatus(calendar.timeInMillis)
        }
    }

    fun getAllCollocations(): Flow<List<Collocation>> = collocationDao.getAllCollocations()

    fun getCollocationsByCategory(category: String): Flow<List<Collocation>> =
        collocationDao.getCollocationsByCategory(category)

    fun getAssignedNewCards(category: String, assignmentKey: String): Flow<List<Collocation>> =
        collocationDao.getAssignedCollocationsByCategory(category, assignmentKey)

    suspend fun getStudyDayCards(category: String, studyDay: Int): List<Collocation> =
        withContext(Dispatchers.IO) {
            collocationDao.getAssignedCollocationsByCategoryNonFlow(
                category,
                studyDayKey(studyDay)
            )
        }

    suspend fun getDueCards(category: String, currentTime: Long): List<Collocation> =
        withContext(Dispatchers.IO) {
            collocationDao.getDueCollocationsByCategory(category, currentTime)
        }

    /**
     * Assigns exactly 10 unique new cards to each learning day from day 1 through day 5.
     * Existing assignments are reused, so reopening the app never changes a day's batch.
     */
    suspend fun prepareStudyDayCards(category: String, studyDay: Int) {
        withContext(Dispatchers.IO) {
            val safeDay = studyDay.coerceIn(1, 5)
            val assignmentKey = studyDayKey(safeDay)
            val alreadyAssigned = collocationDao.getAssignedCollocationsByCategoryNonFlow(
                category,
                assignmentKey
            )
            val neededCount = (10 - alreadyAssigned.size).coerceAtLeast(0)

            if (neededCount == 0) return@withContext

            val unstudied = collocationDao.getUnstudiedCollocationsByCategory(category)
            val updated = unstudied.take(neededCount).map { card ->
                card.copy(
                    assignedDate = assignmentKey,
                    boxIndex = 1,
                    nextReviewTime = 0L,
                    isReviewedToday = false
                )
            }

            if (updated.isNotEmpty()) {
                collocationDao.insertCollocations(updated)
                Log.d(
                    "CollocationRepository",
                    "Assigned ${updated.size} cards to $category day $safeDay"
                )
            }
        }
    }

    suspend fun updateCardState(collocation: Collocation) {
        withContext(Dispatchers.IO) {
            collocationDao.updateCollocation(collocation)
        }
    }

    suspend fun rateCard(collocation: Collocation, rating: String) {
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val updatedCard = when (rating) {
            "EASY" -> {
                val nextBox = collocation.boxIndex + 1
                if (nextBox > 5) {
                    collocation.copy(
                        boxIndex = 5,
                        nextReviewTime = Long.MAX_VALUE,
                        lastReviewedTime = currentTime,
                        isReviewedToday = true
                    )
                } else {
                    val interval = when (nextBox) {
                        2 -> oneDayMs
                        3 -> 3 * oneDayMs
                        4 -> 7 * oneDayMs
                        5 -> 14 * oneDayMs
                        else -> oneDayMs
                    }
                    collocation.copy(
                        boxIndex = nextBox,
                        nextReviewTime = currentTime + interval,
                        lastReviewedTime = currentTime,
                        isReviewedToday = true
                    )
                }
            }

            "MEDIUM" -> collocation.copy(
                nextReviewTime = currentTime + (oneDayMs / 2),
                lastReviewedTime = currentTime,
                isReviewedToday = true
            )

            else -> collocation.copy(
                boxIndex = 1,
                nextReviewTime = currentTime + oneDayMs,
                lastReviewedTime = currentTime,
                isReviewedToday = true
            )
        }

        updateCardState(updatedCard)
    }

    suspend fun resetAllProgress() {
        withContext(Dispatchers.IO) {
            val existing = collocationDao.getAllCollocationsList()
            val resetCards = existing.map { card ->
                card.copy(
                    boxIndex = 0,
                    nextReviewTime = 0,
                    lastReviewedTime = 0,
                    assignedDate = null,
                    isReviewedToday = false
                )
            }
            collocationDao.insertCollocations(resetCards)
        }
    }
}
