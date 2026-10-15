package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GemAccentAmber
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkBg
import com.example.ui.theme.GemDarkSurface
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemLivePulse
import com.example.ui.theme.GemViolet
import com.example.ui.viewmodel.ChatViewModel
import com.example.voice.TimbreStyle
import com.example.voice.VoiceProfile
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val personaSettings by viewModel.personaSettings.collectAsState()
    val voiceProfiles by viewModel.voiceProfiles.collectAsState()
    val confirmedVoiceProfile by viewModel.confirmedVoiceProfile.collectAsState()
    val previewingProfileId by viewModel.previewingProfileId.collectAsState()

    val speechRate by viewModel.speechRate.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val selectedTimbre by viewModel.selectedTimbre.collectAsState()

    var showVoiceSelectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GemDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Głos & Ustawienia Asystenta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GemDarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TTS Sound Profiles, Timbre, Pitch, and Speed
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier.testTag("natural_voices_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SpatialAudio,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Głos i Barwa Dźwięku (TTS)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Wszystkie dostępne profile są w języku polskim. Kliknij 'Wybierz głos', aby otworzyć listę głosów z automatycznym odsłuchem przy kliknięciu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active confirmed voice card with "Wybierz głos" and "Testuj" buttons
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GemDarkBg,
                        border = BorderStroke(1.dp, GemCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(GemLivePulse)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Aktywny głos w czacie:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = confirmedVoiceProfile.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GemCyan
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GemCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = confirmedVoiceProfile.qualityLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GemCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = confirmedVoiceProfile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showVoiceSelectionDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GemCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("open_voice_selection_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Wybierz głos",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.playVoicePreview() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GemCyan),
                                    border = BorderStroke(1.dp, GemCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_active_voice_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Odsłuchaj",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speed / Tempo Slider
                    val speedLabel = when {
                        speechRate < 0.85f -> "Zwolnione"
                        speechRate > 1.15f -> "Przyspieszone"
                        else -> "Normalne"
                    }
                    Text(
                        text = "Tempo mowy: ${String.format(Locale.US, "%.2f", speechRate)}x • $speedLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = GemCyan,
                            activeTrackColor = GemCyan,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_speech_rate")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pitch / Wysokość Tonu Slider
                    val pitchLabel = when {
                        pitch < 0.85f -> "Niski ton"
                        pitch > 1.15f -> "Wysoki ton"
                        else -> "Naturalna wysokość"
                    }
                    Text(
                        text = "Wysokość dźwięku: ${String.format(Locale.US, "%.2f", pitch)}x • $pitchLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.setPitch(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = GemAccentAmber,
                            activeTrackColor = GemAccentAmber,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_pitch")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Timbre / Barwa Głosu Options
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = GemViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Barwa i modulacja akustyczna głosu:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimbreStyle.entries.forEach { timbre ->
                            val isSelected = selectedTimbre == timbre
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) GemViolet.copy(alpha = 0.15f) else GemDarkBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GemViolet else GemBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setTimbreStyle(timbre) }
                                    .testTag("timbre_option_${timbre.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) GemViolet else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = timbre.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                        )
                                        Text(
                                            text = timbre.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Personality Traits Section (Humor, Boldness, Temperature, Quick Archetypes)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier.testTag("personality_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = GemViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ekspresja i Osobowość Asystenta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dopasuj temperament, poziom humoru oraz śmiałość wypowiedzi Aurory do swoich preferencji.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick archetype chips
                    Text(
                        text = "Szybki profil charakteru:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = personaSettings.humorLevel > 0.7f && personaSettings.boldnessLevel > 0.6f,
                            onClick = {
                                viewModel.updatePersonaSettings(humor = 0.85f, boldness = 0.75f, temperature = 0.8f)
                            },
                            label = { Text("Błyskotliwy", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = personaSettings.humorLevel < 0.4f,
                            onClick = {
                                viewModel.updatePersonaSettings(humor = 0.25f, boldness = 0.4f, temperature = 0.5f)
                            },
                            label = { Text("Ekspert", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = personaSettings.humorLevel in 0.4f..0.7f,
                            onClick = {
                                viewModel.updatePersonaSettings(humor = 0.55f, boldness = 0.5f, temperature = 0.7f)
                            },
                            label = { Text("Zrównoważony", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemAccentAmber,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Humor slider
                    val humorFeedback = when {
                        personaSettings.humorLevel > 0.7f -> "Cięty dowcip i inteligentne riposty"
                        personaSettings.humorLevel > 0.35f -> "Przyjazny z lekkim humorem"
                        else -> "Poważny i wyważony"
                    }
                    Text(
                        text = "Poziom dowcipu i humoru: ${(personaSettings.humorLevel * 100).toInt()}% • $humorFeedback",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = personaSettings.humorLevel,
                        onValueChange = { viewModel.updatePersonaSettings(humor = it) },
                        colors = SliderDefaults.colors(
                            thumbColor = GemViolet,
                            activeTrackColor = GemViolet,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_humor")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Boldness slider
                    val boldnessFeedback = when {
                        personaSettings.boldnessLevel > 0.6f -> "Bezpośredni i szczery"
                        else -> "Umiarkowany i dyplomatyczny"
                    }
                    Text(
                        text = "Bezpośredniość i śmiałość: ${(personaSettings.boldnessLevel * 100).toInt()}% • $boldnessFeedback",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = personaSettings.boldnessLevel,
                        onValueChange = { viewModel.updatePersonaSettings(boldness = it) },
                        colors = SliderDefaults.colors(
                            thumbColor = GemCyan,
                            activeTrackColor = GemCyan,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_boldness")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Temperature slider
                    Text(
                        text = "Kreatywność (Temperature): ${String.format(Locale.US, "%.2f", personaSettings.temperature)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = personaSettings.temperature,
                        onValueChange = { viewModel.updatePersonaSettings(temperature = it) },
                        valueRange = 0.2f..1.2f,
                        colors = SliderDefaults.colors(
                            thumbColor = GemAccentAmber,
                            activeTrackColor = GemAccentAmber,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_temperature")
                    )
                }
            }

            // Current Session & Data Management
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wyczyść bieżącą rozmowę",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Usuwa historię wiadomości wyłącznie z bieżącej sesji czatu",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Wyczyść czat",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // Room Local Database & Offline-First Storage Status
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier.testTag("room_storage_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GemLivePulse,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Lokalna Pamięć Room (SQLite)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Wszystkie sesje rozmów, historia wiadomości, profil stylu adaptacyjnego oraz wpisy bazy wiedzy są bezpiecznie przechowywane lokalnie na urządzeniu w bazie SQLite Room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status bazy: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Aktywna lokalnie • Niezależne rozmowy",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GemCyan
                        )
                    }
                }
            }
        }
    }

    // Dedicated Voice Selection Dialog / Page with automatic sample playback upon click and confirmation
    if (showVoiceSelectionDialog) {
        VoiceSelectionDialog(
            voiceProfiles = voiceProfiles,
            confirmedProfile = confirmedVoiceProfile,
            previewingProfileId = previewingProfileId,
            onPreviewVoice = { profile -> viewModel.playVoicePreview(profile) },
            onConfirmVoice = { selectedProfile ->
                viewModel.confirmVoiceProfile(selectedProfile)
                showVoiceSelectionDialog = false
            },
            onDismiss = {
                viewModel.ttsManager.cancelUtterance()
                showVoiceSelectionDialog = false
            }
        )
    }
}

/**
 * Dedicated Voice Selection View:
 * - Clicking on ANY voice in the list automatically plays its audio sample
 * - Selecting and clicking "Zatwierdź wybór" applies and commits it to the chat
 */
@Composable
private fun VoiceSelectionDialog(
    voiceProfiles: List<VoiceProfile>,
    confirmedProfile: VoiceProfile,
    previewingProfileId: String?,
    onPreviewVoice: (VoiceProfile) -> Unit,
    onConfirmVoice: (VoiceProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var stagedSelection by remember { mutableStateOf(confirmedProfile) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GemDarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = GemCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Wybór Głosu Asystenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zamknij",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Kliknij na dowolny głos, aby natychmiast go odsłuchać. Zaznacz pożądany głos i zatwierdź, aby wdrożyć go do czatu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(voiceProfiles, key = { it.id }) { profile ->
                        val isStaged = stagedSelection.id == profile.id
                        val isPreviewingThis = previewingProfileId == profile.id

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isPreviewingThis -> GemCyan.copy(alpha = 0.16f)
                                isStaged -> GemCyan.copy(alpha = 0.08f)
                                else -> GemDarkSurfaceElevated
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isPreviewingThis -> GemLivePulse
                                    isStaged -> GemCyan
                                    else -> GemBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Automatic playback upon clicking voice row + staged selection
                                    stagedSelection = profile
                                    onPreviewVoice(profile)
                                }
                                .testTag("voice_row_${profile.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isStaged) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isStaged) GemCyan else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isStaged) GemCyan else Color.White
                                        )

                                        if (confirmedProfile.id == profile.id) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GemLivePulse.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Aktywny w czacie",
                                                    color = GemLivePulse,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = profile.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = profile.qualityLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }

                                if (isPreviewingThis) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GemLivePulse.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Odtwarzanie próbki",
                                            tint = GemLivePulse,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Gra...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GemLivePulse,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmVoice(stagedSelection) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GemCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_confirm_voice_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zatwierdź i zastosuj głos",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Anuluj",
                    color = Color(0xFF94A3B8)
                )
            }
        }
    )
}
