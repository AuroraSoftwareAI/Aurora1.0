package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.SourceType
import com.example.ui.components.KnowledgeSourceCard
import com.example.ui.components.LearnedFactCard
import com.example.ui.theme.GemAccentAmber
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkBg
import com.example.ui.theme.GemDarkSurface
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemDarkSurfaceVariant
import com.example.ui.theme.GemViolet
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.knowledgeSources.collectAsState()
    val facts by viewModel.learnedFacts.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showAddFactDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<KnowledgeSource?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<SourceType?>(null) }
    var onlyActiveFilter by remember { mutableStateOf(false) }

    val filteredSources = remember(sources, searchQuery, selectedTypeFilter, onlyActiveFilter) {
        sources.filter { source ->
            val matchesQuery = searchQuery.isBlank() ||
                    source.title.contains(searchQuery, ignoreCase = true) ||
                    source.content.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedTypeFilter == null || source.sourceType == selectedTypeFilter
            val matchesActive = !onlyActiveFilter || source.isEnabled
            matchesQuery && matchesType && matchesActive
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GemDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Uczenie & Baza Wiedzy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GemDarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        showAddSourceDialog = true
                    } else {
                        showAddFactDialog = true
                    }
                },
                containerColor = GemCyan,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("add_knowledge_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTabIndex == 0) "Dodaj źródło" else "Dodaj fakt o stylu"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = GemDarkSurface,
                contentColor = GemCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = GemCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Źródła Wiedzy (${sources.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) GemCyan else Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.testTag("tab_sources")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Pamięć Stylu (${facts.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 1) GemCyan else Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.testTag("tab_style_memory")
                )
            }

            // Info banner explaining the learning mechanism
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedTabIndex == 0) Icons.Default.Storage else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (selectedTabIndex == 0) GemCyan else GemAccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (selectedTabIndex == 0) {
                                "Lokalna Baza Room: ${sources.count { it.isEnabled }} z ${sources.size} aktywnych"
                            } else {
                                "Pamięć Stylu i Komunikacji (${facts.count { it.isActive }} aktywnych)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (selectedTabIndex == 0) {
                                "Aurora AI uwzględnia aktywne źródła z bazy Room przy każdej odpowiedzi, działając na twoich danych."
                            } else {
                                "Asystent automatycznie uczy się twojego słownictwa, humoru i preferencji w trakcie rozmów."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Content List
            if (selectedTabIndex == 0) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Szukaj w bazie wiedzy...", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Szukaj",
                            tint = GemCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Wyczyść wyszukiwanie",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GemDarkSurface,
                        unfocusedContainerColor = GemDarkSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("search_sources_input")
                )

                // Filter chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null && !onlyActiveFilter,
                            onClick = {
                                selectedTypeFilter = null
                                onlyActiveFilter = false
                            },
                            label = { Text("Wszystkie (${sources.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = onlyActiveFilter,
                            onClick = { onlyActiveFilter = !onlyActiveFilter },
                            label = { Text("Aktywne (${sources.count { it.isEnabled }})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == SourceType.TEXT_NOTE,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == SourceType.TEXT_NOTE) null else SourceType.TEXT_NOTE
                            },
                            label = { Text("Notatki") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == SourceType.DOCUMENT,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == SourceType.DOCUMENT) null else SourceType.DOCUMENT
                            },
                            label = { Text("Dokumenty") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == SourceType.CUSTOM_RULE,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == SourceType.CUSTOM_RULE) null else SourceType.CUSTOM_RULE
                            },
                            label = { Text("Reguły") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == SourceType.USER_PROFILE,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == SourceType.USER_PROFILE) null else SourceType.USER_PROFILE
                            },
                            label = { Text("Profil") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == SourceType.URL_REFERENCE,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == SourceType.URL_REFERENCE) null else SourceType.URL_REFERENCE
                            },
                            label = { Text("Linki") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                if (sources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak źródeł wiedzy w bazie Room.\nDotknij '+', aby dodać notatki, fragmenty dokumentów lub reguły postępowania.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (filteredSources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Brak wyników pasujących do filtra.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedTypeFilter = null
                                    onlyActiveFilter = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GemDarkSurfaceElevated, contentColor = GemCyan)
                            ) {
                                Text("Wyczyść filtry")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSources, key = { it.id }) { source ->
                            KnowledgeSourceCard(
                                source = source,
                                onToggle = { enabled -> viewModel.toggleKnowledgeSource(source, enabled) },
                                onEdit = { editingSource = source },
                                onDelete = { viewModel.deleteKnowledgeSource(source) }
                            )
                        }
                    }
                }
            } else {
                if (facts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak nauczonych cech stylu.\nProwadź rozmowy lub dodaj ręcznie cechy, które Gemini powinien naśladować.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(facts, key = { it.id }) { fact ->
                            LearnedFactCard(
                                fact = fact,
                                onToggle = { active -> viewModel.toggleLearnedFact(fact, active) },
                                onDelete = { viewModel.deleteLearnedFact(fact) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Source Dialog
    if (showAddSourceDialog) {
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onConfirm = { title, content, type ->
                viewModel.addKnowledgeSource(title, content, type)
                showAddSourceDialog = false
            }
        )
    }

    // Edit Source Dialog (CRUD Update)
    editingSource?.let { sourceToEdit ->
        EditSourceDialog(
            source = sourceToEdit,
            onDismiss = { editingSource = null },
            onConfirm = { updatedTitle, updatedContent, updatedType ->
                viewModel.updateKnowledgeSource(sourceToEdit.id, updatedTitle, updatedContent, updatedType)
                editingSource = null
            }
        )
    }

    // Add Fact Dialog
    if (showAddFactDialog) {
        AddFactDialog(
            onDismiss = { showAddFactDialog = false },
            onConfirm = { fact, category ->
                viewModel.addLearnedFact(fact, category)
                showAddFactDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, type: SourceType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SourceType.TEXT_NOTE) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GemDarkSurface,
        title = {
            Text("Dodaj źródło wiedzy", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Szybki szablon:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Zasady Komunikacji Asystenta"
                                selectedType = SourceType.CUSTOM_RULE
                                content = "Odpowiadaj bezpośrednio, zwięźle i konkretnie. Unikaj zbędnego korporacyjnego lania wody."
                            },
                            label = { Text("Reguła stylu", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = GemCyan)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "O Mnie & Moje Preferencje"
                                selectedType = SourceType.USER_PROFILE
                                content = "Interesuję się nowymi technologiami, AI oraz programowaniem w Androidzie (Kotlin/Compose)."
                            },
                            label = { Text("Profil", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = GemCyan)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Kontekst Mojego Projektu"
                                selectedType = SourceType.DOCUMENT
                                content = "Projekt Aurora AI: inteligentny asystent głosowy działający na urządzeniu z lokalną bazą Room."
                            },
                            label = { Text("Dokument", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(labelColor = GemCyan)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł źródła") },
                    placeholder = { Text("np. Wytyczne klienta, Artykuł") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_source_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            SourceType.TEXT_NOTE -> "Notatka tekstowa"
                            SourceType.DOCUMENT -> "Dokument / Podręcznik"
                            SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                            SourceType.USER_PROFILE -> "Profil Użytkownika"
                            SourceType.CUSTOM_RULE -> "Reguła postępowania"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GemCyan,
                            unfocusedBorderColor = GemBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        containerColor = GemDarkSurfaceElevated
                    ) {
                        SourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            SourceType.TEXT_NOTE -> "Notatka tekstowa"
                                            SourceType.DOCUMENT -> "Dokument / Podręcznik"
                                            SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                                            SourceType.USER_PROFILE -> "Profil Użytkownika"
                                            SourceType.CUSTOM_RULE -> "Reguła postępowania"
                                        },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Treść wiedzy / tekstu") },
                    placeholder = { Text("Wklej tutaj treść, którą Aurora AI ma się kierować...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("dialog_source_content"),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Zapis do lokalnej bazy Room",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${content.length} znaków",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (content.isNotBlank()) GemCyan else Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content, selectedType) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GemCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_source_confirm")
            ) {
                Text("Zapisz", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color(0xFF94A3B8))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFactDialog(
    onDismiss: () -> Unit,
    onConfirm: (fact: String, category: FactCategory) -> Unit
) {
    var factText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FactCategory.STYLE) }
    var catDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GemDarkSurface,
        title = {
            Text("Dodaj cechę stylu / fakt", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = factText,
                    onValueChange = { factText = it },
                    label = { Text("Fakt lub preferencja stylu") },
                    placeholder = { Text("np. Używaj ironii, Zwracaj się po imieniu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_fact_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemViolet,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = catDropdownExpanded,
                    onExpandedChange = { catDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedCategory) {
                            FactCategory.STYLE -> "Styl i ton mowy (Adaptacyjny)"
                            FactCategory.VOCABULARY -> "Słownictwo i slang"
                            FactCategory.HUMOR -> "Poczucie humoru"
                            FactCategory.PREFERENCE -> "Osobista preferencja"
                            FactCategory.SUPPORT_MEMORY -> "Pamięć ogólna"
                            FactCategory.GENERAL -> "Wiedza o mnie"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GemViolet,
                            unfocusedBorderColor = GemBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false },
                        containerColor = GemDarkSurfaceElevated
                    ) {
                        FactCategory.entries.filter { it != FactCategory.SUPPORT_MEMORY }.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (cat) {
                                            FactCategory.STYLE -> "Styl i ton mowy (Adaptacyjny)"
                                            FactCategory.VOCABULARY -> "Słownictwo i slang"
                                            FactCategory.HUMOR -> "Poczucie humoru"
                                            FactCategory.PREFERENCE -> "Osobista preferencja"
                                            FactCategory.SUPPORT_MEMORY -> "Pamięć ogólna"
                                            FactCategory.GENERAL -> "Wiedza o mnie"
                                        },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(factText, selectedCategory) },
                enabled = factText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GemViolet, contentColor = Color.White),
                modifier = Modifier.testTag("dialog_fact_confirm")
            ) {
                Text("Zapamiętaj", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color(0xFF94A3B8))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSourceDialog(
    source: KnowledgeSource,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, type: SourceType) -> Unit
) {
    var title by remember { mutableStateOf(source.title) }
    var content by remember { mutableStateOf(source.content) }
    var selectedType by remember { mutableStateOf(source.sourceType) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GemDarkSurface,
        title = {
            Text("Edytuj źródło wiedzy", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł źródła") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_edit_source_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            SourceType.TEXT_NOTE -> "Notatka tekstowa"
                            SourceType.DOCUMENT -> "Dokument / Podręcznik"
                            SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                            SourceType.USER_PROFILE -> "Profil Użytkownika"
                            SourceType.CUSTOM_RULE -> "Reguła postępowania"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GemCyan,
                            unfocusedBorderColor = GemBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        containerColor = GemDarkSurfaceElevated
                    ) {
                        SourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            SourceType.TEXT_NOTE -> "Notatka tekstowa"
                                            SourceType.DOCUMENT -> "Dokument / Podręcznik"
                                            SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                                            SourceType.USER_PROFILE -> "Profil Użytkownika"
                                            SourceType.CUSTOM_RULE -> "Reguła postępowania"
                                        },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Treść wiedzy / tekstu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("dialog_edit_source_content"),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Aktualizacja w bazie Room",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${content.length} znaków",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (content.isNotBlank()) GemCyan else Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content, selectedType) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GemCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_edit_source_confirm")
            ) {
                Text("Zaktualizuj", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color(0xFF94A3B8))
            }
        }
    )
}
