package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Collocation
import com.example.data.repository.CollocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeitnerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CollocationRepository
    
    // Categories List
    val categories = listOf(
        "محیط زیست",
        "اقتصاد",
        "آموزش",
        "سلامت",
        "فناوری",
        "جامعه",
        "سفر و گردشگری",
        "کار و تجارت",
        "علم و پژوهش",
        "دولت و قانون"
    )

    // States
    private val _selectedCategory = MutableStateFlow("محیط زیست")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _showAnswer = MutableStateFlow(false)
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    private val _sessionIndex = MutableStateFlow(0)
    val sessionIndex: StateFlow<Int> = _sessionIndex.asStateFlow()

    private val _isRatingInProgress = MutableStateFlow(false)
    val isRatingInProgress: StateFlow<Boolean> = _isRatingInProgress.asStateFlow()

    // Helper to get formatted today's date
    val todayDateString: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CollocationRepository(database.collocationDao())
        
        viewModelScope.launch {
            // Ensure database is populated on startup
            repository.checkAndPrepopulate()
            // Make sure new cards for initial category are prepared
            prepareSession()
        }
    }

    // Combine all database collocations for stats
    val allCollocations: StateFlow<List<Collocation>> = repository.getAllCollocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow of collocations in the currently selected category
    val currentCategoryCollocations: StateFlow<List<Collocation>> = combine(
        allCollocations,
        _selectedCategory
    ) { list, category ->
        list.filter { it.category == category }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's assigned new cards for the selected category
    val assignedNewCardsToday: StateFlow<List<Collocation>> = combine(
        currentCategoryCollocations,
        _selectedCategory
    ) { list, _ ->
        list.filter { it.assignedDate == todayDateString }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Old review cards that are currently due
    private val _dueCardsToday = MutableStateFlow<List<Collocation>>(emptyList())
    val dueCardsToday: StateFlow<List<Collocation>> = _dueCardsToday.asStateFlow()

    // The active study deck combining unreviewed new cards today and due cards
    val activeStudySessionDeck: StateFlow<List<Collocation>> = combine(
        assignedNewCardsToday,
        dueCardsToday
    ) { newCards, dueCards ->
        // Session includes:
        // 1. New cards today that have not been rated/reviewed yet
        val pendingNewCards = newCards.filter { !it.isReviewedToday }
        // 2. Older cards whose review interval is due
        val pendingDueCards = dueCards.filter { !it.isReviewedToday }
        
        pendingNewCards + pendingDueCards
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Count of boxes for current category
    val boxCounts: StateFlow<Map<Int, Int>> = currentCategoryCollocations
        .combine(_selectedCategory) { list, _ ->
            val counts = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
            list.forEach { card ->
                if (card.boxIndex in 1..5) {
                    counts[card.boxIndex] = counts.getOrDefault(card.boxIndex, 0) + 1
                }
            }
            counts
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
        )

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _sessionIndex.value = 0
        _showAnswer.value = false
        viewModelScope.launch {
            prepareSession()
        }
    }

    private suspend fun prepareSession() {
        // Step 0: Reset isReviewedToday for cards whose review cycle was completed on previous days
        repository.resetReviewedTodayIfNeeded()
        // Step 1: Assign today's 10 new cards if not already done
        repository.prepareDailyNewCards(_selectedCategory.value, todayDateString)
        // Step 2: Fetch any older cards due for review
        val due = repository.getDueCards(_selectedCategory.value, System.currentTimeMillis())
        _dueCardsToday.value = due
    }

    fun toggleShowAnswer() {
        _showAnswer.value = !_showAnswer.value
    }

    fun rateCard(collocation: Collocation, rating: String) {
        viewModelScope.launch {
            _isRatingInProgress.value = true
            try {
                repository.rateCard(collocation, rating)
                _showAnswer.value = false
                
                // Advance the session index if we still have cards left
                val currentDeckSize = activeStudySessionDeck.value.size
                if (currentDeckSize <= 1) {
                    // If this was the last card, we reset index
                    _sessionIndex.value = 0
                } else {
                    // Keep the index bounded
                    if (_sessionIndex.value >= currentDeckSize - 1) {
                        _sessionIndex.value = 0
                    }
                }
                
                // Refresh the session due list
                val due = repository.getDueCards(_selectedCategory.value, System.currentTimeMillis())
                _dueCardsToday.value = due
            } finally {
                _isRatingInProgress.value = false
            }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            _sessionIndex.value = 0
            _showAnswer.value = false
            prepareSession()
        }
    }

    fun simulateNewDay() {
        viewModelScope.launch {
            // For testing and demonstration, let's clear the today's assignedDate and isReviewedToday on all cards
            // so they can be reviewed again or assigned as new day cards.
            // But actually we can simulate by changing today's cards to yesterday's,
            // allowing us to request another 10 new cards.
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
            val currentCards = allCollocations.value
            currentCards.forEach { card ->
                if (card.assignedDate == todayDateString) {
                    repository.updateCardState(card.copy(assignedDate = yesterday, isReviewedToday = true))
                }
            }
            _sessionIndex.value = 0
            _showAnswer.value = false
            prepareSession()
        }
    }
}
