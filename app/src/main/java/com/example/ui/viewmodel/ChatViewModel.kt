package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import com.example.data.model.SourceType
import com.example.data.remote.ApiDiagnosticResult
import com.example.data.remote.ApiDiagnosticStatus
import com.example.data.remote.GeminiRepository
import com.example.data.remote.PersonaSettings
import com.example.data.repository.ChatRepository
import com.example.data.repository.KnowledgeSourceRepository
import com.example.ui.components.VoiceOrbState
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import com.example.voice.VoiceProfile
import com.example.voice.TimbreStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    val chatRepository = ChatRepository(chatDao)
    private val knowledgeDao = database.knowledgeDao()
    private val knowledgeSourceDao = database.knowledgeSourceDao()
    val knowledgeSourceRepository = KnowledgeSourceRepository(knowledgeSourceDao)
    private val geminiRepo = GeminiRepository(knowledgeDao)

    val ttsManager = TextToSpeechManager(application)
    private lateinit var speechManager: SpeechRecognizerManager

    // Active session (each chat is an independent conversation)
    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = chatRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        chatRepository.getMessagesForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeSources: StateFlow<List<KnowledgeSource>> = knowledgeSourceRepository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedFacts: StateFlow<List<LearnedFact>> = knowledgeDao.getAllFacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adaptiveStyleFacts: StateFlow<List<LearnedFact>> = knowledgeDao.getFactsByCategoryFlow(FactCategory.STYLE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Persona settings
    private val _personaSettings = MutableStateFlow(PersonaSettings())
    val personaSettings: StateFlow<PersonaSettings> = _personaSettings.asStateFlow()

    // Live Voice state
    private val _isLiveModeOpen = MutableStateFlow(false)
    val isLiveModeOpen: StateFlow<Boolean> = _isLiveModeOpen.asStateFlow()

    private val _voiceOrbState = MutableStateFlow(VoiceOrbState.IDLE)
    val voiceOrbState: StateFlow<VoiceOrbState> = _voiceOrbState.asStateFlow()

    private val _liveSubtitle = MutableStateFlow("")
    val liveSubtitle: StateFlow<String> = _liveSubtitle.asStateFlow()

    private val _handsFreeLoop = MutableStateFlow(true)
    val handsFreeLoop: StateFlow<Boolean> = _handsFreeLoop.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _apiDiagnosticResult = MutableStateFlow<ApiDiagnosticResult?>(null)
    val apiDiagnosticResult: StateFlow<ApiDiagnosticResult?> = _apiDiagnosticResult.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    // Direct STT Dictation in chat window
    private val _isDictatingText = MutableStateFlow(false)
    val isDictatingText: StateFlow<Boolean> = _isDictatingText.asStateFlow()

    private val _dictatedText = MutableStateFlow("")
    val dictatedText: StateFlow<String> = _dictatedText.asStateFlow()

    val speechAmplitude: StateFlow<Float>

    init {
        speechManager = SpeechRecognizerManager(
            context = application,
            onResult = { recognizedText -> handleSpeechRecognized(recognizedText) },
            onErrorRecovery = {
                if (_isLiveModeOpen.value && _handsFreeLoop.value && !_isGenerating.value) {
                    if (_voiceOrbState.value != VoiceOrbState.SPEAKING) {
                        _voiceOrbState.value = VoiceOrbState.IDLE
                        _liveSubtitle.value = "Dotknij kuli lub mów dalej..."
                    }
                } else if (_voiceOrbState.value == VoiceOrbState.LISTENING) {
                    _voiceOrbState.value = VoiceOrbState.IDLE
                }
            }
        )
        speechAmplitude = speechManager.rmsLevel

        // Configure instant interruption detector: halts model audio in fraction of second
        speechManager.onInterruptionDetected = {
            if (_isLiveModeOpen.value && (_voiceOrbState.value == VoiceOrbState.SPEAKING || ttsManager.isSpeaking.value)) {
                handleLiveInterruption()
            }
        }

        // Listen to TTS state
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _voiceOrbState.value = VoiceOrbState.SPEAKING
                    // In Live Mode with interruption enabled, keep or trigger microphone listening
                    if (_isLiveModeOpen.value && !speechManager.isListening.value) {
                        speechManager.startListening()
                    }
                } else {
                    _speakingMessageId.value = null
                    if (_isLiveModeOpen.value) {
                        if (_handsFreeLoop.value && !_isGenerating.value) {
                            startLiveListening()
                        } else {
                            _voiceOrbState.value = VoiceOrbState.IDLE
                        }
                    } else if (_voiceOrbState.value == VoiceOrbState.SPEAKING) {
                        _voiceOrbState.value = VoiceOrbState.IDLE
                    }
                }
            }
        }

        // Listen to partial speech text in Live Mode
        viewModelScope.launch {
            speechManager.partialText.collect { partial ->
                if (partial.isNotBlank() && _voiceOrbState.value == VoiceOrbState.LISTENING) {
                    _liveSubtitle.value = partial
                }
            }
        }

        // Listen to speech manager errors
        viewModelScope.launch {
            speechManager.errorMessage.collect { err ->
                err?.let {
                    _errorMessage.value = it
                    _voiceOrbState.value = VoiceOrbState.IDLE
                    if (_isLiveModeOpen.value) {
                        _liveSubtitle.value = it
                    }
                    _isDictatingText.value = false
                }
            }
        }

        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch {
            // Clean up duplicates and obsolete repeated assumptions
            cleanupDuplicateAndObsoleteData()

            val sources = knowledgeDao.getActiveSources()
            if (sources.isEmpty()) {
                knowledgeDao.insertSource(
                    KnowledgeSource(
                        title = "Profil Komunikacji",
                        content = "Użytkownik ceni bezpośrednią komunikację, wysoki poziom merytoryczny i wyrazisty styl odpowiedzi bez owijania w bawełnę.",
                        sourceType = SourceType.USER_PROFILE,
                        isEnabled = true
                    )
                )
                knowledgeDao.insertSource(
                    KnowledgeSource(
                        title = "Specyfikacja Projektu Aurora AI",
                        content = "Asystent osobisty z czatem tekstowym i głosowym Live Voice, syntezą mowy TTS, rozpoznawaniem mowy STT oraz lokalną bazą wiedzy Room.",
                        sourceType = SourceType.DOCUMENT,
                        isEnabled = true
                    )
                )
            }

            val facts = knowledgeDao.getActiveFacts()
            if (facts.isEmpty()) {
                knowledgeDao.insertFact(
                    LearnedFact(
                        fact = "Błyskotliwy, dynamiczny styl wypowiedzi z szybkim przechodzeniem do sedna sprawy",
                        category = FactCategory.STYLE,
                        isActive = true
                    )
                )
            }

            // Ensure current session exists
            val existingSessions = chatRepository.allSessions.first()
            if (existingSessions.isNotEmpty()) {
                _currentSessionId.value = existingSessions.first().sessionId
            } else {
                chatRepository.insertSession(
                    ChatSessionEntity(
                        sessionId = _currentSessionId.value,
                        title = "Rozmowa z Aurora AI"
                    )
                )
            }
        }
    }

    /**
     * Cleans up duplicate knowledge entries, deletes obsolete Support Mode remnants,
     * and ensures no duplicate or repeated facts clutter the memory bank.
     */
    private suspend fun cleanupDuplicateAndObsoleteData() {
        try {
            // 1. Deduplicate Knowledge Sources
            val allSources = knowledgeDao.getActiveSources()
            val seenTitles = mutableSetOf<String>()
            val seenContents = mutableSetOf<String>()
            for (source in allSources) {
                val normTitle = source.title.trim().lowercase()
                val normContent = source.content.trim().lowercase()
                if (seenTitles.contains(normTitle) || seenContents.contains(normContent)) {
                    knowledgeDao.deleteSource(source)
                } else {
                    seenTitles.add(normTitle)
                    seenContents.add(normContent)
                }
            }

            // 2. Clear obsolete Support Mode memories
            knowledgeDao.clearFactsByCategory(FactCategory.SUPPORT_MEMORY)

            // 3. Deduplicate Facts & remove repetitions of knowledge sources
            val allFacts = knowledgeDao.getActiveFacts()
            val sourcesRemaining = knowledgeDao.getActiveSources()
            val seenFacts = mutableSetOf<String>()

            for (fact in allFacts) {
                val normFact = fact.fact.trim().lowercase()
                val isRedundantWithSources = sourcesRemaining.any { source ->
                    val sContent = source.content.lowercase()
                    sContent.contains(normFact) || (normFact.length > 25 && sContent.contains(normFact.take(25)))
                }
                val isDuplicate = seenFacts.contains(normFact) || seenFacts.any { existing ->
                    existing.contains(normFact) || normFact.contains(existing)
                }
                val isObsolete = normFact.contains("support mode") || normFact.contains("tryb wsparcia")

                if (isRedundantWithSources || isDuplicate || isObsolete) {
                    knowledgeDao.deleteFact(fact)
                } else {
                    seenFacts.add(normFact)
                }
            }
        } catch (_: Exception) {}
    }

    // --- LIVE VOICE INTERRUPTION MODE ---

    /**
     * Instantly cuts off the TTS audio stream in a fraction of a second and immediately switches to listening.
     */
    fun handleLiveInterruption() {
        ttsManager.cancelUtterance()
        _speakingMessageId.value = null
        _voiceOrbState.value = VoiceOrbState.LISTENING
        _liveSubtitle.value = "Przerwano – słucham Cię..."
        if (!speechManager.isListening.value) {
            speechManager.startListening()
        }
    }

    fun setLiveModeOpen(isOpen: Boolean) {
        _isLiveModeOpen.value = isOpen
        if (isOpen) {
            if (_isDictatingText.value) {
                stopDictation()
            }
            _liveSubtitle.value = "Dotknij kuli lub mów, aby rozpocząć..."
            if (_handsFreeLoop.value) {
                startLiveListening()
            }
        } else {
            stopLiveListening()
            ttsManager.cancelUtterance()
            _voiceOrbState.value = VoiceOrbState.IDLE
        }
    }

    fun toggleHandsFreeLoop() {
        _handsFreeLoop.value = !_handsFreeLoop.value
    }

    fun startLiveListening() {
        ttsManager.cancelUtterance()
        _voiceOrbState.value = VoiceOrbState.LISTENING
        _liveSubtitle.value = "Słucham cię..."
        speechManager.startListening()
    }

    fun stopLiveListening() {
        speechManager.stopListening()
        if (_voiceOrbState.value == VoiceOrbState.LISTENING) {
            _voiceOrbState.value = VoiceOrbState.IDLE
        }
    }

    fun toggleLiveListening() {
        if (_voiceOrbState.value == VoiceOrbState.SPEAKING) {
            handleLiveInterruption()
        } else if (_voiceOrbState.value == VoiceOrbState.LISTENING) {
            stopLiveListening()
        } else {
            startLiveListening()
        }
    }

    private fun handleSpeechRecognized(text: String) {
        if (text.isBlank()) {
            if (_isLiveModeOpen.value && _handsFreeLoop.value) {
                startLiveListening()
            }
            return
        }
        _liveSubtitle.value = text
        sendMessage(text, isVoice = true)
    }

    // --- CHAT STT DICTATION MODE ---

    /**
     * Dictates directly into chat input field without opening full live screen.
     */
    fun startDictation() {
        if (_isLiveModeOpen.value) return
        ttsManager.cancelUtterance()
        _isDictatingText.value = true
        _dictatedText.value = ""

        speechManager.startDictation(
            onPartial = { partial ->
                _dictatedText.value = partial
            },
            onFinal = { result ->
                _dictatedText.value = result
                _isDictatingText.value = false
            }
        )
    }

    fun stopDictation() {
        _isDictatingText.value = false
        speechManager.stopListening()
    }

    fun toggleDictation() {
        if (_isDictatingText.value) {
            stopDictation()
        } else {
            startDictation()
        }
    }

    fun clearDictatedText() {
        _dictatedText.value = ""
    }

    // --- MESSAGING & ADAPTIVE STYLE UPDATES ---

    fun sendMessage(text: String, isVoice: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        val sId = _currentSessionId.value
        viewModelScope.launch {
            _isGenerating.value = true
            _voiceOrbState.value = VoiceOrbState.THINKING
            _errorMessage.value = null

            // 1. Insert user message
            val userMsg = ChatMessageEntity(
                sessionId = sId,
                role = "user",
                content = trimmed,
                isVoice = isVoice
            )
            chatRepository.insertMessage(userMsg)
            chatRepository.updateSessionTimestamp(sId, System.currentTimeMillis())

            // Update session title on first message
            val currentSession = chatRepository.getSession(sId)
            if (currentSession != null && (currentSession.title.startsWith("Rozmowa") || currentSession.title == "Nowa rozmowa")) {
                val cleanTitle = trimmed.take(28).let { if (trimmed.length > 28) "$it..." else it }
                chatRepository.updateSessionTitle(sId, cleanTitle)
            }

            // 2. Prepare past context: STRICTLY bounded to the current session
            val historyEntities = chatRepository.getRecentMessages(sId, limit = 14).reversed()
            val historyPairs = historyEntities.map { it.role to it.content }

            // 3. Request Gemini
            val result = geminiRepo.generateResponse(historyPairs, _personaSettings.value)

            result.onSuccess { responseText ->
                val aiMsg = ChatMessageEntity(
                    sessionId = sId,
                    role = "model",
                    content = responseText,
                    isVoice = isVoice
                )
                val newId = chatRepository.insertMessage(aiMsg)
                chatRepository.updateSessionTimestamp(sId, System.currentTimeMillis())

                _isGenerating.value = false

                // 4. Live update of adaptive style profile in background
                analyzeAndUpdateAdaptiveStyle(trimmed, responseText)

                // If in live mode or voice message, play via TTS
                if (_isLiveModeOpen.value || isVoice) {
                    _liveSubtitle.value = responseText
                    _speakingMessageId.value = newId
                    _voiceOrbState.value = VoiceOrbState.SPEAKING
                    ttsManager.speak(responseText)
                } else {
                    _voiceOrbState.value = VoiceOrbState.IDLE
                }
            }.onFailure { err ->
                _isGenerating.value = false
                _voiceOrbState.value = VoiceOrbState.IDLE
                val errText = err.message ?: "Wystąpił nieoczekiwany błąd podczas komunikacji z Gemini."
                _errorMessage.value = errText
                _liveSubtitle.value = "Błąd: $errText"
            }
        }
    }

    /**
     * Automatically refines and updates the local adaptive communication style profile
     * based on conversation patterns, stored in Room for use across future chats.
     * Records synthesized semantic insights understood by the assistant, avoiding raw literal fragments.
     */
    private fun analyzeAndUpdateAdaptiveStyle(userMsg: String, modelResponse: String) {
        viewModelScope.launch {
            try {
                val userLower = userMsg.lowercase()
                val isCasual = userLower.contains("siema") || userLower.contains("elo") || userLower.contains("hej") || userLower.contains("spoko") || userLower.contains("fajnie")
                val isDirect = userMsg.length < 50 && !userMsg.contains("?")
                val hasHumor = userLower.contains("haha") || userLower.contains("xd") || userLower.contains("żart") || userLower.contains("lol")
                val isTechnical = userLower.contains("kod") || userLower.contains("funkcj") || userLower.contains("android") || userLower.contains("kotlin") || userLower.contains("api")

                // Formulate clear, well-articulated semantic insights understood from context (no weird raw quotes or slang noise)
                val understoodTraits = mutableListOf<String>()
                if (isDirect) understoodTraits.add("Preferencja zwięzłych, bezpośrednich wypowiedzi bez zbędnych wstępów")
                if (isCasual) understoodTraits.add("Docenienie naturalnego, swobodnego tonu konwersacji")
                if (hasHumor) understoodTraits.add("Otwartość na inteligentny humor, cięte puenty i luźniejszy klimat")
                if (isTechnical) understoodTraits.add("Nacisk na merytoryczne podejście i logiczne wnioski")

                if (understoodTraits.isNotEmpty()) {
                    val activeFacts = knowledgeDao.getActiveFacts()
                    for (trait in understoodTraits) {
                        val norm = trait.lowercase()
                        val alreadyExists = activeFacts.any { existing ->
                            val eNorm = existing.fact.lowercase()
                            eNorm.contains(norm) || norm.contains(eNorm)
                        }
                        if (!alreadyExists) {
                            knowledgeDao.insertFact(
                                LearnedFact(
                                    fact = trait,
                                    category = FactCategory.STYLE,
                                    isActive = true
                                )
                            )
                            break // Save one focused understood insight at a time to keep memory bank clean
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleSpeakMessage(message: ChatMessageEntity) {
        if (_speakingMessageId.value == message.id && ttsManager.isSpeaking.value) {
            ttsManager.cancelUtterance()
            _speakingMessageId.value = null
        } else {
            ttsManager.cancelUtterance()
            _speakingMessageId.value = message.id
            ttsManager.speak(message.content)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            ttsManager.cancelUtterance()
            chatRepository.clearMessagesForSession(_currentSessionId.value)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            if (_speakingMessageId.value == messageId) {
                ttsManager.cancelUtterance()
                _speakingMessageId.value = null
            }
            chatRepository.deleteMessageById(messageId)
        }
    }

    fun regenerateLastResponse() {
        if (_isGenerating.value) return
        val currentMsgs = messages.value
        val lastUserMsg = currentMsgs.lastOrNull { it.role == "user" } ?: return
        viewModelScope.launch {
            ttsManager.cancelUtterance()
            _speakingMessageId.value = null
            val lastMsg = currentMsgs.lastOrNull()
            if (lastMsg?.role == "model") {
                chatRepository.deleteMessageById(lastMsg.id)
            }

            val sId = _currentSessionId.value
            _isGenerating.value = true
            _voiceOrbState.value = VoiceOrbState.THINKING
            _errorMessage.value = null

            val historyEntities = chatRepository.getRecentMessages(sId, limit = 14).reversed()
            val historyPairs = historyEntities.map { it.role to it.content }

            val result = geminiRepo.generateResponse(historyPairs, _personaSettings.value)
            result.onSuccess { responseText ->
                val aiMsg = ChatMessageEntity(
                    sessionId = sId,
                    role = "model",
                    content = responseText,
                    isVoice = false
                )
                chatRepository.insertMessage(aiMsg)
                chatRepository.updateSessionTimestamp(sId, System.currentTimeMillis())
                _isGenerating.value = false
                _voiceOrbState.value = VoiceOrbState.IDLE
            }.onFailure { err ->
                _isGenerating.value = false
                _voiceOrbState.value = VoiceOrbState.IDLE
                val errText = err.message ?: "Wystąpił błąd podczas regenerowania odpowiedzi."
                _errorMessage.value = errText
            }
        }
    }

    // --- SESSION MANAGEMENT (NEW CHAT IS AN INDEPENDENT CONVERSATION) ---

    fun startNewSession() {
        viewModelScope.launch {
            ttsManager.cancelUtterance()
            val newId = UUID.randomUUID().toString()
            chatRepository.insertSession(
                ChatSessionEntity(
                    sessionId = newId,
                    title = "Rozmowa ${System.currentTimeMillis() % 10000}"
                )
            )
            _currentSessionId.value = newId
        }
    }

    fun switchSession(sessionId: String) {
        ttsManager.cancelUtterance()
        _currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            ttsManager.cancelUtterance()
            chatRepository.deleteSessionWithMessages(sessionId)
            val remaining = chatRepository.allSessions.first()
            if (_currentSessionId.value == sessionId) {
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().sessionId
                } else {
                    startNewSession()
                }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            chatRepository.updateSessionTitle(sessionId, newTitle.trim())
        }
    }

    // --- KNOWLEDGE BASE MANAGEMENT ---

    fun addKnowledgeSource(title: String, content: String, type: SourceType) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            val normTitle = title.trim().lowercase()
            val normContent = content.trim().lowercase()
            val activeSources = knowledgeDao.getActiveSources()
            val alreadyExists = activeSources.any {
                it.title.trim().lowercase() == normTitle || it.content.trim().lowercase() == normContent
            }
            if (!alreadyExists) {
                knowledgeSourceRepository.insertSource(
                    KnowledgeSource(
                        title = title.trim(),
                        content = content.trim(),
                        sourceType = type,
                        isEnabled = true
                    )
                )
            }
        }
    }

    fun updateKnowledgeSource(id: Long, title: String, content: String, type: SourceType) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            knowledgeSourceRepository.updateSourceContent(
                id = id,
                title = title.trim(),
                content = content.trim(),
                sourceType = type
            )
        }
    }

    fun toggleKnowledgeSource(source: KnowledgeSource, enabled: Boolean) {
        viewModelScope.launch {
            knowledgeSourceRepository.setSourceEnabled(source.id, enabled)
        }
    }

    fun deleteKnowledgeSource(source: KnowledgeSource) {
        viewModelScope.launch {
            knowledgeSourceRepository.deleteSource(source)
        }
    }

    // --- LEARNED FACTS & ADAPTIVE STYLE MANAGEMENT ---

    fun addLearnedFact(fact: String, category: FactCategory) {
        if (fact.isBlank()) return
        viewModelScope.launch {
            val normFact = fact.trim().lowercase()
            val activeFacts = knowledgeDao.getActiveFacts()
            val alreadyExists = activeFacts.any {
                val existing = it.fact.trim().lowercase()
                existing == normFact || existing.contains(normFact) || normFact.contains(existing)
            }
            if (!alreadyExists) {
                knowledgeDao.insertFact(
                    LearnedFact(
                        fact = fact.trim(),
                        category = category,
                        isActive = true
                    )
                )
            }
        }
    }

    fun toggleLearnedFact(fact: LearnedFact, active: Boolean) {
        viewModelScope.launch {
            knowledgeDao.setFactActive(fact.id, active)
        }
    }

    fun deleteLearnedFact(fact: LearnedFact) {
        viewModelScope.launch {
            knowledgeDao.deleteFact(fact)
        }
    }

    // --- VOICE PROFILES, TIMBRE, RATE & PITCH CONTROLS ---

    val voiceProfiles: StateFlow<List<VoiceProfile>> = ttsManager.availableProfiles
    val confirmedVoiceProfile: StateFlow<VoiceProfile> = ttsManager.confirmedProfile
    val previewingProfileId: StateFlow<String?> = ttsManager.previewingProfileId

    val speechRate: StateFlow<Float> = ttsManager.speechRate
    val pitch: StateFlow<Float> = ttsManager.pitch
    val selectedTimbre: StateFlow<TimbreStyle> = ttsManager.selectedTimbre

    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }

    fun setTimbreStyle(timbre: TimbreStyle) {
        ttsManager.setTimbreStyle(timbre)
    }

    fun playVoicePreview(profile: VoiceProfile? = null) {
        if (profile != null) {
            ttsManager.playPreviewForProfile(profile)
        } else {
            ttsManager.playPreview()
        }
    }

    fun confirmVoiceProfile(profile: VoiceProfile) {
        ttsManager.confirmVoiceProfile(profile)
    }

    fun applyVoiceProfile(profile: VoiceProfile) {
        ttsManager.applyVoiceProfile(profile)
    }

    fun selectVoiceByName(name: String) {
        ttsManager.setVoiceByName(name)
    }

    // --- PERSONA SETTINGS UPDATES ---

    fun updatePersonaSettings(
        humor: Float? = null,
        boldness: Float? = null,
        temperature: Float? = null,
        model: String? = null,
        autoLearn: Boolean? = null
    ) {
        val curr = _personaSettings.value
        _personaSettings.value = curr.copy(
            humorLevel = humor ?: curr.humorLevel,
            boldnessLevel = boldness ?: curr.boldnessLevel,
            temperature = temperature ?: curr.temperature,
            selectedModel = model ?: curr.selectedModel,
            autoLearn = autoLearn ?: curr.autoLearn
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun runApiDiagnostics() {
        viewModelScope.launch {
            _isTestingApi.value = true
            try {
                val result = geminiRepo.testConnection(_personaSettings.value.selectedModel)
                _apiDiagnosticResult.value = result
            } catch (e: Exception) {
                _apiDiagnosticResult.value = ApiDiagnosticResult(
                    isSuccess = false,
                    status = ApiDiagnosticStatus.NETWORK_ERROR,
                    httpCode = null,
                    modelTested = _personaSettings.value.selectedModel,
                    latencyMs = 0L,
                    headline = "Błąd wykonania diagnostyki",
                    details = e.localizedMessage ?: "Wystąpił nieoczekiwany błąd"
                )
            } finally {
                _isTestingApi.value = false
            }
        }
    }

    fun clearApiDiagnostics() {
        _apiDiagnosticResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
