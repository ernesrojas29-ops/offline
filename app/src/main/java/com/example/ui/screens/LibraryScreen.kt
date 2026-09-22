package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoryEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMutedAmoled
import com.example.ui.theme.TextPrimaryAmoled
import com.example.ui.theme.TextSecondaryAmoled

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onStoryClick: (String) -> Unit,
    onNavigateToScraper: () -> Unit
) {
    val stories by viewModel.displayedStories.collectAsState()
    val savedCategories by viewModel.savedCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterFavorites by viewModel.filterOnlyFavorites.collectAsState()
    val filterUnread by viewModel.filterOnlyUnread.collectAsState()
    val totalCount by viewModel.totalSavedStoriesCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Encabezado de la Biblioteca
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Biblioteca Offline",
                        color = TextPrimaryAmoled,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Badge Offline
                    Box(
                        modifier = Modifier
                            .background(AmoledCard, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Sin conexión requerida",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Offline",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "$totalCount relatos disponibles sin internet",
                    color = TextSecondaryAmoled,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Campo de búsqueda por texto en el título o autor
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            placeholder = {
                Text("Buscar por título o autor...", color = TextMutedAmoled, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = AmberGold
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar búsqueda",
                            tint = TextMutedAmoled
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmoledCard,
                unfocusedContainerColor = AmoledSurface,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = AmoledBorder,
                focusedTextColor = TextPrimaryAmoled,
                unfocusedTextColor = TextPrimaryAmoled
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Selector horizontal de categorías
        val allCategories = listOf("Todas") + savedCategories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCategories.forEach { cat ->
                val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategoryFilter(cat) },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberGold,
                        selectedLabelColor = AmoledBlack,
                        containerColor = AmoledSurface,
                        labelColor = TextSecondaryAmoled
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) AmberGold else AmoledBorder,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filtros rápidos adicionales (Favoritos / No leídos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterFavorites,
                onClick = { viewModel.toggleFilterFavorites() },
                leadingIcon = {
                    Icon(
                        imageVector = if (filterFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritos",
                        tint = if (filterFavorites) AmoledBlack else AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = { Text("Favoritos", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberGold,
                    selectedLabelColor = AmoledBlack,
                    containerColor = AmoledSurface,
                    labelColor = TextSecondaryAmoled
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (filterFavorites) AmberGold else AmoledBorder,
                    enabled = true,
                    selected = filterFavorites
                )
            )

            FilterChip(
                selected = filterUnread,
                onClick = { viewModel.toggleFilterUnread() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "No leídos",
                        tint = if (filterUnread) AmoledBlack else IndigoAccent,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = { Text("No leídos", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IndigoAccent,
                    selectedLabelColor = AmoledBlack,
                    containerColor = AmoledSurface,
                    labelColor = TextSecondaryAmoled
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (filterUnread) IndigoAccent else AmoledBorder,
                    enabled = true,
                    selected = filterUnread
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de relatos o Estado Vacío
        if (stories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Sin relatos",
                        tint = TextMutedAmoled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron relatos con '$searchQuery'" else "No hay relatos guardados en esta categoría",
                        color = TextPrimaryAmoled,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sincroniza tus categorías favoritas desde todorelatos.com para leer sin conexión.",
                        color = TextSecondaryAmoled,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToScraper,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = AmoledBlack),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ir a Descargar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(stories, key = { it.id }) { story ->
                    StoryCardItem(
                        story = story,
                        onClick = { onStoryClick(story.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(story) },
                        onToggleRead = { viewModel.toggleRead(story) },
                        onDelete = { viewModel.deleteStory(story.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoryCardItem(
    story: StoryEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("story_card_${story.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila superior: Categoría y Duración (Validada <= 25 min)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = story.category.uppercase(),
                    color = IndigoAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Badge de Duración en Minutos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AmoledSurface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Duración",
                        tint = AmberGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${story.durationMinutes} min",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título del Relato
            Text(
                text = story.title,
                color = TextPrimaryAmoled,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Autor del Relato
            Text(
                text = "Por ${story.author}",
                color = TextSecondaryAmoled,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fila inferior: Estado de lectura y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de Leído / No Leído
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onToggleRead() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (story.isRead) Icons.Default.Check else Icons.Default.Book,
                        contentDescription = if (story.isRead) "Leído" else "No leído",
                        tint = if (story.isRead) EmeraldGreen else TextMutedAmoled,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (story.isRead) "Leído" else "Por leer",
                        color = if (story.isRead) EmeraldGreen else TextMutedAmoled,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp).testTag("fav_btn_${story.id}")
                    ) {
                        Icon(
                            imageVector = if (story.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (story.isFavorite) RoseError else TextSecondaryAmoled,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar de la biblioteca",
                            tint = TextMutedAmoled,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
