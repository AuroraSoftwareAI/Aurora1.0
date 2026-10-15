package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemViolet
import kotlinx.coroutines.launch

/**
 * Modern Jetpack Compose Message List UI with:
 * - Differentiated user vs Aurora AI message speech bubbles
 * - Automatic and manual smooth scrolling mechanism
 * - Floating "Scroll to Bottom" pill indicator
 * - Live typing indicator
 * - Rich interaction actions (TTS, Copy, Quote, Share, Regenerate, Delete)
 */
@Composable
fun MessageList(
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    speakingId: Long?,
    onSpeakClick: (ChatMessageEntity) -> Unit,
    onRegenerateClick: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onQuoteMessage: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Smooth auto-scroll mechanism when new messages are added or generated
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Determine if user has scrolled up and show the "Scroll to Bottom" button
    val showScrollToBottom by remember {
        derivedStateOf {
            if (messages.size <= 2) {
                false
            } else {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex < messages.size - 1
            }
        }
    }

    val latestAiMessageId = remember(messages) {
        messages.lastOrNull { it.role == "model" }?.id
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            // Inviting Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(GemCyan.copy(alpha = 0.45f), GemViolet.copy(alpha = 0.2f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aurora_logo),
                            contentDescription = "Aurora AI",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Witaj w Aurora AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zadaj dowolne pytanie, rozmawiaj głosem w trybie Live Voice lub zarządzaj własną bazą wiedzy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            // LazyColumn message stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_lazy_column"),
                contentPadding = PaddingValues(top = 10.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        isSpeakingThis = speakingId == msg.id,
                        onSpeakClick = { onSpeakClick(msg) },
                        isLatestAiMessage = msg.id == latestAiMessageId,
                        onRegenerateClick = onRegenerateClick,
                        onDeleteClick = { onDeleteMessage(msg.id) },
                        onQuoteClick = onQuoteMessage
                    )
                }

                if (isGenerating) {
                    item(key = "typing_indicator") {
                        TypingIndicatorBubble()
                    }
                }
            }
        }

        // Floating "Scroll to Bottom" pill button
        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GemDarkSurfaceElevated.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, GemCyan.copy(alpha = 0.6f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            val targetIndex = if (isGenerating) messages.size else (messages.size - 1).coerceAtLeast(0)
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                    .testTag("scroll_to_bottom_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Przewiń do najnowszej",
                        tint = GemCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Na dół",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
