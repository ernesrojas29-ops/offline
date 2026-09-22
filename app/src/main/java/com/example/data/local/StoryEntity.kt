package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar relatos descargados offline.
 *
 * Cumple con la regla de negocio: duración máxima <= 25 minutos.
 */
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val id: String, // Slug o URL única del relato para evitar duplicados
    val title: String,
    val category: String,
    val author: String,
    val durationMinutes: Int, // Duración validada <= 25 minutos
    val contentHtmlOrText: String, // Texto limpio del relato para lectura nocturna
    val isFavorite: Boolean = false,
    val isRead: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)
