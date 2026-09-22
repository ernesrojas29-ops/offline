package com.example.data.scraper

data class CategoryItem(
    val id: String,
    val name: String,
    val url: String,
    val storyCountApprox: Int = 0,
    val isSelected: Boolean = false
)

data class ScrapedStorySummary(
    val id: String,
    val title: String,
    val author: String,
    val durationMinutes: Int,
    val url: String,
    val isDurationEligible: Boolean // true if durationMinutes <= 25
)

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
