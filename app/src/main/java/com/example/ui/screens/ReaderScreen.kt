package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Divider
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
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMutedAmoled
import com.example.ui.theme.TextPrimaryAmoled
import com.example.ui.theme.TextSecondaryAmoled

@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val story by viewModel.activeStory.collectAsState()
    val fontSize by viewModel.readerFontSize.collectAsState()
    val isAmoled by viewModel.isAmoledBlack.collectAsState()

    // Fondo: Negro puro #000000 para ahorro de energía en pantallas OLED durante apagones
    val currentBg = if (isAmoled) AmoledBlack else Color(0xFF111418)
    val textColor = TextPrimaryAmoled

    if (story == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentBg),
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
        // Barra superior con controles nocturnos
        Surface(
            color = currentBg,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
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

                // Selector de tamaño de fuente
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AmoledCard, RoundedCornerShape(18.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.decreaseFontSize() },
                        modifier = Modifier.size(34.dp).testTag("decrease_font_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextDecrease,
                            contentDescription = "Reducir letra",
                            tint = if (fontSize > 12) AmberGold else TextMutedAmoled,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "${fontSize}sp",
                        color = TextPrimaryAmoled,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { viewModel.increaseFontSize() },
                        modifier = Modifier.size(34.dp).testTag("increase_font_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextIncrease,
                            contentDescription = "Aumentar letra",
                            tint = if (fontSize < 32) AmberGold else TextMutedAmoled,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Acciones de Favorito y Leído
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentStory) },
                        modifier = Modifier.testTag("reader_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (currentStory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (currentStory.isFavorite) RoseError else TextSecondaryAmoled
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleRead(currentStory) },
                        modifier = Modifier.testTag("reader_read_button")
                    ) {
                        Icon(
                            imageVector = if (currentStory.isRead) Icons.Default.Check else Icons.Default.BookmarkBorder,
                            contentDescription = "Marcar como leído",
                            tint = if (currentStory.isRead) EmeraldGreen else TextSecondaryAmoled
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = AmoledBorder, thickness = 1.dp)

        // Contenido del relato con scroll vertical
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            // Insignia de categoría y tiempo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentStory.category.uppercase(),
                    color = IndigoAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AmoledCard, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Tiempo de lectura",
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentStory.durationMinutes} min de lectura",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título de la historia
            Text(
                text = currentStory.title,
                color = TextPrimaryAmoled,
                fontSize = (fontSize + 6).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (fontSize + 12).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Autor
            Text(
                text = "Por ${currentStory.author}",
                color = TextSecondaryAmoled,
                fontSize = (fontSize - 3).coerceAtLeast(11).sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AmoledBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(20.dp))

            // Texto limpio del relato con tipografía cómoda para la vista
            Text(
                text = currentStory.contentHtmlOrText,
                color = textColor,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.65).sp,
                textAlign = TextAlign.Start,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Tarjeta de fin de relato
            Card(
                colors = CardDefaults.cardColors(containerColor = AmoledCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "— Fin del Relato —",
                        color = AmberGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Guardado en almacenamiento local (Room Database) para lectura sin internet.",
                        color = TextSecondaryAmoled,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
