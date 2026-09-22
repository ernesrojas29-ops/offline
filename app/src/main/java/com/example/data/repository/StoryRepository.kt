package com.example.data.repository

import android.util.Log
import com.example.data.local.StoryDao
import com.example.data.local.StoryEntity
import com.example.data.scraper.CategoryItem
import com.example.data.scraper.ScraperService
import com.example.data.scraper.ScrapingException
import com.example.data.scraper.ScrapingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio centralizado para el acceso a datos de relatos.
 * Conecta StoryDao (Room) y ScraperService (Web Scraping real con Jsoup).
 */
class StoryRepository(
    private val storyDao: StoryDao,
    private val scraperService: ScraperService
) {

    // Flujos reactivos directos desde Room
    val allStories: Flow<List<StoryEntity>> = storyDao.getAllStories()
    val savedCategories: Flow<List<String>> = storyDao.getSavedCategories()
    val totalCount: Flow<Int> = storyDao.getTotalStoriesCount()
    val favoriteStories: Flow<List<StoryEntity>> = storyDao.getFavoriteStories()
    val unreadStories: Flow<List<StoryEntity>> = storyDao.getUnreadStories()

    fun getStoriesByCategory(category: String): Flow<List<StoryEntity>> {
        return if (category.equals("Todas", ignoreCase = true) || category.isBlank()) {
            storyDao.getAllStories()
        } else {
            storyDao.getStoriesByCategory(category)
        }
    }

    fun searchStories(query: String, category: String?): Flow<List<StoryEntity>> {
        val trimmed = query.trim()
        val isAllCategory = category == null || category.equals("Todas", ignoreCase = true) || category.isBlank()

        return if (trimmed.isBlank()) {
            if (isAllCategory) storyDao.getAllStories() else storyDao.getStoriesByCategory(category!!)
        } else {
            if (isAllCategory) storyDao.searchStories(trimmed) else storyDao.searchStoriesByCategory(category!!, trimmed)
        }
    }

    fun getStoryById(id: String): Flow<StoryEntity?> = storyDao.getStoryById(id)

    suspend fun setFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        storyDao.setFavorite(id, isFavorite)
    }

    suspend fun setRead(id: String, isRead: Boolean) = withContext(Dispatchers.IO) {
        storyDao.setRead(id, isRead)
    }

    suspend fun deleteStory(id: String) = withContext(Dispatchers.IO) {
        storyDao.deleteStory(id)
    }

    suspend fun deleteAllStories() = withContext(Dispatchers.IO) {
        storyDao.deleteAllStories()
    }

    suspend fun insertStories(stories: List<StoryEntity>): List<Long> = withContext(Dispatchers.IO) {
        storyDao.insertStories(stories)
    }

    suspend fun fetchAvailableCategories(): List<CategoryItem> = withContext(Dispatchers.IO) {
        scraperService.fetchCategories()
    }

    /**
     * Sincronización real con movil.todorelatos.com:
     * - Pausa anti-baneo de 1200ms entre descargas (delay(1200)).
     * - Filtro estricto: Descarte automático de relatos con duración > 25 minutos.
     * - Inserción inmediata en Room para persistencia offline garantizada.
     * - Sin datos simulados: propagación de progreso y errores reales.
     */
    fun syncCategories(
        categories: List<CategoryItem>,
        maxStoriesPerCategory: Int = 8
    ): Flow<ScrapingProgress> = flow {
        var downloadedTotal = 0
        var skippedTotal = 0
        var processedTotal = 0

        emit(
            ScrapingProgress(
                isRunning = true,
                statusMessage = "Conectando a movil.todorelatos.com...",
                totalProcessed = 0
            )
        )

        for (cat in categories) {
            emit(
                ScrapingProgress(
                    isRunning = true,
                    currentCategory = cat.name,
                    downloadedCount = downloadedTotal,
                    skippedDueToDurationCount = skippedTotal,
                    totalProcessed = processedTotal,
                    statusMessage = "Explorando categoría: ${cat.name}..."
                )
            )

            try {
                val summaries = scraperService.fetchStoriesForCategory(
                    categoryUrl = cat.url,
                    categoryName = cat.name,
                    maxStoriesToFetch = maxStoriesPerCategory
                )

                if (summaries.isEmpty()) {
                    emit(
                        ScrapingProgress(
                            isRunning = true,
                            currentCategory = cat.name,
                            downloadedCount = downloadedTotal,
                            skippedDueToDurationCount = skippedTotal,
                            totalProcessed = processedTotal,
                            statusMessage = "Sin nuevos relatos en ${cat.name}."
                        )
                    )
                    continue
                }

                for (summary in summaries) {
                    processedTotal++

                    // Filtro previo por tarjeta
                    if (summary.durationMinutes > ScraperService.MAX_ALLOWED_DURATION_MINUTES) {
                        skippedTotal++
                        emit(
                            ScrapingProgress(
                                isRunning = true,
                                currentCategory = cat.name,
                                currentStoryTitle = summary.title,
                                downloadedCount = downloadedTotal,
                                skippedDueToDurationCount = skippedTotal,
                                totalProcessed = processedTotal,
                                statusMessage = "Omitido (>25 min): '${summary.title}' (${summary.durationMinutes} min)"
                            )
                        )
                        continue
                    }

                    emit(
                        ScrapingProgress(
                            isRunning = true,
                            currentCategory = cat.name,
                            currentStoryTitle = summary.title,
                            downloadedCount = downloadedTotal,
                            skippedDueToDurationCount = skippedTotal,
                            totalProcessed = processedTotal,
                            statusMessage = "Descargando: '${summary.title}'..."
                        )
                    )

                    // Pausa anti-baneo obligatoria de 1200 ms
                    delay(ScraperService.ANTI_BAN_DELAY_MS)

                    try {
                        val storyEntity = scraperService.fetchStoryContent(summary, cat.name)

                        if (storyEntity != null) {
                            if (storyEntity.durationMinutes <= ScraperService.MAX_ALLOWED_DURATION_MINUTES) {
                                storyDao.insertStory(storyEntity)
                                downloadedTotal++

                                emit(
                                    ScrapingProgress(
                                        isRunning = true,
                                        currentCategory = cat.name,
                                        currentStoryTitle = storyEntity.title,
                                        downloadedCount = downloadedTotal,
                                        skippedDueToDurationCount = skippedTotal,
                                        totalProcessed = processedTotal,
                                        statusMessage = "Guardado localmente: '${storyEntity.title}' (${storyEntity.durationMinutes} min)"
                                    )
                                )
                            } else {
                                skippedTotal++
                                emit(
                                    ScrapingProgress(
                                        isRunning = true,
                                        currentCategory = cat.name,
                                        currentStoryTitle = storyEntity.title,
                                        downloadedCount = downloadedTotal,
                                        skippedDueToDurationCount = skippedTotal,
                                        totalProcessed = processedTotal,
                                        statusMessage = "Omitido (>25 min): '${storyEntity.title}' (${storyEntity.durationMinutes} min)"
                                    )
                                )
                            }
                        } else {
                            skippedTotal++
                            emit(
                                ScrapingProgress(
                                    isRunning = true,
                                    currentCategory = cat.name,
                                    currentStoryTitle = summary.title,
                                    downloadedCount = downloadedTotal,
                                    skippedDueToDurationCount = skippedTotal,
                                    totalProcessed = processedTotal,
                                    statusMessage = "Omitido (>25 min): '${summary.title}'"
                                )
                            )
                        }
                    } catch (e: ScrapingException) {
                        Log.w("StoryRepository", "Error descargando '${summary.title}': ${e.message}")
                        emit(
                            ScrapingProgress(
                                isRunning = true,
                                currentCategory = cat.name,
                                currentStoryTitle = summary.title,
                                downloadedCount = downloadedTotal,
                                skippedDueToDurationCount = skippedTotal,
                                totalProcessed = processedTotal,
                                statusMessage = "Error en '${summary.title}': ${e.localizedMessage}"
                            )
                        )
                    }
                }
            } catch (e: ScrapingException) {
                Log.e("StoryRepository", "Error procesando categoría ${cat.name}: ${e.message}")
                emit(
                    ScrapingProgress(
                        isRunning = true,
                        currentCategory = cat.name,
                        downloadedCount = downloadedTotal,
                        skippedDueToDurationCount = skippedTotal,
                        totalProcessed = processedTotal,
                        statusMessage = "Error en ${cat.name}: ${e.localizedMessage}"
                    )
                )
            }
        }

        emit(
            ScrapingProgress(
                isRunning = false,
                isFinished = true,
                downloadedCount = downloadedTotal,
                skippedDueToDurationCount = skippedTotal,
                totalProcessed = processedTotal,
                statusMessage = "Sincronización completada. $downloadedTotal relatos guardados, $skippedTotal omitidos (> 25 min)."
            )
        )
    }.flowOn(Dispatchers.IO)
}
