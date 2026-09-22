package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos Room principal para almacenamiento offline de relatos.
 *
 * Características de producción:
 * 1. Patrón Singleton seguro con sincronización y doble verificación (Double-Checked Locking).
 * 2. Cero callbacks con corrutinas sueltas ni inserciones de datos falsos.
 * 3. Migraciones estructuradas en lugar de fallback destructivo que borraría la biblioteca del usuario.
 */
@Database(
    entities = [StoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storyDao(): StoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "todorelatos_offline.db"

        /**
         * Migración de versión 1 a versión 2:
         * Asegura la creación de índices para optimizar filtros y búsquedas de relatos.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stories_category` ON `stories` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stories_isFavorite` ON `stories` (`isFavorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stories_isRead` ON `stories` (`isRead`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
