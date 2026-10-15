package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import com.example.data.model.SourceType
import com.example.ui.theme.GemAccentAmber
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemDarkSurfaceVariant
import com.example.ui.theme.GemViolet

@Composable
fun KnowledgeSourceCard(
    source: KnowledgeSource,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isEnabled) GemDarkSurfaceElevated else GemDarkSurfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (source.isEnabled) GemCyan.copy(alpha = 0.35f) else GemBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("source_card_${source.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (source.sourceType) {
                        SourceType.TEXT_NOTE -> Icons.Default.Description
                        SourceType.DOCUMENT -> Icons.AutoMirrored.Filled.MenuBook
                        SourceType.URL_REFERENCE -> Icons.Default.Language
                        SourceType.USER_PROFILE -> Icons.Default.Psychology
                        SourceType.CUSTOM_RULE -> Icons.AutoMirrored.Filled.Rule
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = source.sourceType.name,
                        tint = if (source.isEnabled) GemCyan else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (source.isEnabled) Color.White else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = source.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GemCyan,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("source_switch_${source.id}")
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("source_edit_${source.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edytuj źródło",
                            tint = GemCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("source_delete_${source.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń źródło",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = source.content,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GemDarkSurfaceVariant
            ) {
                Text(
                    text = when (source.sourceType) {
                        SourceType.TEXT_NOTE -> "Notatka tekstowa"
                        SourceType.DOCUMENT -> "Dokument / Podręcznik"
                        SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                        SourceType.USER_PROFILE -> "Profil Użytkownika"
                        SourceType.CUSTOM_RULE -> "Reguła postępowania"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = GemCyan,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun LearnedFactCard(
    fact: LearnedFact,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fact.isActive) GemDarkSurfaceElevated else GemDarkSurfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (fact.isActive) GemViolet.copy(alpha = 0.4f) else GemBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fact_card_${fact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (fact.category) {
                    FactCategory.STYLE -> Icons.Default.RecordVoiceOver
                    FactCategory.VOCABULARY -> Icons.Default.Description
                    FactCategory.HUMOR -> Icons.Default.Lightbulb
                    FactCategory.PREFERENCE -> Icons.Default.Psychology
                    FactCategory.SUPPORT_MEMORY -> Icons.Default.Psychology
                    FactCategory.GENERAL -> Icons.Default.Lightbulb
                }
                Icon(
                    imageVector = icon,
                    contentDescription = fact.category.name,
                    tint = if (fact.isActive) GemViolet else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = fact.fact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (fact.isActive) Color.White else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (fact.category) {
                            FactCategory.STYLE -> "Styl i ton mowy (Adaptacyjny)"
                            FactCategory.VOCABULARY -> "Słownictwo"
                            FactCategory.HUMOR -> "Humor i dowcip"
                            FactCategory.PREFERENCE -> "Osobista preferencja"
                            FactCategory.SUPPORT_MEMORY -> "Pamięć ogólna"
                            FactCategory.GENERAL -> "Fakt o użytkowniku"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = GemAccentAmber,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = fact.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GemViolet,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("fact_switch_${fact.id}")
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("fact_delete_${fact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń fakt",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
