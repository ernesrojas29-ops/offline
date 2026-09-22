package com.example.data.scraper

import android.util.Log
import com.example.data.local.StoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

/**
 * Servicio de Web Scraping para https://movil.todorelatos.com/
 *
 * Características clave:
 * 1. Enfoque 100% real: Cero datos falsos o mock data en caso de fallo.
 * 2. Manejo de URLs reales:
 *    - Categorías numéricas: /categorias/(\d+)/
 *    - Relatos: /relato/(\d+)/
 *    - Autores: /perfil/(\d+)/ o texto "por [autor]"
 * 3. Detección precisa de tiempo de lectura:
 *    - Patrón: "Tiempo estimado de lectura: [ 15 min. ]", "[ 12 min. ]", "15 min", etc.
 *    - Fallback algorítmico si no existe etiqueta: conteo de palabras / 190 ppm.
 * 4. Regla crítica de negocio:
 *    - Si duración > 25 minutos, se omite de inmediato (sin descargar cuerpo pesado).
 * 5. Control anti-baneo:
 *    - Constante ANTI_BAN_DELAY_MS = 1200L para pausar entre peticiones en el repositorio.
 */
class ScraperService {

    companion object {
        private const val TAG = "ScraperService"
        const val BASE_URL = "https://movil.todorelatos.com"
        const val CATEGORIES_URL = "https://movil.todorelatos.com/categorias/"

        // Regla de Negocio Crítica: Máximo 25 minutos de lectura
        const val MAX_ALLOWED_DURATION_MINUTES = 25

        // Control Anti-Baneo obligatorio entre descargas consecutivas
        const val ANTI_BAN_DELAY_MS = 1200L

        // Timeout estricto de red: 10 segundos
        private const val TIMEOUT_MS = 10000

        // Palabras por minuto promedio para estimar duración de lectura
        private const val WORDS_PER_MINUTE = 190

        // User-Agent móvil moderno y realista
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UD1A.230803.041; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/126.0.6478.133 Mobile Safari/537.36"
    }

    // Regex 1: Formato real del sitio "Tiempo estimado de lectura: [ 15 min. ]" o "[ 15 min. ]"
    private val bracketsDurationRegex =
        """\[\s*(\d+)\s*min(?:\.|\b)""".toRegex(RegexOption.IGNORE_CASE)

    // Regex 2: Formatos alternativos en listas: "Lectura: 12 min", "15 min.", "15 minutos"
    private val generalDurationRegex =
        """(?:lectura|duraci[oó]n|tiempo)?[:\s]*(\d+)\s*(?:minutos?|mins?|min\.?|m\b)""".toRegex(RegexOption.IGNORE_CASE)

    // Regex 3: Fallback numérico antes de palabra min
    private val fallbackDurationRegex =
        """(\d+)\s*(?:min|minuto)""".toRegex(RegexOption.IGNORE_CASE)

    // Regex para identificar enlaces a relatos: /relato/ID/
    private val storyLinkRegex = """/relato/(\d+)/?""".toRegex()

    // Regex para identificar enlaces a categorías numéricas: /categorias/ID/ o /categoria/ID/
    private val categoryLinkRegex = """/categorias?/(\d+)/?""".toRegex()

    /**
     * Extrae el número entero de minutos de cadenas de texto tales como:
     * - "Tiempo estimado de lectura: [ 15 min. ]"
     * - "[ 8 min. ]"
     * - "Lectura: 12 min"
     * - "5 min"
     * Retorna -1 si no se localizó una duración explícita.
     */
    fun extractDurationMinutes(rawText: String): Int {
        if (rawText.isBlank()) return -1

        // Prioridad 1: Formato con corchetes [ XX min. ]
        bracketsDurationRegex.find(rawText)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        // Prioridad 2: Formato general "Lectura: XX min"
        generalDurationRegex.find(rawText)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        // Prioridad 3: Fallback "XX min"
        fallbackDurationRegex.find(rawText)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        return -1
    }

