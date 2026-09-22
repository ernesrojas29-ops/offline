package com.example.data.repository

import com.example.data.local.StoryDao
import com.example.data.local.StoryEntity
import com.example.data.scraper.CategoryItem
import com.example.data.scraper.ScraperService
import com.example.data.scraper.ScrapingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

class StoryRepository(
    private val storyDao: StoryDao,
    private val scraperService: ScraperService
) {

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

    suspend fun setFavorite(id: String, isFavorite: Boolean) {
        storyDao.setFavorite(id, isFavorite)
    }

    suspend fun setRead(id: String, isRead: Boolean) {
        storyDao.setRead(id, isRead)
    }

    suspend fun deleteStory(id: String) {
        storyDao.deleteStory(id)
    }

    suspend fun fetchAvailableCategories(): List<CategoryItem> {
        return scraperService.fetchCategories()
    }

    /**
     * Motor de Sincronización y Scraping con soporte Offline-First.
     *
     * Cumple con:
     * 1. Pausa anti-baneo de 1200ms entre descargas (delay(1200)).
     * 2. Filtro estricto: OMITIR cualquier relato con duración > 25 minutos.
     * 3. Persistencia inmediata en Room para que ninguna historia descargada se pierda si se corta internet.
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
                statusMessage = "Iniciando conexión con todorelatos.com...",
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

                for (summary in summaries) {
                    processedTotal++

                    // VERIFICACIÓN PRELIMINAR DE DURACIÓN
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

                    // AVISO DE DESCARGA
                    emit(
                        ScrapingProgress(
                            isRunning = true,
                            currentCategory = cat.name,
                            currentStoryTitle = summary.title,
                            downloadedCount = downloadedTotal,
                            skippedDueToDurationCount = skippedTotal,
                            totalProcessed = processedTotal,
                            statusMessage = "Descargando: '${summary.title}' (${summary.durationMinutes} min)..."
                        )
                    )

                    // CONTROL ANTI-BANEO OBLIGATORIO: Pausa de 1200 ms
                    delay(ScraperService.ANTI_BAN_DELAY_MS)

                    try {
                        val storyEntity = scraperService.fetchStoryContent(summary, cat.name)

                        if (storyEntity != null) {
                            // VALIDACIÓN FINAL ESTRICTA: Duración <= 25 minutos
                            if (storyEntity.durationMinutes <= ScraperService.MAX_ALLOWED_DURATION_MINUTES) {
                                // Guardar de inmediato en Room (OnConflictStrategy.IGNORE)
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
                                        statusMessage = "Guardado localmente: '${storyEntity.title}'"
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
                                        statusMessage = "Omitido en verificación final (>25 min): '${storyEntity.title}'"
                                    )
                                )
                            }
                        } else {
                            // El relato no pudo parsearse o fue descartado por exceso de duración
                            skippedTotal++
                        }
                    } catch (e: IOException) {
                        // Error de red en este relato específico: continuar con los demás sin perder los ya guardados
                        emit(
                            ScrapingProgress(
                                isRunning = true,
                                currentCategory = cat.name,
                                currentStoryTitle = summary.title,
                                downloadedCount = downloadedTotal,
                                skippedDueToDurationCount = skippedTotal,
                                totalProcessed = processedTotal,
                                statusMessage = "Conexión inestable en '${summary.title}'. Continuando..."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Si la categoría falla completamente, no detener el resto
                emit(
                    ScrapingProgress(
                        isRunning = true,
                        currentCategory = cat.name,
                        downloadedCount = downloadedTotal,
                        skippedDueToDurationCount = skippedTotal,
                        totalProcessed = processedTotal,
                        statusMessage = "Aviso en categoría ${cat.name}: ${e.localizedMessage ?: "error de red"}"
                    )
                )
            }
        }

        // Finalización exitosa
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
