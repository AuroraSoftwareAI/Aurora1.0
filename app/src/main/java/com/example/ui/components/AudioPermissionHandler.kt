package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkSurface
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemViolet

/**
 * State and callbacks for managing audio runtime permissions cleanly across the app.
 */
class AudioPermissionState(
    val hasPermission: () -> Boolean,
    val requestPermission: () -> Unit,
    val showRationaleDialog: Boolean,
    val isPermanentlyDenied: Boolean,
    val onDismissDialog: () -> Unit,
    val onOpenSettings: () -> Unit
)

/**
 * Rememberable hook managing RECORD_AUDIO permission checking, request launchers,
 * and user-friendly explanation dialogs when denied.
 */
@Composable
fun rememberAudioPermissionState(
    onPermissionGranted: () -> Unit
): AudioPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity

    var showRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showRationaleDialog = false
            onPermissionGranted()
        } else {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false
            isPermanentlyDenied = !shouldShowRationale
            showRationaleDialog = true
        }
    }

    return AudioPermissionState(
        hasPermission = {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        },
        requestPermission = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onPermissionGranted()
            } else {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        showRationaleDialog = showRationaleDialog,
        isPermanentlyDenied = isPermanentlyDenied,
        onDismissDialog = { showRationaleDialog = false },
        onOpenSettings = {
            showRationaleDialog = false
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    )
}

/**
 * Polished dialog explaining why microphone access is required for Live Voice.
 */
@Composable
fun AudioPermissionRationaleDialog(
    permissionState: AudioPermissionState
) {
    if (!permissionState.showRationaleDialog) return

    AlertDialog(
        onDismissRequest = permissionState.onDismissDialog,
        containerColor = GemDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(GemCyan.copy(alpha = 0.3f), GemViolet.copy(alpha = 0.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (permissionState.isPermanentlyDenied) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = GemCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (permissionState.isPermanentlyDenied) "Mikrofon zablokowany" else "Wymagany dostęp do mikrofonu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (permissionState.isPermanentlyDenied) {
                        "Dostęp do mikrofonu został zablokowany w ustawieniach urządzenia. Aby rozmawiać z asystentem Aurora w czasie rzeczywistym w trybie Live Voice, przejdź do ustawień aplikacji i zezwól na korzystanie z mikrofonu."
                    } else {
                        "Funkcja Live Voice w Aurora AI pozwala na naturalną rozmowę głosową na żywo. Twój głos jest przetwarzany wyłącznie w celu rozpoznawania wypowiedzi i generowania odpowiedzi asystenta."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 20.sp
                )

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                    border = BorderStroke(1.dp, GemBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Voice reaguje w ułamku sekundy na Twoje pytania.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (permissionState.isPermanentlyDenied) {
                        permissionState.onOpenSettings()
                    } else {
                        permissionState.requestPermission()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GemCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("permission_confirm_button")
            ) {
                if (permissionState.isPermanentlyDenied) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Otwórz ustawienia", fontWeight = FontWeight.Bold)
                } else {
                    Text("Zezwól na mikrofon", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = permissionState.onDismissDialog,
                modifier = Modifier.testTag("permission_dismiss_button")
            ) {
                Text("Nie teraz", color = Color(0xFF94A3B8))
            }
        }
    )
}

/**
 * In-screen banner displayed inside LiveVoiceScreen when permission is missing.
 */
@Composable
fun AudioPermissionMissingBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
        border = BorderStroke(1.dp, GemCyan.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_permission_missing_banner")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GemCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = GemCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Wymagane uprawnienie do mikrofonu",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Aby Aurora AI mogła słyszeć Twój głos w trybie na żywo, zezwól na dostęp do mikrofonu.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = GemCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("grant_mic_in_banner_button")
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Włącz mikrofon", fontWeight = FontWeight.Bold)
            }
        }
    }
}
