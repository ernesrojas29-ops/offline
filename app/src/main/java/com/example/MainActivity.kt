package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.repository.StoryRepository
import com.example.data.scraper.ScraperService
import com.example.ui.MainViewModel
import com.example.ui.screens.CategoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMutedAmoled
import com.example.ui.theme.TextPrimaryAmoled

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val preferencesRepository = com.example.data.preferences.UserPreferencesRepository(applicationContext)
        val scraperService = ScraperService()
        val repository = StoryRepository(database.storyDao(), scraperService)
        val viewModel = androidx.lifecycle.ViewModelProvider(
            this,
            MainViewModel.Factory(applicationContext, repository, preferencesRepository)
        )[MainViewModel::class.java]

        setContent {
            MyApplicationTheme(darkTheme = true, amoledMode = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentStoryId by viewModel.currentStoryId.collectAsState()
    val totalStoriesCount by viewModel.totalSavedStoriesCount.collectAsState()
    val progress by viewModel.scrapingProgress.collectAsState()

    // Manejar botón atrás de Android para salir del lector si está abierto
    BackHandler(enabled = currentStoryId != null) {
        viewModel.closeReader()
    }

    if (currentStoryId != null) {
        // Pantalla 3: Lector de Relatos AMOLED
        ReaderScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeReader() }
        )
    } else {
        // Navegación entre Biblioteca (Pantalla 2) y Descargas (Pantalla 1)
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = AmoledBlack,
                    contentColor = TextPrimaryAmoled,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (totalStoriesCount > 0) {
                                        Badge(
                                            containerColor = AmberGold,
                                            contentColor = AmoledBlack
                                        ) {
                                            Text(totalStoriesCount.toString(), fontSize = 10.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "Biblioteca"
                                )
                            }
                        },
                        label = { Text("Biblioteca", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmoledBlack,
                            selectedTextColor = AmberGold,
                            indicatorColor = AmberGold,
                            unselectedIconColor = TextMutedAmoled,
                            unselectedTextColor = TextMutedAmoled
                        ),
                        modifier = Modifier.testTag("nav_library")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (progress.isRunning) {
                                        Badge(
                                            containerColor = AmberGold,
                                            contentColor = AmoledBlack
                                        ) {
                                            Text("...", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Sincronizar"
                                )
                            }
                        },
                        label = { Text("Descargar", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmoledBlack,
                            selectedTextColor = AmberGold,
                            indicatorColor = AmberGold,
                            unselectedIconColor = TextMutedAmoled,
                            unselectedTextColor = TextMutedAmoled
                        ),
                        modifier = Modifier.testTag("nav_download")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack)
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> LibraryScreen(
                        viewModel = viewModel,
                        onStoryClick = { id -> viewModel.openStory(id) },
                        onNavigateToScraper = { selectedTab = 1 }
                    )
                    1 -> CategoryScreen(
                        viewModel = viewModel,
                        onNavigateToLibrary = { selectedTab = 0 }
                    )
                }
            }
        }
    }
}
