package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Collocation
import kotlinx.coroutines.flow.Flow

@Dao
interface CollocationDao {
    @Query("SELECT * FROM collocations ORDER BY id ASC")
    fun getAllCollocations(): Flow<List<Collocation>>

    @Query("SELECT * FROM collocations WHERE category = :category ORDER BY id ASC")
    fun getCollocationsByCategory(category: String): Flow<List<Collocation>>

    @Query("SELECT * FROM collocations WHERE id = :id LIMIT 1")
    suspend fun getCollocationById(id: Int): Collocation?

    @Query("SELECT COUNT(*) FROM collocations")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollocations(collocations: List<Collocation>)

    @Update
    suspend fun updateCollocation(collocation: Collocation)

    // Unstudied cards are where boxIndex == 0 AND assignedDate IS NULL
    @Query("SELECT * FROM collocations WHERE category = :category AND boxIndex = 0 AND assignedDate IS NULL ORDER BY id ASC")
    suspend fun getUnstudiedCollocationsByCategory(category: String): List<Collocation>

    // Today's new assigned cards (assigned for study on today's date)
    @Query("SELECT * FROM collocations WHERE category = :category AND assignedDate = :date ORDER BY id ASC")
    fun getAssignedCollocationsByCategory(category: String, date: String): Flow<List<Collocation>>

    @Query("SELECT * FROM collocations WHERE category = :category AND assignedDate = :date ORDER BY id ASC")
    suspend fun getAssignedCollocationsByCategoryNonFlow(category: String, date: String): List<Collocation>

    // Cards due for review (boxIndex > 0 AND nextReviewTime <= :currentTime)
    @Query("SELECT * FROM collocations WHERE category = :category AND boxIndex > 0 AND nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC")
    suspend fun getDueCollocationsByCategory(category: String, currentTime: Long): List<Collocation>

    @Query("SELECT * FROM collocations ORDER BY id ASC")
    suspend fun getAllCollocationsList(): List<Collocation>

    @Query("DELETE FROM collocations WHERE id = :id")
    suspend fun deleteCollocationById(id: Int)

    @Query("UPDATE collocations SET isReviewedToday = 0 WHERE isReviewedToday = 1 AND lastReviewedTime < :startOfToday")
    suspend fun resetReviewedTodayStatus(startOfToday: Long)
}
