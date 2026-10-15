package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AudioPermissionMissingBanner
import com.example.ui.components.AudioPermissionRationaleDialog
import com.example.ui.components.VoiceOrbState
import com.example.ui.components.VoiceVisualizerOrb
import com.example.ui.components.rememberAudioPermissionState
import com.example.ui.theme.GemAccentAmber
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkBg
import com.example.ui.theme.GemDarkSurface
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemDarkSurfaceVariant
import com.example.ui.theme.GemLivePulse
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun LiveVoiceScreen(
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onClose()
    }

    val orbState by viewModel.voiceOrbState.collectAsState()
    val subtitle by viewModel.liveSubtitle.collectAsState()
    val amplitude by viewModel.speechAmplitude.collectAsState()
    val handsFree by viewModel.handsFreeLoop.collectAsState()
    val personaSettings by viewModel.personaSettings.collectAsState()

    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = {
            viewModel.startLiveListening()
        }
    )

    AudioPermissionRationaleDialog(permissionState = permissionState)

    val hasAudioPermission = permissionState.hasPermission()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B12),
                        GemDarkBg,
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(16.dp)
            .testTag("live_voice_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GemDarkSurfaceElevated)
                        .testTag("live_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zamknij tryb Live",
                        tint = Color.White
                    )
                }

                // Live status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GemDarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GemCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("live_status_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aurora_logo),
                            contentDescription = "Aurora Logo",
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !hasAudioPermission -> Color(0xFFEF4444)
                                        orbState == VoiceOrbState.LISTENING -> GemLivePulse
                                        orbState == VoiceOrbState.THINKING -> GemAccentAmber
                                        orbState == VoiceOrbState.SPEAKING -> GemCyan
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE VOICE • INTERRUPT ON",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GemDarkSurfaceElevated)
                        .testTag("live_switch_to_chat")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Przejdź do czatu",
                        tint = GemCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Microphone permission missing banner if not granted
            if (!hasAudioPermission) {
                AudioPermissionMissingBanner(
                    onRequestPermission = { permissionState.requestPermission() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Center Dynamic Visualizer Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            if (!hasAudioPermission) {
                                permissionState.requestPermission()
                            } else if (orbState == VoiceOrbState.SPEAKING) {
                                // Instant interrupt on touch
                                viewModel.handleLiveInterruption()
                            } else {
                                viewModel.toggleLiveListening()
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VoiceVisualizerOrb(
                        state = if (!hasAudioPermission) VoiceOrbState.IDLE else orbState,
                        amplitude = if (!hasAudioPermission) 0f else amplitude,
                        size = 230.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Orb state indicator
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GemDarkSurfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier.clickable {
                        if (!hasAudioPermission) {
                            permissionState.requestPermission()
                        } else if (orbState == VoiceOrbState.SPEAKING) {
                            viewModel.handleLiveInterruption()
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        val stateIcon = when {
                            !hasAudioPermission -> Icons.Default.MicOff
                            orbState == VoiceOrbState.LISTENING -> Icons.Default.Hearing
                            orbState == VoiceOrbState.THINKING -> Icons.Default.Sync
                            orbState == VoiceOrbState.SPEAKING -> Icons.Default.RecordVoiceOver
                            else -> Icons.Default.Mic
                        }
                        Icon(
                            imageVector = stateIcon,
                            contentDescription = null,
                            tint = when {
                                !hasAudioPermission -> Color(0xFFEF4444)
                                orbState == VoiceOrbState.LISTENING -> GemLivePulse
                                orbState == VoiceOrbState.THINKING -> GemAccentAmber
                                orbState == VoiceOrbState.SPEAKING -> GemCyan
                                else -> Color.White
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !hasAudioPermission -> "Wymagane uprawnienie do mikrofonu (kliknij)"
                                orbState == VoiceOrbState.LISTENING -> "Słucham Cię... Mów teraz"
                                orbState == VoiceOrbState.THINKING -> "Aurora analizuje i formułuje odpowiedź..."
                                orbState == VoiceOrbState.SPEAKING -> "Aurora odpowiada • Mów lub dotknij, aby przerwać"
                                else -> "Dotknij kuli, aby zacząć mówić"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Live Subtitle transcript / answer box
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GemDarkSurface.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GemCyan.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = subtitle,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "subtitle_anim"
                    ) { currentText ->
                        Text(
                            text = if (currentText.isBlank()) "Powiedz cokolwiek lub zadaj pytanie. Wypowiedź możesz w każdej chwili przerwać swoim głosem." else currentText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentText.isBlank()) Color(0xFF64748B) else Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Bottom Voice Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hands-Free Loop Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Ciągła rozmowa (Hands-Free):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = handsFree,
                        onCheckedChange = { viewModel.toggleHandsFreeLoop() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GemCyan,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("hands_free_switch")
                    )
                }

                // Control Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Instant Interrupt Button
                    IconButton(
                        onClick = { viewModel.handleLiveInterruption() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GemDarkSurfaceElevated)
                            .testTag("live_stop_tts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Natychmiast przerwij wypowiedź",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Mic Action Button
                    Surface(
                        shape = CircleShape,
                        color = when {
                            !hasAudioPermission -> Color(0xFFEF4444)
                            orbState == VoiceOrbState.LISTENING -> GemLivePulse
                            else -> GemCyan
                        },
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                if (hasAudioPermission) {
                                    viewModel.toggleLiveListening()
                                } else {
                                    permissionState.requestPermission()
                                }
                            }
                            .testTag("live_mic_action_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (hasAudioPermission && orbState == VoiceOrbState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (hasAudioPermission && orbState == VoiceOrbState.LISTENING) "Wyłącz mikrofon" else "Włącz mikrofon",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Voice Interruption Mode Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GemDarkSurfaceElevated,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Przerywanie",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Aktywne",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GemCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
