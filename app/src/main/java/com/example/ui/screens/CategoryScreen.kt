package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.scraper.CategoryItem
import com.example.data.scraper.ScrapingProgress
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMutedAmoled
import com.example.ui.theme.TextPrimaryAmoled
import com.example.ui.theme.TextSecondaryAmoled

@Composable
fun CategoryScreen(
    viewModel: MainViewModel,
    onNavigateToLibrary: () -> Unit
) {
    val categories by viewModel.availableCategories.collectAsState()
    val selectedIds by viewModel.selectedCategoryIds.collectAsState()
    val isLoading by viewModel.isCategoriesLoading.collectAsState()
    val progress by viewModel.scrapingProgress.collectAsState()
    val totalSaved by viewModel.totalSavedStoriesCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Encabezado principal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sincronizador Offline",
                    color = TextPrimaryAmoled,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Descarga para leer durante apagones",
                    color = TextSecondaryAmoled,
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = { viewModel.loadAvailableCategories() },
                modifier = Modifier.testTag("refresh_categories_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refrescar categorías",
                    tint = AmberGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tarjeta de Regla de Negocio y Control Anti-Baneo
        RuleBadgeCard()

        Spacer(modifier = Modifier.height(12.dp))

        // Tarjeta de Progreso Activo de Scraping
        AnimatedVisibility(visible = progress.isRunning || progress.isFinished) {
            ScrapingProgressCard(
                progress = progress,
                onCancel = { viewModel.cancelSync() },
                onGoToLibrary = onNavigateToLibrary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de acciones de selección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categorías (${selectedIds.size} seleccionadas)",
                color = TextPrimaryAmoled,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row {
                Text(
                    text = "Todas",
                    color = AmberGold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { viewModel.selectAllCategories() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("select_all_categories_button")
                )
                Text(
                    text = "Ninguna",
                    color = TextMutedAmoled,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { viewModel.deselectAllCategories() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("deselect_all_categories_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de categorías con checkboxes
        if (isLoading && categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmberGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Conectando a todorelatos.com...",
                        color = TextSecondaryAmoled,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val isChecked = selectedIds.contains(cat.id)
                    CategoryRowItem(
                        category = cat,
                        isChecked = isChecked,
                        onToggle = { viewModel.toggleCategorySelection(cat.id) }
                    )
                }
            }
        }

        // Botón principal de sincronización fijado abajo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Button(
                onClick = { viewModel.startSync() },
                enabled = selectedIds.isNotEmpty() && !progress.isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = AmoledBlack,
                    disabledContainerColor = AmoledCard,
                    disabledContentColor = TextMutedAmoled
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sync_button")
            ) {
                if (progress.isRunning) {
                    CircularProgressIndicator(
                        color = AmoledBlack,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sincronizando...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Descargar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciar Descarga (${selectedIds.size} seleccionadas)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RuleBadgeCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtro",
                    tint = AmberGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Regla de lectura: Relatos ≤ 25 minutos",
                    color = AmberGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Anti-baneo",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Control anti-baneo: pausa de 1.2s entre descargas",
                    color = TextSecondaryAmoled,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ScrapingProgressCard(
    progress: ScrapingProgress,
    onCancel: () -> Unit,
    onGoToLibrary: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scraping_progress_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress.isRunning) {
                        CircularProgressIndicator(
                            color = AmberGold,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completado",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress.isRunning) "Descargando relatos..." else "Descarga finalizada",
                        color = TextPrimaryAmoled,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress.isRunning) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp).testTag("cancel_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener",
                            tint = RoseError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (progress.isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AmberGold,
                    trackColor = AmoledBorder
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Estado y detalles del scraper
            Text(
                text = progress.statusMessage,
                color = TextSecondaryAmoled,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contadores en tiempo real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Guardados",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guardados: ${progress.downloadedCount}",
                        color = EmeraldGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // CONTADOR DE RELATOS OMITIDOS POR DURACIÓN (> 25 MIN)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Omitidos",
                        tint = RoseError,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Omitidos (>25m): ${progress.skippedDueToDurationCount}",
                        color = RoseError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (progress.isFinished) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGoToLibrary,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Abrir Biblioteca Offline", color = AmberGold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun CategoryRowItem(
    category: CategoryItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) AmoledCard else AmoledSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("category_item_${category.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    color = if (isChecked) TextPrimaryAmoled else TextSecondaryAmoled,
                    fontSize = 15.sp,
                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "movil.todorelatos.com",
                    color = TextMutedAmoled,
                    fontSize = 11.sp
                )
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AmberGold,
                    checkmarkColor = AmoledBlack,
                    uncheckedColor = TextMutedAmoled
                ),
                modifier = Modifier.testTag("checkbox_${category.id}")
            )
        }
    }
}
