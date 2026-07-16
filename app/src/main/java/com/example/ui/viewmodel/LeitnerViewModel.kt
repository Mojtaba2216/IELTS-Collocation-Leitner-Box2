package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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

class LeitnerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CollocationRepository
    private val preferences = application.getSharedPreferences(
        "leitner_study_day_progress",
        Context.MODE_PRIVATE
    )

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

    private val _selectedCategory = MutableStateFlow("محیط زیست")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStudyDay = MutableStateFlow(1)
    val selectedStudyDay: StateFlow<Int> = _selectedStudyDay.asStateFlow()

    private val _unlockedStudyDay = MutableStateFlow(1)
    val unlockedStudyDay: StateFlow<Int> = _unlockedStudyDay.asStateFlow()

    private val _completedStudyDays = MutableStateFlow<Set<Int>>(emptySet())
    val completedStudyDays: StateFlow<Set<Int>> = _completedStudyDays.asStateFlow()

    private val _showAnswer = MutableStateFlow(false)
    val showAnswer: StateFlow<Boolean> = _showAnswer.asStateFlow()

    private val _sessionIndex = MutableStateFlow(0)
    val sessionIndex: StateFlow<Int> = _sessionIndex.asStateFlow()

    private val _isRatingInProgress = MutableStateFlow(false)
    val isRatingInProgress: StateFlow<Boolean> = _isRatingInProgress.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CollocationRepository(database.collocationDao())

        viewModelScope.launch {
            repository.checkAndPrepopulate()
            restoreSelectedDayForCurrentCategory()
            prepareSession()
        }
    }

    val allCollocations: StateFlow<List<Collocation>> = repository.getAllCollocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    /**
     * The 10 cards assigned to the selected learning day (day 1 to day 5).
     * The old property name is kept so the rest of the UI remains compatible.
     */
    val assignedNewCardsToday: StateFlow<List<Collocation>> = combine(
        currentCategoryCollocations,
        _selectedStudyDay
    ) { list, day ->
        val assignmentKey = CollocationRepository.studyDayKey(day)
        list.filter { it.assignedDate == assignmentKey }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _dueCardsToday = MutableStateFlow<List<Collocation>>(emptyList())
    val dueCardsToday: StateFlow<List<Collocation>> = _dueCardsToday.asStateFlow()

    val activeStudySessionDeck: StateFlow<List<Collocation>> = combine(
        assignedNewCardsToday,
        dueCardsToday
    ) { newCards, dueCards ->
        val pendingNewCards = newCards.filter { it.lastReviewedTime == 0L }
        val pendingDueCards = dueCards.filter { !it.isReviewedToday }
        (pendingNewCards + pendingDueCards).distinctBy { it.id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
        if (category == _selectedCategory.value) return

        viewModelScope.launch {
            _selectedCategory.value = category
            _sessionIndex.value = 0
            _showAnswer.value = false
            restoreSelectedDayForCurrentCategory()
            prepareSession()
        }
    }

    fun selectStudyDay(day: Int) {
        viewModelScope.launch {
            refreshStudyDayProgress()
            val safeDay = day.coerceIn(1, 5)
            if (safeDay > _unlockedStudyDay.value) return@launch

            _selectedStudyDay.value = safeDay
            persistSelectedDay(_selectedCategory.value, safeDay)
            _sessionIndex.value = 0
            _showAnswer.value = false
            prepareSession()
        }
    }

    private suspend fun restoreSelectedDayForCurrentCategory() {
        refreshStudyDayProgress()
        val category = _selectedCategory.value
        val savedDay = preferences.getInt(dayPreferenceKey(category), _unlockedStudyDay.value)
        val safeDay = savedDay.coerceIn(1, _unlockedStudyDay.value.coerceAtMost(5))
        _selectedStudyDay.value = safeDay
        persistSelectedDay(category, safeDay)
    }

    private suspend fun prepareSession() {
        repository.resetReviewedTodayIfNeeded()
        refreshStudyDayProgress()

        if (_selectedStudyDay.value > _unlockedStudyDay.value) {
            _selectedStudyDay.value = _unlockedStudyDay.value
            persistSelectedDay(_selectedCategory.value, _selectedStudyDay.value)
        }

        repository.prepareStudyDayCards(
            category = _selectedCategory.value,
            studyDay = _selectedStudyDay.value
        )

        refreshStudyDayProgress()
        refreshDueCards()
    }

    private suspend fun refreshDueCards() {
        _dueCardsToday.value = repository.getDueCards(
            category = _selectedCategory.value,
            currentTime = System.currentTimeMillis()
        )
    }

    private suspend fun refreshStudyDayProgress() {
        val category = _selectedCategory.value
        val completedDays = mutableSetOf<Int>()

        for (day in 1..5) {
            val cards = repository.getStudyDayCards(category, day)
            if (cards.size == 10 && cards.all { it.lastReviewedTime > 0L }) {
                completedDays += day
            }
        }

        var unlockedDay = 1
        for (day in 1..4) {
            if (day in completedDays) {
                unlockedDay = day + 1
            } else {
                break
            }
        }

        _completedStudyDays.value = completedDays
        _unlockedStudyDay.value = unlockedDay.coerceIn(1, 5)
    }

    fun toggleShowAnswer() {
        _showAnswer.value = !_showAnswer.value
    }

    fun rateCard(collocation: Collocation, rating: String) {
        if (_isRatingInProgress.value) return

        viewModelScope.launch {
            _isRatingInProgress.value = true
            try {
                val category = _selectedCategory.value
                val studyDayBeforeRating = _selectedStudyDay.value
                val isNewCardFromSelectedDay =
                    collocation.assignedDate == CollocationRepository.studyDayKey(studyDayBeforeRating) &&
                        collocation.lastReviewedTime == 0L

                repository.rateCard(collocation, rating)
                _showAnswer.value = false
                _sessionIndex.value = 0

                refreshDueCards()
                refreshStudyDayProgress()

                if (isNewCardFromSelectedDay) {
                    val dayCards = repository.getStudyDayCards(category, studyDayBeforeRating)
                    val dayCompleted = dayCards.size == 10 && dayCards.all { it.lastReviewedTime > 0L }

                    if (dayCompleted && studyDayBeforeRating < 5) {
                        val nextDay = studyDayBeforeRating + 1
                        _selectedStudyDay.value = nextDay
                        persistSelectedDay(category, nextDay)
                        repository.prepareStudyDayCards(category, nextDay)
                        refreshStudyDayProgress()
                        refreshDueCards()
                    }
                }
            } finally {
                _isRatingInProgress.value = false
            }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            preferences.edit().clear().apply()
            _selectedStudyDay.value = 1
            _unlockedStudyDay.value = 1
            _completedStudyDays.value = emptySet()
            _sessionIndex.value = 0
            _showAnswer.value = false
            prepareSession()
        }
    }

    /**
     * Kept for backward compatibility. It now moves only to an unlocked day and
     * never rewrites dates or corrupts the Leitner history.
     */
    fun simulateNewDay() {
        val nextDay = (_selectedStudyDay.value + 1).coerceAtMost(5)
        selectStudyDay(nextDay)
    }

    private fun dayPreferenceKey(category: String): String =
        "selected_study_day_${category.hashCode()}"

    private fun persistSelectedDay(category: String, day: Int) {
        preferences.edit().putInt(dayPreferenceKey(category), day).apply()
    }
}
