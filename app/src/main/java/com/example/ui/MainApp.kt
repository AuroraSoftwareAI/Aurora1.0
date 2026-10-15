package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.KnowledgeScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkBg
import com.example.ui.theme.GemDarkSurface
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun MainApp(viewModel: ChatViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isLiveModeOpen by viewModel.isLiveModeOpen.collectAsState()
    val context = LocalContext.current

    val audioPermissionState = com.example.ui.components.rememberAudioPermissionState(
        onPermissionGranted = {
            viewModel.setLiveModeOpen(true)
        }
    )

    fun openLiveModeWithPermission() {
        audioPermissionState.requestPermission()
    }

    com.example.ui.components.AudioPermissionRationaleDialog(permissionState = audioPermissionState)

    BackHandler(enabled = isLiveModeOpen) {
        viewModel.setLiveModeOpen(false)
    }

    BackHandler(enabled = !isLiveModeOpen && selectedTab != 0) {
        selectedTab = 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = GemDarkBg,
            bottomBar = {
                if (!isLiveModeOpen) {
                    NavigationBar(
                        containerColor = GemDarkSurface,
                        contentColor = GemCyan,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Czat"
                                )
                            },
                            label = { Text("Czat") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GemCyan,
                                indicatorColor = GemCyan,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_chat")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Wiedza"
                                )
                            },
                            label = { Text("Baza Wiedzy") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GemCyan,
                                indicatorColor = GemCyan,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_knowledge")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ustawienia"
                                )
                            },
                            label = { Text("Osobowość") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GemCyan,
                                indicatorColor = GemCyan,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> ChatScreen(
                        viewModel = viewModel,
                        onOpenLiveVoice = { openLiveModeWithPermission() },
                        onNavigateToKnowledge = { selectedTab = 1 }
                    )
                    1 -> KnowledgeScreen(viewModel = viewModel)
                    2 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Fullscreen Live Voice Mode Overlay
        AnimatedVisibility(
            visible = isLiveModeOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            LiveVoiceScreen(
                viewModel = viewModel,
                onClose = { viewModel.setLiveModeOpen(false) }
            )
        }
    }
}
