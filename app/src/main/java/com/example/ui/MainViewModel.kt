package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.StoryEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.StoryRepository
import com.example.data.scraper.CategoryItem
import com.example.data.scraper.ScrapingProgress
import com.example.worker.SyncStoriesWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val context: Context,
    private val repository: StoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    // --- ESTADO DE CATEGORÍAS Y SINCRONIZACIÓN WORKMANAGER ---
    private val _availableCategories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val availableCategories: StateFlow<List<CategoryItem>> = _availableCategories.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<String>> = _selectedCategoryIds.asStateFlow()

    private val _isCategoriesLoading = MutableStateFlow(false)
    val isCategoriesLoading: StateFlow<Boolean> = _isCategoriesLoading.asStateFlow()

    // Observar progreso real desde WorkManager
    val scrapingProgress: StateFlow<ScrapingProgress> = workManager
        .getWorkInfosForUniqueWorkFlow(SyncStoriesWorker.UNIQUE_WORK_NAME)
        .map { workInfoList ->
            val workInfo = workInfoList.firstOrNull() ?: return@map ScrapingProgress()

            val progressData = workInfo.progress
            val outputData = workInfo.outputData

            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    ScrapingProgress(
                        isRunning = true,
                        currentCategory = progressData.getString(SyncStoriesWorker.PROGRESS_CATEGORY) ?: "",
                        currentStoryTitle = progressData.getString(SyncStoriesWorker.PROGRESS_STORY_TITLE) ?: "",
                        downloadedCount = progressData.getInt(SyncStoriesWorker.PROGRESS_DOWNLOADED, 0),
                        skippedDueToDurationCount = progressData.getInt(SyncStoriesWorker.PROGRESS_SKIPPED, 0),
                        totalProcessed = progressData.getInt(SyncStoriesWorker.PROGRESS_TOTAL, 0),
                        statusMessage = progressData.getString(SyncStoriesWorker.PROGRESS_STATUS_MSG) ?: "Descargando en segundo plano...",
                        isFinished = false
                    )
                }
                WorkInfo.State.SUCCEEDED -> {
                    val downloaded = outputData.getInt(SyncStoriesWorker.PROGRESS_DOWNLOADED, 0)
                    val skipped = outputData.getInt(SyncStoriesWorker.PROGRESS_SKIPPED, 0)
                    ScrapingProgress(
                        isRunning = false,
                        isFinished = true,
                        downloadedCount = downloaded,
                        skippedDueToDurationCount = skipped,
                        statusMessage = outputData.getString(SyncStoriesWorker.PROGRESS_STATUS_MSG)
                            ?: "Sincronización completada. $downloaded relatos guardados."
                    )
                }
                WorkInfo.State.FAILED -> {
                    ScrapingProgress(
                        isRunning = false,
                        isFinished = true,
                        error = outputData.getString(SyncStoriesWorker.PROGRESS_STATUS_MSG) ?: "Error en la sincronización.",
                        statusMessage = "Fallo en la sincronización."
                    )
                }
                WorkInfo.State.CANCELLED -> {
                    ScrapingProgress(
                        isRunning = false,
                        isFinished = false,
                        statusMessage = "Sincronización cancelada por el usuario."
                    )
                }
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                    ScrapingProgress(
                        isRunning = true,
                        statusMessage = "En cola para iniciar descarga..."
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScrapingProgress())

    // --- ESTADO DE BIBLIOTECA OFFLINE (ROOM) ---
    private val _selectedCategoryFilter = MutableStateFlow("Todas")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyFavorites = MutableStateFlow(false)
    val filterOnlyFavorites: StateFlow<Boolean> = _filterOnlyFavorites.asStateFlow()

    private val _filterOnlyUnread = MutableStateFlow(false)
    val filterOnlyUnread: StateFlow<Boolean> = _filterOnlyUnread.asStateFlow()

    val savedCategories: StateFlow<List<String>> = repository.savedCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavedStoriesCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val displayedStories: StateFlow<List<StoryEntity>> = combine(
        _selectedCategoryFilter,
        _searchQuery
    ) { category, query ->
        category to query
    }.flatMapLatest { (category, query) ->
        repository.searchStories(query, category)
    }.combine(_filterOnlyFavorites) { list, onlyFavs ->
        if (onlyFavs) list.filter { it.isFavorite } else list
    }.combine(_filterOnlyUnread) { list, onlyUnread ->
        if (onlyUnread) list.filter { !it.isRead } else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ESTADO DEL LECTOR ---
    private val _currentStoryId = MutableStateFlow<String?>(null)
    val currentStoryId: StateFlow<String?> = _currentStoryId.asStateFlow()

    val activeStory: StateFlow<StoryEntity?> = _currentStoryId.flatMapLatest { id ->
        if (id != null) repository.getStoryById(id) else MutableStateFlow(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Ajustes persistidos en Jetpack DataStore Preferences
    val readerFontSize: StateFlow<Int> = preferencesRepository.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_FONT_SIZE)

    val isAmoledBlack: StateFlow<Boolean> = preferencesRepository.isAmoledThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_AMOLED_THEME)

    init {
        loadAvailableCategories()
    }

    fun loadAvailableCategories() {
        viewModelScope.launch {
            _isCategoriesLoading.value = true
            try {
                val list = repository.fetchAvailableCategories()
                _availableCategories.value = list
                if (_selectedCategoryIds.value.isEmpty()) {
                    _selectedCategoryIds.value = list.take(3).map { it.id }.toSet()
                }
            } catch (e: Exception) {
                // Notificar en la interfaz sin datos falsos
            } finally {
                _isCategoriesLoading.value = false
            }
        }
    }

    fun toggleCategorySelection(categoryId: String) {
        val current = _selectedCategoryIds.value.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _selectedCategoryIds.value = current
    }

    fun selectAllCategories() {
        _selectedCategoryIds.value = _availableCategories.value.map { it.id }.toSet()
    }

    fun deselectAllCategories() {
        _selectedCategoryIds.value = emptySet()
    }

    /**
     * Inicia la sincronización delegándola a WorkManager.
     * Si el proceso muere, la descarga continúa o se reanuda de forma segura.
     */
    fun startSync() {
        val selected = _availableCategories.value.filter { _selectedCategoryIds.value.contains(it.id) }
        if (selected.isEmpty()) return

        val inputData = workDataOf(
            SyncStoriesWorker.KEY_CATEGORY_IDS to selected.map { it.id }.toTypedArray(),
            SyncStoriesWorker.KEY_CATEGORY_NAMES to selected.map { it.name }.toTypedArray(),
            SyncStoriesWorker.KEY_CATEGORY_URLS to selected.map { it.url }.toTypedArray(),
            SyncStoriesWorker.KEY_MAX_PER_CAT to 8
        )

        val workRequest = OneTimeWorkRequestBuilder<SyncStoriesWorker>()
            .setInputData(inputData)
            .addTag(SyncStoriesWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(
            SyncStoriesWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelSync() {
        workManager.cancelUniqueWork(SyncStoriesWorker.UNIQUE_WORK_NAME)
    }

    // --- ACCIONES DE BIBLIOTECA ---
    fun selectCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterFavorites() {
        _filterOnlyFavorites.value = !_filterOnlyFavorites.value
    }

    fun toggleFilterUnread() {
        _filterOnlyUnread.value = !_filterOnlyUnread.value
    }

    fun toggleFavorite(story: StoryEntity) {
        viewModelScope.launch {
            repository.setFavorite(story.id, !story.isFavorite)
        }
    }

    fun toggleRead(story: StoryEntity) {
        viewModelScope.launch {
            repository.setRead(story.id, !story.isRead)
        }
    }

    fun deleteStory(id: String) {
        viewModelScope.launch {
            repository.deleteStory(id)
        }
    }

    fun deleteAllStories() {
        viewModelScope.launch {
            repository.deleteAllStories()
        }
    }

    // --- ACCIONES DEL LECTOR Y DATASTORE ---
    fun openStory(id: String) {
        _currentStoryId.value = id
        viewModelScope.launch {
            repository.setRead(id, true)
        }
    }

    fun closeReader() {
        _currentStoryId.value = null
    }

    fun increaseFontSize() {
        viewModelScope.launch {
            val current = readerFontSize.value
            if (current < 32) {
                preferencesRepository.setFontSize(current + 2)
            }
        }
    }

    fun decreaseFontSize() {
        viewModelScope.launch {
            val current = readerFontSize.value
            if (current > 12) {
                preferencesRepository.setFontSize(current - 2)
            }
        }
    }

    fun toggleAmoledMode() {
        viewModelScope.launch {
            preferencesRepository.setAmoledTheme(!isAmoledBlack.value)
        }
    }

    class Factory(
        private val context: Context,
        private val repository: StoryRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(context.applicationContext, repository, preferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
