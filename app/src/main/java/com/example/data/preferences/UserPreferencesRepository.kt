package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_reader_settings")

/**
 * Repositorio de preferencias del usuario implementado con Jetpack DataStore Preferences.
 *
 * Persiste de forma asíncrona, transaccional y consistente:
 * - fontSize: Tamaño de fuente del lector en sp (por defecto 18sp).
 * - isAmoledTheme: Modo de lectura con fondo negro absoluto #000000 (por defecto true).
 */
class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    companion object {
        val KEY_FONT_SIZE = intPreferencesKey("reader_font_size")
        val KEY_AMOLED_THEME = booleanPreferencesKey("reader_amoled_theme")

        const val DEFAULT_FONT_SIZE = 18
        const val DEFAULT_AMOLED_THEME = true
    }

    val fontSizeFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_FONT_SIZE] ?: DEFAULT_FONT_SIZE
        }

    val isAmoledThemeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_AMOLED_THEME] ?: DEFAULT_AMOLED_THEME
        }

    suspend fun setFontSize(fontSize: Int) {
        val clampedSize = fontSize.coerceIn(12, 36)
        dataStore.edit { preferences ->
            preferences[KEY_FONT_SIZE] = clampedSize
        }
    }

    suspend fun setAmoledTheme(isAmoled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AMOLED_THEME] = isAmoled
        }
    }
}