    /**
     * Calcula la duración aproximada en minutos a partir del texto limpio.
     * Utiliza el estándar de lectura de 190 palabras por minuto.
     */
    fun calculateDurationFromWordCount(text: String): Int {
        if (text.isBlank()) return 0
        val words = text.split("""\s+""".toRegex()).count { it.isNotBlank() }
        val minutes = words / WORDS_PER_MINUTE
        return if (minutes < 1) 1 else minutes
    }

    /**
     * Conecta a https://movil.todorelatos.com/categorias/ y extrae las categorías reales.
     * Selector real: Parsear los enlaces que apunten a /categorias/ID/.
     * Lanza ScrapingException si la red falla o la página no contiene datos válidos.
     */
    suspend fun fetchCategories(): List<CategoryItem> = withContext(Dispatchers.IO) {
        val categories = mutableListOf<CategoryItem>()

        try {
            val doc: Document = Jsoup.connect(CATEGORIES_URL)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .timeout(TIMEOUT_MS)
                .get()

            // Buscar todos los enlaces que contengan /categorias/
            val links = doc.select("a[href*=/categorias/]")

            for (link in links) {
                val href = link.attr("href")
                val absUrl = link.attr("abs:href").ifBlank {
                    if (href.startsWith("http")) href else "$BASE_URL$href"
                }

                val match = categoryLinkRegex.find(href) ?: categoryLinkRegex.find(absUrl)
                if (match != null) {
                    val categoryId = match.groupValues[1]
                    val rawName = link.text().trim()

                    // Limpiar nombre si contiene conteos entre paréntesis, e.g. "Romance (142)"
                    val cleanName = rawName.replace("""\(\d+\)""".toRegex(), "").trim()

                    if (cleanName.isNotBlank() && !categories.any { it.id == categoryId }) {
                        categories.add(
                            CategoryItem(
                                id = categoryId,
                                name = cleanName,
                                url = absUrl
                            )
                        )
                    }
                }
            }

            if (categories.isEmpty()) {
                throw ScrapingException(
                    "No se encontraron enlaces de categorías válidas en $CATEGORIES_URL"
                )
            }

            categories
        } catch (e: ScrapingException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error de red al conectar a $CATEGORIES_URL: ${e.message}")
            throw ScrapingException("Error de conexión al cargar categorías: ${e.localizedMessage ?: "Servidor inaccesible"}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al procesar categorías: ${e.message}")
            throw ScrapingException("Fallo al procesar categorías: ${e.localizedMessage ?: "Error desconocido"}", e)
        }
    }

