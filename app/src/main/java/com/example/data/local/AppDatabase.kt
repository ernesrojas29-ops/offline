package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [StoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storyDao(): StoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todorelatos_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.storyDao()?.insertStories(getInitialWelcomeStories())
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = INSTANCE?.storyDao()
                                if (dao != null && dao.getCountDirect() == 0) {
                                    dao.insertStories(getInitialWelcomeStories())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getInitialWelcomeStories(): List<StoryEntity> {
            return listOf(
                StoryEntity(
                    id = "init_01",
                    title = "El Guardián del Apagón",
                    category = "Ciencia Ficción",
                    author = "M. Sterling",
                    durationMinutes = 12,
                    contentHtmlOrText = """
                        La sirena de la subestación eléctrica resonó como un trueno distante en toda la ciudad. En cuestión de tres segundos, las miles de luces de los rascacielos parpadearon dos veces y se extinguieron, sumiendo al valle en una oscuridad densa y casi sólida.
                        
                        Rodrigo permaneció inmóvil en el balcón de su apartamento en el piso doce. A diferencia del pánico que comenzaba a escucharse en las calles inferiores con bocinazos y alarmas desorientadas, él encendió con calma su viejo dispositivo de pantalla monocromática.
                        
                        Había estado esperando este apagón durante siete semanas. No por anarquía, sino por silencio.
                        
                        En la oscuridad total, los secretos de la red óptica auxiliar finalmente podían transmitirse sin interferencia de las estaciones de radiofrecuencia comerciales. El pulso que buscaba no provenía de satélites ni de antenas de telefonía, sino de un viejo cable submarino desactivado en 1998.
                        
                        Ajustó el conector analógico a su terminal. En la pantalla negra pura, un solo renglón de texto ámbar titiló:
                        
                        "Conexión establecida. Iniciando sincronización de memoria de archivo...".
                        
                        Sonrió. El apagón apenas estaba comenzando, pero la biblioteca eterna ya estaba a salvo.
                    """.trimIndent(),
                    isFavorite = true,
                    isRead = false
                ),
                StoryEntity(
                    id = "init_02",
                    title = "Sombras en la Niebla Nocturna",
                    category = "Misterio",
                    author = "Elena V.",
                    durationMinutes = 8,
                    contentHtmlOrText = """
                        La lluvia golpeaba suavemente los cristales del vagón de tren mientras avanzaba por el bosque gallego. Éramos solo tres pasajeros a bordo a esas horas de la medianoche.
                        
                        Frente a mí, un anciano de gabardina impermeable sostenía un libro con tapas de cuero gastadas. Durante todo el trayecto no pasó una sola página, pero sus ojos seguían atentos el ritmo del cristal empañado.
                        
                        Cuando el tren redujo la marcha al cruzar el viejo puente de piedra sobre el cañón, las luces del compartimento parpadearon levemente. Fue en ese instante fugaz cuando noté el reflejo en la ventana: el anciano no tenía sombra proyectada contra la madera del respaldo.
                        
                        Al volver la mirada hacia su asiento, el libro yacía cerrado sobre la butaca de terciopelo. De él solo quedaba el sutil aroma a tierra mojada y páginas antiguas.
                    """.trimIndent(),
                    isFavorite = false,
                    isRead = false
                ),
                StoryEntity(
                    id = "init_03",
                    title = "Promesas Bajo el Viejo Roble",
                    category = "Romance",
                    author = "Carlos D.",
                    durationMinutes = 15,
                    contentHtmlOrText = """
                        Volver al pueblo después de diez años se sentía como hojear un diario que creías haber olvidado en el ático. La colina seguía idéntica, coronada por las ramas monumentales del roble donde grabamos nuestras iniciales en el verano del 2014.
                        
                        El viento de la tarde arrastraba el olor a hierba fresca y pinos. Me senté sobre las raíces descubiertas y cerré los ojos un instante, recordando la promesa: «Si a los treinta seguimos buscando un rumbo, nos encontramos aquí al caer el sol».
                        
                        Eran exactamente las seis y cuarenta y cinco cuando el crujido de hojas secas interrumpió el murmullo de los pájaros. Levanté la mirada. Allí estaba ella, con la misma sonrisa tímida y una bufanda azul que desafiaba el paso de los años.
                    """.trimIndent(),
                    isFavorite = false,
                    isRead = true
                )
            )
        }
    }
}
