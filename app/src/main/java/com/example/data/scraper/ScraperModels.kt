package com.example.data.scraper

/**
 * Representa una categoría disponible en todorelatos.com.
 * Nota: En https://movil.todorelatos.com/ las categorías son numéricas, e.g. /categorias/1/, /categorias/12/
 */
data class CategoryItem(
    val id: String,
    val name: String,
    val url: String,
    val storyCountApprox: Int = 0,
    val isSelected: Boolean = false
)

/**
 * Resumen de un relato extraído de la lista de una categoría.
 * Incluye la duración preliminar en minutos y si es elegible según la regla (<= 25 minutos).
 */
data class ScrapedStorySummary(
    val id: String,
    val title: String,
    val author: String,
    val durationMinutes: Int,
    val url: String,
    val isDurationEligible: Boolean // true si durationMinutes <= 25
)

/**
 * Estado y métricas en tiempo real del motor de scraping para la UI.
 */
data class ScrapingProgress(
    val isRunning: Boolean = false,
    val currentCategory: String = "",
    val currentStoryTitle: String = "",
    val downloadedCount: Int = 0,
    val skippedDueToDurationCount: Int = 0,
    val totalProcessed: Int = 0,
    val statusMessage: String = "",
    val isFinished: Boolean = false,
    val error: String? = null
)

/**
 * Excepción controlada para errores de red o parsing en el Web Scraping.
 * Garantiza que la UI reciba estados de error reales y honestos sin inyección de datos falsos.
 */
class ScrapingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
