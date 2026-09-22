package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar relatos descargados offline.
 *
 * Índices:
 * - Índice único en 'id' (clave primaria) y en 'category' para consultas rápidas.
 * - Validación estricta de regla de negocio: duración máxima <= 25 minutos.
 */
@Entity(
    tableName = "stories",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["category"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isRead"])
    ]
)
data class StoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // ID numérico o slug único del relato (ej. "262742")

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "durationMinutes")
    val durationMinutes: Int, // Duración validada estricta <= 25 minutos

    @ColumnInfo(name = "contentHtmlOrText")
    val contentHtmlOrText: String, // Texto limpio formateado para lectura nocturna

    @ColumnInfo(name = "isFavorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "isRead", defaultValue = "0")
    val isRead: Boolean = false,

    @ColumnInfo(name = "savedAt")
    val savedAt: Long = System.currentTimeMillis()
)
