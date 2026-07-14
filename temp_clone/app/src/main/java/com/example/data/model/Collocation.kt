package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collocations")
data class Collocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val english: String,
    val persian: String,
    val pronunciation: String,
    val level: String, // e.g. "B2", "C1"
    val example: String, // IELTS Writing Task 2 example
    val exampleTranslation: String, // Persian example translation
    val category: String, // 10 main categories
    
    // Leitner State
    val boxIndex: Int = 0, // 0 means unstudied. 1-5 represent Leitner boxes.
    val nextReviewTime: Long = 0, // epoch millis for review due
    val lastReviewedTime: Long = 0, // epoch millis of last review
    
    // Daily Study Limit tracking
    val assignedDate: String? = null, // e.g. "2026-07-13" to restrict to 10 new cards daily per category
    val isReviewedToday: Boolean = false // Track if the card has been rated today during study
) {
    val isMastered: Boolean get() = boxIndex == 5 && nextReviewTime == Long.MAX_VALUE
}
