package com.example.data.repository

import android.util.Log
import com.example.data.local.CollocationDao
import com.example.data.local.InitialData
import com.example.data.model.Collocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CollocationRepository(private val collocationDao: CollocationDao) {

    suspend fun checkAndPrepopulate() {
        withContext(Dispatchers.IO) {
            val existingList = collocationDao.getAllCollocationsList()
            Log.d("CollocationRepository", "Database existing count: ${existingList.size}")
            
            if (existingList.isEmpty()) {
                Log.d("CollocationRepository", "Database is empty. Populating with InitialData...")
                collocationDao.insertCollocations(InitialData.list)
                return@withContext
            }
            
            val existingGrouped = existingList.groupBy { it.english.trim().lowercase() }
            val initialMapped = InitialData.list.associateBy { it.english.trim().lowercase() }
            
            val toDeleteIds = mutableListOf<Int>()
            val toUpdate = mutableListOf<Collocation>()
            val toInsert = mutableListOf<Collocation>()
            
            // 1. Process existing items to resolve duplicates, update content, and delete outdated
            existingGrouped.forEach { (lowercaseEnglish, group) ->
                val initialMatch = initialMapped[lowercaseEnglish]
                if (initialMatch == null) {
                    // Outdated item, not in InitialData anymore. Delete all
                    group.forEach { toDeleteIds.add(it.id) }
                } else {
                    // Item exists in InitialData. Resolve duplicates if any
                    val bestEntry = if (group.size > 1) {
                        // Find the one with most progress or assigned date
                        val sorted = group.sortedWith(
                            compareByDescending<Collocation> { it.boxIndex }
                                .thenByDescending { it.assignedDate != null }
                                .thenByDescending { it.id }
                        )
                        // Keep the first (best) one, delete others
                        sorted.first().also { keep ->
                            sorted.drop(1).forEach { dup -> toDeleteIds.add(dup.id) }
                        }
                    } else {
                        group.first()
                    }
                    
                    // Update content fields to latest InitialData values, keeping Leitner progress
                    val updated = bestEntry.copy(
                        persian = initialMatch.persian,
                        pronunciation = initialMatch.pronunciation,
                        level = initialMatch.level,
                        example = initialMatch.example,
                        exampleTranslation = initialMatch.exampleTranslation,
                        category = initialMatch.category
                    )
                    
                    // Only update in DB if something actually changed
                    if (updated != bestEntry) {
                        toUpdate.add(updated)
                    }
                }
            }
            
            // 2. Identify missing items from InitialData to insert
            InitialData.list.forEach { card ->
                val lowercaseEnglish = card.english.trim().lowercase()
                if (!existingGrouped.containsKey(lowercaseEnglish)) {
                    toInsert.add(card)
                }
            }
            
            // 3. Execute DB operations
            if (toDeleteIds.isNotEmpty()) {
                Log.d("CollocationRepository", "Repair: Deleting ${toDeleteIds.size} duplicate/outdated collocations")
                toDeleteIds.forEach { collocationDao.deleteCollocationById(it) }
            }
            
            if (toUpdate.isNotEmpty()) {
                Log.d("CollocationRepository", "Repair: Updating content of ${toUpdate.size} existing collocations")
                collocationDao.insertCollocations(toUpdate)
            }
            
            if (toInsert.isNotEmpty()) {
                Log.d("CollocationRepository", "Repair: Inserting ${toInsert.size} missing collocations")
                collocationDao.insertCollocations(toInsert)
            }
            
            Log.d("CollocationRepository", "Repair and migration complete.")
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
            val startOfToday = calendar.timeInMillis
            collocationDao.resetReviewedTodayStatus(startOfToday)
            Log.d("CollocationRepository", "Reset isReviewedToday for cards reviewed before $startOfToday")
        }
    }

    fun getAllCollocations(): Flow<List<Collocation>> = collocationDao.getAllCollocations()

    fun getCollocationsByCategory(category: String): Flow<List<Collocation>> =
        collocationDao.getCollocationsByCategory(category)

    fun getAssignedNewCards(category: String, todayDate: String): Flow<List<Collocation>> =
        collocationDao.getAssignedCollocationsByCategory(category, todayDate)

    suspend fun getDueCards(category: String, currentTime: Long): List<Collocation> {
        return withContext(Dispatchers.IO) {
            collocationDao.getDueCollocationsByCategory(category, currentTime)
        }
    }

    /**
     * Prepares today's 10 new cards for a given category.
     * Keeps track of already assigned ones on today's date so they don't change or regenerate.
     */
    suspend fun prepareDailyNewCards(category: String, todayDate: String) {
        withContext(Dispatchers.IO) {
            val alreadyAssigned = collocationDao.getAssignedCollocationsByCategoryNonFlow(category, todayDate)
            val neededCount = 10 - alreadyAssigned.size
            
            if (neededCount > 0) {
                // Fetch unstudied cards for this category
                val unstudied = collocationDao.getUnstudiedCollocationsByCategory(category)
                val toAssign = unstudied.take(neededCount)
                
                if (toAssign.isNotEmpty()) {
                    val updated = toAssign.map { card ->
                        card.copy(
                            assignedDate = todayDate,
                            boxIndex = 1, // Study starts in Box 1
                            nextReviewTime = System.currentTimeMillis(), // Due immediately
                            isReviewedToday = false
                        )
                    }
                    collocationDao.insertCollocations(updated)
                    Log.d("CollocationRepository", "Assigned ${updated.size} new cards for $category today ($todayDate)")
                }
            }
        }
    }

    suspend fun updateCardState(collocation: Collocation) {
        withContext(Dispatchers.IO) {
            collocationDao.updateCollocation(collocation)
        }
    }

    /**
     * Handles card evaluation in Leitner Box System
     */
    suspend fun rateCard(collocation: Collocation, rating: String) {
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        
        val updatedCard = when (rating) {
            "EASY" -> { // "یاد گرفتم" (Learned it / Green)
                val nextBox = collocation.boxIndex + 1
                if (nextBox > 5) {
                    // Fully mastered
                    collocation.copy(
                        boxIndex = 5,
                        nextReviewTime = Long.MAX_VALUE, // Infinity, no more review
                        lastReviewedTime = currentTime,
                        isReviewedToday = true
                    )
                } else {
                    val interval = when (nextBox) {
                        2 -> 1 * oneDayMs
                        3 -> 3 * oneDayMs
                        4 -> 7 * oneDayMs
                        5 -> 14 * oneDayMs
                        else -> 1 * oneDayMs
                    }
                    collocation.copy(
                        boxIndex = nextBox,
                        nextReviewTime = currentTime + interval,
                        lastReviewedTime = currentTime,
                        isReviewedToday = true
                    )
                }
            }
            "MEDIUM" -> { // "سخت بود" (Was hard / Orange)
                // Stays in same box, scheduled closer (e.g. tomorrow)
                collocation.copy(
                    nextReviewTime = currentTime + (oneDayMs / 2), // 12 hours from now
                    lastReviewedTime = currentTime,
                    isReviewedToday = true
                )
            }
            else -> { // "EASY" or "HARD" - "بلد نبودم" (Didn't know / Red)
                // Moves back to Box 1, scheduled for tomorrow
                collocation.copy(
                    boxIndex = 1,
                    nextReviewTime = currentTime + oneDayMs,
                    lastReviewedTime = currentTime,
                    isReviewedToday = true
                )
            }
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
            Log.d("CollocationRepository", "Successfully reset progress on ${resetCards.size} collocations")
        }
    }
}
