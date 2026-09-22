package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.scraper.ScraperService
import com.example.data.scraper.ScrapingException
import kotlinx.coroutines.delay

/**
 * CoroutineWorker para sincronización en segundo plano de relatos.
 *
 * Resuelve:
 * 1. Resistencia a muerte de proceso y cambios de ciclo de vida (WorkManager gestionado por el SO).
 * 2. Pausa respetuosa anti-baneo (1200 ms) entre peticiones consecutivas.
 * 3. Notificación Foreground con barra de progreso en tiempo real ("Descargando relato X de Y de la categoría Z...").
 * 4. Tolerancia a fallos: guarda inmediatamente en Room cada relato exitoso sin descartar descargas previas.
 */
class SyncStoriesWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SyncStoriesWorker"
        const val UNIQUE_WORK_NAME = "SyncStoriesWork"

        const val CHANNEL_ID = "sync_stories_channel"
        const val NOTIFICATION_ID = 1001

        // Parámetros de entrada
        const val KEY_CATEGORY_IDS = "category_ids"
        const val KEY_CATEGORY_NAMES = "category_names"
        const val KEY_CATEGORY_URLS = "category_urls"
        const val KEY_MAX_PER_CAT = "max_per_category"

        // Claves de progreso y resultado
        const val PROGRESS_CATEGORY = "progress_category"
        const val PROGRESS_STORY_TITLE = "progress_story_title"
        const val PROGRESS_DOWNLOADED = "progress_downloaded"
        const val PROGRESS_SKIPPED = "progress_skipped"
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_STATUS_MSG = "progress_status_msg"
        const val PROGRESS_IS_RUNNING = "progress_is_running"
    }

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val catIds = inputData.getStringArray(KEY_CATEGORY_IDS) ?: emptyArray()
        val catNames = inputData.getStringArray(KEY_CATEGORY_NAMES) ?: emptyArray()
        val catUrls = inputData.getStringArray(KEY_CATEGORY_URLS) ?: emptyArray()
        val maxPerCategory = inputData.getInt(KEY_MAX_PER_CAT, 8)

        if (catIds.isEmpty() || catNames.isEmpty() || catUrls.isEmpty()) {
            return Result.failure(workDataOf(PROGRESS_STATUS_MSG to "No se seleccionaron categorías válidas."))
        }

        createNotificationChannel()

        // Establecer servicio en primer plano para asegurar que Android no mate la descarga
        try {
            setForeground(createForegroundInfo(0, 0, "Iniciando descarga...", ""))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar ForegroundService directo: ${e.message}")
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.storyDao()
        val scraper = ScraperService()

        var downloadedCount = 0
        var skippedCount = 0
        var processedTotal = 0

        val totalEstimatedStories = catIds.size * maxPerCategory

        for (i in catIds.indices) {
            if (isStopped) {
                return Result.success(
                    workDataOf(
                        PROGRESS_DOWNLOADED to downloadedCount,
                        PROGRESS_SKIPPED to skippedCount,
                        PROGRESS_STATUS_MSG to "Sincronización cancelada por el sistema."
                    )
                )
            }

            val categoryId = catIds[i]
            val categoryName = catNames.getOrNull(i) ?: "Categoría $categoryId"
            val categoryUrl = catUrls.getOrNull(i) ?: "${ScraperService.BASE_URL}/categorias/$categoryId/"

            setProgress(
                workDataOf(
                    PROGRESS_IS_RUNNING to true,
                    PROGRESS_CATEGORY to categoryName,
                    PROGRESS_DOWNLOADED to downloadedCount,
                    PROGRESS_SKIPPED to skippedCount,
                    PROGRESS_TOTAL to processedTotal,
                    PROGRESS_STATUS_MSG to "Explorando categoría: $categoryName..."
                )
            )

            try {
                setForeground(
                    createForegroundInfo(
                        current = processedTotal,
                        max = totalEstimatedStories,
                        title = "Sincronizando $categoryName",
                        content = "Buscando relatos..."
                    )
                )
            } catch (e: Exception) {
                // Ignore foreground update failure
            }

            try {
                val summaries = scraper.fetchStoriesForCategory(
                    categoryUrl = categoryUrl,
                    categoryName = categoryName,
                    maxStoriesToFetch = maxPerCategory
                )

                var storyIndexInCat = 0
                val storiesInCat = summaries.size

                for (summary in summaries) {
                    if (isStopped) break

                    storyIndexInCat++
                    processedTotal++

                    // Filtro previo de duración (> 25 min)
                    if (summary.durationMinutes > ScraperService.MAX_ALLOWED_DURATION_MINUTES) {
                        skippedCount++
                        setProgress(
                            workDataOf(
                                PROGRESS_IS_RUNNING to true,
                                PROGRESS_CATEGORY to categoryName,
                                PROGRESS_STORY_TITLE to summary.title,
                                PROGRESS_DOWNLOADED to downloadedCount,
                                PROGRESS_SKIPPED to skippedCount,
                                PROGRESS_TOTAL to processedTotal,
                                PROGRESS_STATUS_MSG to "Omitido (>25 min): '${summary.title}'"
                            )
                        )
                        continue
                    }

                    val progressTitle = "Relato $storyIndexInCat de $storiesInCat en $categoryName"
                    val progressContent = "'${summary.title}'"

                    try {
                        setForeground(
                            createForegroundInfo(
                                current = processedTotal,
                                max = totalEstimatedStories,
                                title = progressTitle,
                                content = progressContent
                            )
                        )
                    } catch (e: Exception) {
                        // ignore
                    }

                    setProgress(
                        workDataOf(
                            PROGRESS_IS_RUNNING to true,
                            PROGRESS_CATEGORY to categoryName,
                            PROGRESS_STORY_TITLE to summary.title,
                            PROGRESS_DOWNLOADED to downloadedCount,
                            PROGRESS_SKIPPED to skippedCount,
                            PROGRESS_TOTAL to processedTotal,
                            PROGRESS_STATUS_MSG to "Descargando relato $storyIndexInCat de $storiesInCat..."
                        )
                    )

                    // Pausa respetuosa anti-baneo obligatoria de 1200ms
                    delay(ScraperService.ANTI_BAN_DELAY_MS)

                    try {
                        val storyEntity = scraper.fetchStoryContent(summary, categoryName)
                        if (storyEntity != null && storyEntity.durationMinutes <= ScraperService.MAX_ALLOWED_DURATION_MINUTES) {
                            dao.insertStory(storyEntity)
                            downloadedCount++

                            setProgress(
                                workDataOf(
                                    PROGRESS_IS_RUNNING to true,
                                    PROGRESS_CATEGORY to categoryName,
                                    PROGRESS_STORY_TITLE to storyEntity.title,
                                    PROGRESS_DOWNLOADED to downloadedCount,
                                    PROGRESS_SKIPPED to skippedCount,
                                    PROGRESS_TOTAL to processedTotal,
                                    PROGRESS_STATUS_MSG to "Guardado offline: '${storyEntity.title}' (${storyEntity.durationMinutes} min)"
                                )
                            )
                        } else {
                            skippedCount++
                        }
                    } catch (e: ScrapingException) {
                        Log.w(TAG, "Error en relato '${summary.title}': ${e.message}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Excepción inesperada en '${summary.title}': ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando categoría $categoryName: ${e.message}")
            }
        }

        // Notificación final de término
        try {
            setForeground(
                createForegroundInfo(
                    current = processedTotal,
                    max = processedTotal,
                    title = "Sincronización completada",
                    content = "$downloadedCount relatos guardados, $skippedCount omitidos (>25m)"
                )
            )
        } catch (e: Exception) {
            // ignore
        }

        return Result.success(
            workDataOf(
                PROGRESS_DOWNLOADED to downloadedCount,
                PROGRESS_SKIPPED to skippedCount,
                PROGRESS_TOTAL to processedTotal,
                PROGRESS_STATUS_MSG to "Sincronización finalizada con éxito. $downloadedCount relatos descargados."
            )
        )
    }

    private fun createForegroundInfo(
        current: Int,
        max: Int,
        title: String,
        content: String
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(if (max > 0) max else 100, current, max <= 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronización de Relatos Offline",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de descarga de relatos offline para lectura nocturna"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
