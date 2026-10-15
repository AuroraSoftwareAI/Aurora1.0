package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val voiceName: String? = null,
    val isSystemVoice: Boolean = false,
    val qualityLabel: String = "Wysoka jakość • pl-PL"
)

enum class TimbreStyle(
    val id: String,
    val displayName: String,
    val description: String,
    val pitchMultiplier: Float,
    val rateMultiplier: Float
) {
    NATURAL("natural", "Naturalna & Czysta", "Autentyczny, zrównoważony tembr bez dodatkowej koloryzacji", 1.0f, 1.0f),
    WARM("warm", "Ciepła & Aksamitna", "Zaokrąglona, kojąca barwa o łagodnym i miękkim tonie", 0.93f, 0.97f),
    BRIGHT("bright", "Krystaliczna & Wyrazista", "Wyostrzona artykulacja, podbita dynamika i przejrzystość", 1.07f, 1.03f),
    DEEP("deep", "Głęboka & Spokojna", "Niski, opanowany rezonans o stonowanym charakterze", 0.86f, 0.94f),
    DYNAMIC("dynamic", "Dynamiczna & Radiowa", "Energiczny, radiowy ton z wyrazistym akcentowaniem zdań", 1.04f, 1.08f)
}

class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>("Domyślny głos (pl-PL)")
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    // Speech Rate (Tempo) & Pitch (Wysokość) Controls
    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    // Timbre / Barwa Głosu Options
    private val _selectedTimbre = MutableStateFlow(TimbreStyle.NATURAL)
    val selectedTimbre: StateFlow<TimbreStyle> = _selectedTimbre.asStateFlow()

    companion object {
        val DEFAULT_POLISH_PROFILES = listOf(
            VoiceProfile(
                id = "aurora_natural_pl",
                name = "Aurora Naturalna",
                description = "Zrównoważony, ciepły i autentyczny tembr mowy w języku polskim",
                voiceName = null,
                qualityLabel = "Domyślny syntezator • pl-PL"
            ),
            VoiceProfile(
                id = "aurora_warm_pl",
                name = "Aurora Spokojna i Łagodna",
                description = "Głębokie, aksamitne i kojące brzmienie języka polskiego",
                voiceName = null,
                qualityLabel = "Aksamitny tembr • pl-PL"
            ),
            VoiceProfile(
                id = "aurora_modern_pl",
                name = "Aurora Nowoczesna i Wyrazista",
                description = "Dynamiczna, energiczna barwa z czystą i bezpośrednią dykcją",
                voiceName = null,
                qualityLabel = "Wysoka wyrazistość • pl-PL"
            ),
            VoiceProfile(
                id = "aurora_radio_pl",
                name = "Aurora Radiowa",
                description = "Precyzyjna modulacja i wyraźne akcentowanie kluczowych myśli",
                voiceName = null,
                qualityLabel = "Format lektorski • pl-PL"
            ),
            VoiceProfile(
                id = "aurora_studio_pl",
                name = "Aurora Studyjna HD",
                description = "Krystaliczny ton o studyjnej czystości brzmienia głosu",
                voiceName = null,
                qualityLabel = "Studyjne HD • pl-PL"
            )
        )
    }

    private val _availableProfiles = MutableStateFlow<List<VoiceProfile>>(DEFAULT_POLISH_PROFILES)
    val availableProfiles: StateFlow<List<VoiceProfile>> = _availableProfiles.asStateFlow()

    private val _confirmedProfile = MutableStateFlow(DEFAULT_POLISH_PROFILES[0])
    val confirmedProfile: StateFlow<VoiceProfile> = _confirmedProfile.asStateFlow()
    val selectedProfile: StateFlow<VoiceProfile> = _confirmedProfile.asStateFlow()

    private val _previewingProfileId = MutableStateFlow<String?>(null)
    val previewingProfileId: StateFlow<String?> = _previewingProfileId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val plLocale = Locale.forLanguageTag("pl-PL")
                val langResult = tts?.setLanguage(plLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallbackPl = Locale("pl", "PL")
                    tts?.setLanguage(fallbackPl)
                }

                applyTuningToEngine()
                loadAvailableVoices()

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _previewingProfileId.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _previewingProfileId.value = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        _previewingProfileId.value = null
                        Log.e("TextToSpeechManager", "TTS error code: $errorCode")
                    }
                })
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate.coerceIn(0.5f, 2.0f)
        applyTuningToEngine()
    }

    fun setPitch(pitch: Float) {
        _pitch.value = pitch.coerceIn(0.5f, 2.0f)
        applyTuningToEngine()
    }

    fun setTimbreStyle(timbre: TimbreStyle) {
        _selectedTimbre.value = timbre
        applyTuningToEngine()
    }

    private fun applyTuningToEngine() {
        val effectiveRate = (_speechRate.value * _selectedTimbre.value.rateMultiplier).coerceIn(0.5f, 2.0f)
        val effectivePitch = (_pitch.value * _selectedTimbre.value.pitchMultiplier).coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(effectiveRate)
        tts?.setPitch(effectivePitch)
    }

    /**
     * Strictly filters and loads ONLY Polish voices (pl, pl-PL, pol).
     * Non-Polish voices are completely excluded.
     */
    private fun loadAvailableVoices() {
        try {
            val allVoices = tts?.voices?.toList().orEmpty()
            val plVoices = allVoices.filter { voice ->
                val lang = voice.locale.language.lowercase(Locale.ROOT)
                val country = voice.locale.country.lowercase(Locale.ROOT)
                val name = voice.name.lowercase(Locale.ROOT)
                val iso3 = try { voice.locale.isO3Language.lowercase(Locale.ROOT) } catch (e: Exception) { "" }

                lang == "pl" || iso3 == "pol" || country == "pl" || name.contains("pl-pl") || name.contains("pol")
            }.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.name }
            )

            _availableVoices.value = plVoices

            val dynamicProfiles = mutableListOf<VoiceProfile>()
            if (plVoices.isNotEmpty()) {
                plVoices.forEachIndexed { index, voice ->
                    val isNetwork = voice.name.contains("network", ignoreCase = true)
                    val friendlyName = when {
                        voice.name.contains("oda", ignoreCase = true) -> "Polski Głos Studyjny (Oda)"
                        voice.name.contains("afs", ignoreCase = true) -> "Polski Głos Ciepły (Afs)"
                        voice.name.contains("bda", ignoreCase = true) -> "Polski Głos Głęboki (Bda)"
                        isNetwork -> "Polski Głos Sieciowy #${index + 1}"
                        else -> "Polski Głos Asystenta #${index + 1}"
                    }
                    val desc = when {
                        isNetwork -> "Naturalna artykulacja sieciowa o wysokiej wierności"
                        voice.quality >= 400 -> "Bogaty tembr mowy z precyzyjną dykcją"
                        else -> "Płynny i bezpośredni syntezator mowy"
                    }
                    val quality = if (isNetwork || voice.quality >= 400) "Wysoka jakość / HD (pl-PL)" else "Standard (pl-PL)"

                    dynamicProfiles.add(
                        VoiceProfile(
                            id = "sys_pl_${voice.name}",
                            name = friendlyName,
                            description = desc,
                            voiceName = voice.name,
                            isSystemVoice = true,
                            qualityLabel = quality
                        )
                    )
                }
            } else {
                dynamicProfiles.addAll(DEFAULT_POLISH_PROFILES)
            }

            _availableProfiles.value = dynamicProfiles

            // Set initial confirmed profile
            val current = dynamicProfiles.firstOrNull() ?: DEFAULT_POLISH_PROFILES[0]
            _confirmedProfile.value = current
            applyProfileToEngine(current)
            applyTuningToEngine()
        } catch (e: Exception) {
            Log.w("TextToSpeechManager", "Failed to query system voices", e)
            _availableProfiles.value = DEFAULT_POLISH_PROFILES
        }
    }

    /**
     * Plays an audio preview of the specified voice profile.
     * When user taps on any voice in the voice selection view, this plays automatically.
     */
    fun playPreviewForProfile(
        profile: VoiceProfile,
        sampleText: String = "Cześć! Tak brzmi ten głos w Aurora AI. Jak mogę Ci dzisiaj pomóc?"
    ) {
        if (!isInitialized || tts == null) return

        cancelUtterance()
        applyProfileToEngine(profile)
        applyTuningToEngine()
        _previewingProfileId.value = profile.id
        _isSpeaking.value = true

        val utteranceId = "preview_${profile.id}_${UUID.randomUUID()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Confirms the selected voice profile as the assistant's active voice for the chat.
     */
    fun confirmVoiceProfile(profile: VoiceProfile) {
        cancelUtterance()
        _confirmedProfile.value = profile
        applyProfileToEngine(profile)
        applyTuningToEngine()
    }

    fun applyVoiceProfile(profile: VoiceProfile) {
        confirmVoiceProfile(profile)
    }

    fun setVoiceByName(voiceName: String) {
        val targetProfile = _availableProfiles.value.firstOrNull { it.voiceName == voiceName }
        if (targetProfile != null) {
            confirmVoiceProfile(targetProfile)
        } else {
            val targetVoice = _availableVoices.value.firstOrNull { it.name == voiceName }
            if (targetVoice != null) {
                tts?.voice = targetVoice
                _selectedVoiceName.value = targetVoice.name
                applyTuningToEngine()
            }
        }
    }

    private fun applyProfileToEngine(profile: VoiceProfile) {
        if (!profile.voiceName.isNullOrBlank()) {
            val targetVoice = _availableVoices.value.firstOrNull { it.name == profile.voiceName }
                ?: tts?.voices?.firstOrNull { it.name == profile.voiceName }
            if (targetVoice != null) {
                tts?.voice = targetVoice
                _selectedVoiceName.value = targetVoice.name
                return
            }
        }
        val plLocale = Locale.forLanguageTag("pl-PL")
        tts?.setLanguage(plLocale)
        _selectedVoiceName.value = profile.name
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) return

        // Clean out excessive asterisks or markdown artifacts for fluid speech
        val cleanedText = text
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()

        if (cleanedText.isBlank()) return

        cancelUtterance()
        // Ensure confirmed profile and acoustic tuning are applied
        applyProfileToEngine(_confirmedProfile.value)
        applyTuningToEngine()

        _isSpeaking.value = true
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Instantly cancels playback and frees the audio stream in a fraction of a second.
     */
    fun cancelUtterance() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w("TextToSpeechManager", "Error stopping TTS", e)
        } finally {
            _isSpeaking.value = false
            _previewingProfileId.value = null
        }
    }

    fun stop() {
        cancelUtterance()
    }

    fun playPreview(sampleText: String = "Cześć! Tak brzmi mój naturalny głos w Aurora AI. Jak mogę Ci dzisiaj pomóc?") {
        playPreviewForProfile(_confirmedProfile.value, sampleText)
    }

    fun shutdown() {
        cancelUtterance()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
