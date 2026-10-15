package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemDarkSurfaceVariant
import com.example.ui.theme.GemViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    isSpeakingThis: Boolean,
    onSpeakClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLatestAiMessage: Boolean = false,
    onRegenerateClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onQuoteClick: ((String) -> Unit)? = null
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    var showActions by remember { mutableStateOf(false) }

    val wordCount = remember(message.content) {
        message.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // AI Avatar
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GemCyan, GemViolet))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Aurora AI",
                    tint = Color.Black,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) {
                    Color(0xFF0F3A4D)
                } else {
                    GemDarkSurfaceElevated
                },
                border = BorderStroke(
                    1.dp,
                    if (isUser) GemCyan.copy(alpha = 0.35f) else GemBorder.copy(alpha = 0.7f)
                ),
                tonalElevation = if (isUser) 4.dp else 2.dp,
                modifier = Modifier
                    .clickable { showActions = !showActions }
                    .testTag("chat_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header label (Voice badge / AI speaking badge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isVoice) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Wiadomość głosowa",
                                    tint = if (isUser) GemCyan else GemViolet,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Głos (Live Voice)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) GemCyan else GemViolet,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = if (isUser) "Ty" else "Aurora AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) Color(0xFF7DD3FC) else GemCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Speaking wave animation if playing audio
                        if (!isUser && isSpeakingThis) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpeakingWaveIndicator()
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mówi...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GemCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Message Content
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) Color.White else Color(0xFFF1F5F9),
                        lineHeight = 22.sp,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Footer Row: Timestamp & Reading info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                            if (!isUser && wordCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•  $wordCount słów",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Primary quick buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isUser) {
                                IconButton(
                                    onClick = onSpeakClick,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("tts_button_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isSpeakingThis) "Zatrzymaj mowę" else "Odtwórz na głos",
                                        tint = if (isSpeakingThis) GemCyan else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Aurora Message", message.content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Skopiowano do schowka", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("copy_button_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopiuj",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Expandable Extra Actions (Share, Regenerate, Quote, Delete)
                    AnimatedVisibility(
                        visible = showActions,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(GemBorder.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Quote to input
                                if (onQuoteClick != null) {
                                    IconButton(
                                        onClick = { onQuoteClick(message.content) },
                                        modifier = Modifier.size(28.dp).testTag("quote_button_${message.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = "Cytuj",
                                            tint = GemCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                // Share via Intent
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message.content)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Udostępnij wiadomość"))
                                    },
                                    modifier = Modifier.size(28.dp).testTag("share_button_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Udostępnij",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Regenerate (only for latest AI message)
                                if (!isUser && isLatestAiMessage && onRegenerateClick != null) {
                                    IconButton(
                                        onClick = onRegenerateClick,
                                        modifier = Modifier.size(28.dp).testTag("regenerate_button_${message.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Przegeneruj odpowiedź",
                                            tint = GemCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Delete message
                                if (onDeleteClick != null) {
                                    IconButton(
                                        onClick = onDeleteClick,
                                        modifier = Modifier.size(28.dp).testTag("delete_message_${message.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Usuń",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // User Avatar
        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F3A4D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Użytkownik",
                    tint = GemCyan,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Animated soundwave equalizer bars shown while Aurora AI is actively speaking a message via TTS.
 */
@Composable
fun SpeakingWaveIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "speech_wave")
    val h1 by transition.animateFloat(
        initialValue = 4f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 14f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 6f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 12f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "h4"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = modifier
    ) {
        listOf(h1, h2, h3, h4).forEach { heightVal ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(heightVal.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(GemCyan)
            )
        }
    }
}

/**
 * Shimmering typing / thinking bubble shown when Aurora is formulating an answer.
 */
@Composable
fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing_pulsing")
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "dot1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.5f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200, easing = LinearEasing), RepeatMode.Reverse), label = "dot2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400, easing = LinearEasing), RepeatMode.Reverse), label = "dot3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(GemCyan, GemViolet))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Aurora AI",
                tint = Color.Black,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            color = GemDarkSurfaceElevated,
            border = BorderStroke(1.dp, GemBorder.copy(alpha = 0.7f)),
            tonalElevation = 2.dp,
            modifier = Modifier.testTag("typing_indicator_bubble")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aurora pisze...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GemCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GemCyan.copy(alpha = alpha1)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GemCyan.copy(alpha = alpha2)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GemCyan.copy(alpha = alpha3)))
                }
            }
        }
    }
}