    /**
     * Extrae el listado de relatos para una categoría específica (ej. /categorias/1/).
     * Captura:
     * - Título
     * - URL del relato (/relato/ID/)
     * - Autor (/perfil/ID/ o tras texto "por")
     * - Duración en minutos
     *
     * Regla estricta:
     * Si la duración parseada en la tarjeta es > 25 minutos, se marca isDurationEligible = false.
     * Lanza ScrapingException en caso de fallo de red (cero mock data).
     */
    suspend fun fetchStoriesForCategory(
        categoryUrl: String,
        categoryName: String,
        maxStoriesToFetch: Int = 10
    ): List<ScrapedStorySummary> = withContext(Dispatchers.IO) {
        val summaries = mutableListOf<ScrapedStorySummary>()
        val targetUrl = if (categoryUrl.startsWith("http")) categoryUrl else "$BASE_URL$categoryUrl"

        try {
            val doc: Document = Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .timeout(TIMEOUT_MS)
                .get()

            // Buscar todos los enlaces que apunten a /relato/ID/
            val storyLinks = doc.select("a[href*=/relato/]")

            for (link in storyLinks) {
                if (summaries.size >= maxStoriesToFetch) break

                val href = link.attr("href")
                val absUrl = link.attr("abs:href").ifBlank {
                    if (href.startsWith("http")) href else "$BASE_URL$href"
                }

                val match = storyLinkRegex.find(href) ?: storyLinkRegex.find(absUrl) ?: continue
                val storyId = match.groupValues[1]

                // Evitar duplicados
                if (summaries.any { it.id == storyId }) continue

                // Título
                val title = link.text().trim().ifBlank {
                    link.parent()?.selectFirst("h1, h2, h3, .titulo")?.text()?.trim() ?: "Relato #$storyId"
                }

                // Contenedor padre de la tarjeta o elemento de lista
                val container = link.parents().firstOrNull { parent ->
                    parent.tagName() in listOf("article", "div", "li", "tr") &&
                    parent.select("a[href*=/relato/]").size <= 2
                } ?: link.parent()

                // Extraer autor: Enlace a /perfil/ID/ o texto que contenga "por"
                var author = "Anónimo"
                val profileLink = container?.selectFirst("a[href*=/perfil/]")
                if (profileLink != null && profileLink.text().isNotBlank()) {
                    author = profileLink.text().replace("""^por\s+""".toRegex(RegexOption.IGNORE_CASE), "").trim()
                } else if (container != null) {
                    val containerText = container.text()
                    val authorMatch = """por\s+([A-Za-z0-9_áéíóúÁÉÍÓÚñÑ\.\-\s]{2,30})""".toRegex(RegexOption.IGNORE_CASE).find(containerText)
                    if (authorMatch != null) {
                        author = authorMatch.groupValues[1].trim()
                    }
                }

                // Extraer tiempo de lectura de la tarjeta
                val containerSnippet = container?.text() ?: ""
                val parsedMinutes = extractDurationMinutes(containerSnippet)

                // Si se detectó una duración válida en la tarjeta
                val finalMinutes = if (parsedMinutes > 0) parsedMinutes else -1
                val isEligible = finalMinutes in 1..MAX_ALLOWED_DURATION_MINUTES

                summaries.add(
                    ScrapedStorySummary(
                        id = storyId,
                        title = title,
                        author = author,
                        durationMinutes = finalMinutes,
                        url = absUrl,
                        isDurationEligible = isEligible
                    )
                )
            }

            if (summaries.isEmpty()) {
                Log.w(TAG, "No se encontraron relatos en $targetUrl")
            }

            summaries
        } catch (e: IOException) {
            Log.e(TAG, "Error de conexión al obtener relatos de $targetUrl: ${e.message}")
            throw ScrapingException("No se pudo conectar a la categoría: ${e.localizedMessage ?: "Error de red"}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al parsear relatos de $targetUrl: ${e.message}")
            throw ScrapingException("Error al procesar la lista de relatos: ${e.localizedMessage ?: "Fallo inesperado"}", e)
        }
    }

