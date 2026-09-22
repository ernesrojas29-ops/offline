package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.StoryEntity
import com.example.data.repository.StoryRepository
import com.example.data.scraper.CategoryItem
import com.example.data.scraper.ScrapingProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: StoryRepository
) : ViewModel() {

    // --- ESTADO DE CATEGORÍAS Y DESCARGA ---
    private val _availableCategories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val availableCategories: StateFlow<List<CategoryItem>> = _availableCategories.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<String>> = _selectedCategoryIds.asStateFlow()

    private val _isCategoriesLoading = MutableStateFlow(false)
    val isCategoriesLoading: StateFlow<Boolean> = _isCategoriesLoading.asStateFlow()

    private val _scrapingProgress = MutableStateFlow(ScrapingProgress())
    val scrapingProgress: StateFlow<ScrapingProgress> = _scrapingProgress.asStateFlow()

    private var syncJob: Job? = null

    // --- ESTADO DE BIBLIOTECA OFFLINE ---
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

    // Lista reactiva de relatos filtrados por categoría, búsqueda y switches
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

    // Ajustes de lectura nocturna
    private val _readerFontSize = MutableStateFlow(18) // Sp
    val readerFontSize: StateFlow<Int> = _readerFontSize.asStateFlow()

    private val _isAmoledBlack = MutableStateFlow(true) // Por defecto fondo negro puro AMOLED #000000
    val isAmoledBlack: StateFlow<Boolean> = _isAmoledBlack.asStateFlow()

    init {
        loadAvailableCategories()
    }

    fun loadAvailableCategories() {
        viewModelScope.launch {
            _isCategoriesLoading.value = true
            try {
                val list = repository.fetchAvailableCategories()
                _availableCategories.value = list
                // Seleccionar las primeras 3 por conveniencia
                if (_selectedCategoryIds.value.isEmpty()) {
                    _selectedCategoryIds.value = list.take(3).map { it.id }.toSet()
                }
            } catch (e: Exception) {
                // El servicio ya provee lista predefinida si falla
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

    fun startSync() {
        val selected = _availableCategories.value.filter { _selectedCategoryIds.value.contains(it.id) }
        if (selected.isEmpty()) return

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            repository.syncCategories(selected).collect { progress ->
                _scrapingProgress.value = progress
            }
        }
    }

    fun cancelSync() {
        syncJob?.cancel()
        _scrapingProgress.value = _scrapingProgress.value.copy(
            isRunning = false,
            statusMessage = "Sincronización detenida por el usuario."
        )
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

    // --- ACCIONES DEL LECTOR ---
    fun openStory(id: String) {
        _currentStoryId.value = id
        // Marcar como leído automáticamente al abrir
        viewModelScope.launch {
            repository.setRead(id, true)
        }
    }

    fun closeReader() {
        _currentStoryId.value = null
    }

    fun increaseFontSize() {
        if (_readerFontSize.value < 32) {
            _readerFontSize.value += 2
        }
    }

    fun decreaseFontSize() {
        if (_readerFontSize.value > 12) {
            _readerFontSize.value -= 2
        }
    }

    fun toggleAmoledMode() {
        _isAmoledBlack.value = !_isAmoledBlack.value
    }

    class Factory(private val repository: StoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
