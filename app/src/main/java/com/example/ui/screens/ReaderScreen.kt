package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMutedAmoled
import com.example.ui.theme.TextPrimaryAmoled
import com.example.ui.theme.TextSecondaryAmoled

/**
 * Pantalla de lectura optimizada para lectura nocturna y apagones.
 *
 * Características:
 * 1. Soporte completo de Edge-to-Edge con WindowInsets:
 *    - Inset superior seguro (Status Bar / Punch-hole / Notch)
 *    - Inset inferior seguro (Navigation Bar / Barra de gestos)
 * 2. Fondo dinámico persistido:
 *    - Negro absoluto puro AMOLED (#000000) o Gris oscuro (#121212)
 * 3. Controles interactivos persistidos en Jetpack DataStore:
 *    - Control de tamaño de letra (sp) con botones accesibles (+/-)
 *    - Alternancia de tema AMOLED
 *    - Marcado rápido como Favorito o Leído
 */
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val story by viewModel.activeStory.collectAsState()
    val fontSize by viewModel.readerFontSize.collectAsState()
    val isAmoled by viewModel.isAmoledBlack.collectAsState()

    // Fondo: Negro puro #000000 para ahorro en pantallas OLED o Gris oscuro #121212
    val currentBg = if (isAmoled) AmoledBlack else DarkBackground
    val cardBg = if (isAmoled) AmoledCard else Color(0xFF1E1E1E)
    val textColor = TextPrimaryAmoled

    // Padding de insets del sistema para Edge-to-Edge seguro
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    if (story == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentBg)
                .padding(statusBarPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Cargando relato...", color = TextSecondaryAmoled)
        }
        return
    }

    val currentStory = story!!
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBg)
    ) {
        // BARRA SUPERIOR CON INSET DE STATUS BAR SEGURO
        Surface(
            color = currentBg,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding.calculateTopPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a la biblioteca",
                            tint = AmberGold
                        )
                    }

                    // Acciones rápidas de lectura
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Alternar favorito
                        IconButton(
                            onClick = { viewModel.toggleFavorite(currentStory) },
                            modifier = Modifier.testTag("reader_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (currentStory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (currentStory.isFavorite) "Quitar de favoritos" else "Marcar favorito",
                                tint = if (currentStory.isFavorite) RoseError else TextMutedAmoled
                            )
                        }

                        // Alternar leído / pendiente
                        IconButton(
                            onClick = { viewModel.toggleRead(currentStory) },
                            modifier = Modifier.testTag("reader_mark_read_button")
                        ) {
                            Icon(
                                imageVector = if (currentStory.isRead) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (currentStory.isRead) "Marcar no leído" else "Marcar leído",
                                tint = if (currentStory.isRead) EmeraldGreen else TextMutedAmoled
                            )
                        }

                        // Alternar fondo AMOLED puro (#000000 vs #121212)
                        IconButton(
                            onClick = { viewModel.toggleAmoledMode() },
                            modifier = Modifier.testTag("reader_amoled_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness2,
                                contentDescription = "Alternar modo AMOLED",
                                tint = if (isAmoled) AmberGold else TextSecondaryAmoled
                            )
                        }
                    }
                }
            }
        }

        // CONTENIDO DEL RELATO CON SCROLL Y RESPETO DE INSETS
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Insignia de categoría y tiempo de lectura
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = IndigoAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = currentStory.category,
                        color = IndigoAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${currentStory.durationMinutes} min de lectura",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título principal
            Text(
                text = currentStory.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Autor y estado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "por ${currentStory.author}",
                    fontSize = 14.sp,
                    color = TextSecondaryAmoled,
                    fontWeight = FontWeight.Normal
                )

                if (currentStory.isRead) {
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Leído",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AmoledBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            // CUERPO DEL RELATO CON TIPOGRAFÍA CONFIGURABLE
            Text(
                text = currentStory.contentHtmlOrText,
                fontSize = fontSize.sp,
                color = textColor,
                lineHeight = (fontSize * 1.6).sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reader_story_body")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Fin del relato
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "• • •",
                        color = TextMutedAmoled,
                        fontSize = 18.sp,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Fin del relato",
                        color = TextSecondaryAmoled,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Espaciador para evitar que la barra flotante tape el texto final
            Spacer(modifier = Modifier.height(64.dp))
        }

        // BARRA INFERIOR CON CONTROLES TIPOGRÁFICOS Y RESPETO DE NAVIGATION BAR
        Surface(
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarPadding.calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Control de fuente -
                    IconButton(
                        onClick = { viewModel.decreaseFontSize() },
                        enabled = fontSize > 12,
                        modifier = Modifier.testTag("decrease_font_size_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextDecrease,
                            contentDescription = "Reducir tamaño de letra",
                            tint = if (fontSize > 12) AmberGold else TextMutedAmoled
                        )
                    }

                    // Indicador de tamaño actual
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = TextSecondaryAmoled,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${fontSize}sp",
                            color = TextPrimaryAmoled,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Control de fuente +
                    IconButton(
                        onClick = { viewModel.increaseFontSize() },
                        enabled = fontSize < 32,
                        modifier = Modifier.testTag("increase_font_size_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextIncrease,
                            contentDescription = "Aumentar tamaño de letra",
                            tint = if (fontSize < 32) AmberGold else TextMutedAmoled
                        )
                    }

                    // Modo de fondo actual
                    Surface(
                        color = if (isAmoled) AmoledBlack else DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder)
                    ) {
                        Text(
                            text = if (isAmoled) "AMOLED Puro" else "Gris Oscuro",
                            color = if (isAmoled) AmberGold else TextSecondaryAmoled,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