    /**
     * Descarga y limpia el contenido completo de un relato individual (/relato/ID/).
     *
     * Reglas estrictas:
     * 1. Si la duración previa en el resumen ya es > 25 minutos, se descarta INMEDIATAMENTE
     *    sin descargar el HTML pesado ni procesar el contenido.
     * 2. Extrae y formatea el texto limpio: descarta scripts, estilos, publicidad, nav, cabeceras y botones sociales.
     * 3. Extrae la duración precisa desde la página del relato (ej. "Tiempo estimado de lectura: [ 15 min. ]").
     * 4. Si la página no contiene etiqueta de duración, se calcula en tiempo real por palabras: (palabras / 190).
     * 5. REGLA CRÍTICA: Si la duración final es > 25 minutos, devuelve null (relato omitido).
     * 6. Lanza ScrapingException si falla la red (cero inyecciones de datos falsos/mocks).
     */
    suspend fun fetchStoryContent(
        summary: ScrapedStorySummary,
        categoryName: String
    ): StoryEntity? = withContext(Dispatchers.IO) {
        // FILTRO PRELIMINAR INMEDIATO: Si ya sabemos que supera 25 minutos, omitir sin descargar HTML
        if (summary.durationMinutes > MAX_ALLOWED_DURATION_MINUTES) {
            Log.i(TAG, "Relato '${summary.title}' OMITIDO previamente: ${summary.durationMinutes} min > $MAX_ALLOWED_DURATION_MINUTES min.")
            return@withContext null
        }

        try {
            val doc: Document = Jsoup.connect(summary.url)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .timeout(TIMEOUT_MS)
                .get()

            // 1. Extraer duración exacta desde la página de detalle
            val entirePageText = doc.text()
            var detectedDuration = extractDurationMinutes(entirePageText)

            // 2. Extraer y limpiar título
            var cleanTitle = summary.title
            val h1 = doc.selectFirst("h1, .titulo-relato, .entry-title")
            if (h1 != null && h1.text().isNotBlank()) {
                cleanTitle = h1.text().trim()
            }

            // 3. Extraer y limpiar autor
            var cleanAuthor = summary.author
            val authorEl = doc.selectFirst("a[href*=/perfil/], .autor, .author")
            if (authorEl != null && authorEl.text().isNotBlank()) {
                cleanAuthor = authorEl.text().replace("""^por\s+""".toRegex(RegexOption.IGNORE_CASE), "").trim()
            }

            // 4. Selector del contenedor de texto y saneamiento de elementos no deseados
            val contentElement: Element? = doc.selectFirst(
                "div.texto, div.relato, #cuerpo-relato, div.contenido, article .entry-content, div.post-content, .cuerpo, div#texto"
            ) ?: doc.selectFirst("article")

            val cleanedText: String
            if (contentElement != null) {
                // Eliminar anuncios, cabeceras, scripts, estilos, formularios y barras sociales
                contentElement.select(
                    "script, style, iframe, .ads, .publicidad, nav, footer, header, .compartir, .social, .breadcrumb, form, button"
                ).remove()
                cleanedText = cleanStoryHtml(contentElement)
            } else {
                // Fallback de extracción por párrafos con longitud significativa
                val paragraphs = doc.select("p").filter { it.text().trim().length > 35 }
                cleanedText = paragraphs.joinToString("\n\n") { it.text().trim() }
            }

            if (cleanedText.isBlank()) {
                throw ScrapingException("No se pudo extraer contenido de texto legible en ${summary.url}")
            }

            // 5. Cálculo de duración por palabras si la web no indicó la etiqueta de tiempo
            if (detectedDuration <= 0) {
                detectedDuration = calculateDurationFromWordCount(cleanedText)
                Log.d(TAG, "Duración calculada por palabras para '${summary.title}': $detectedDuration min")
            }

            // 6. REGLA DE NEGOCIO CRÍTICA: Filtrar y OMITIR si supera 25 minutos
            if (detectedDuration > MAX_ALLOWED_DURATION_MINUTES) {
                Log.i(TAG, "Relato '$cleanTitle' OMITIDO: Duración de $detectedDuration min supera los $MAX_ALLOWED_DURATION_MINUTES min permitidos.")
                return@withContext null
            }

            StoryEntity(
                id = summary.id.ifBlank { "relato_${System.currentTimeMillis()}" },
                title = cleanTitle,
                category = categoryName,
                author = cleanAuthor.ifBlank { "Anónimo" },
                durationMinutes = detectedDuration,
                contentHtmlOrText = cleanedText,
                isFavorite = false,
                isRead = false,
                savedAt = System.currentTimeMillis()
            )
        } catch (e: IOException) {
            Log.e(TAG, "Error de red al descargar relato ${summary.url}: ${e.message}")
            throw ScrapingException("Error de red al descargar relato '${summary.title}': ${e.localizedMessage ?: "Conexión perdida"}", e)
        } catch (e: ScrapingException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar el contenido de ${summary.url}: ${e.message}")
            throw ScrapingException("Error inesperado en '${summary.title}': ${e.localizedMessage ?: "Fallo de parsing"}", e)
        }
    }

    /**
     * Limpia los elementos HTML dejando un texto plano legible estructurado en párrafos.
     */
    private fun cleanStoryHtml(element: Element): String {
        val paragraphs = element.select("p, div.parrafo")
        if (paragraphs.isNotEmpty()) {
            val sb = StringBuilder()
            for (p in paragraphs) {
                val text = p.text().trim()
                if (text.isNotBlank()) {
                    sb.append(text).append("\n\n")
                }
            }
            val result = sb.toString().trim()
            if (result.isNotBlank()) return result
        }

        // Reemplazar saltos <br> por saltos de línea y extraer texto
        element.select("br").append("\\n")
        return element.text().replace("\\n", "\n").trim()
    }
}
