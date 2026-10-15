package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorRecovery: (() -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Interruption callback: fired immediately in fraction of second when user voice is detected
    var onInterruptionDetected: (() -> Unit)? = null

    // Dedicated callbacks for chat-level STT dictation
    var onDictationPartial: ((String) -> Unit)? = null
    var onDictationResult: ((String) -> Unit)? = null
    var isDictationActive: Boolean = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(locale: Locale = Locale.forLanguageTag("pl-PL")) {
        isDictationActive = false
        mainHandler.post {
            internalStartListening(locale)
        }
    }

    fun startDictation(
        locale: Locale = Locale.forLanguageTag("pl-PL"),
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit
    ) {
        isDictationActive = true
        onDictationPartial = onPartial
        onDictationResult = onFinal
        mainHandler.post {
            internalStartListening(locale)
        }
    }

    private fun internalStartListening(locale: Locale) {
        internalStopListening()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Rozpoznawanie mowy nie jest dostępne na tym urządzeniu."
            return
        }

        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            _errorMessage.value = "Brak uprawnień do mikrofonu. Przyznaj uprawnienie, aby korzystać z mowy."
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialText.value = ""
                        _errorMessage.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                        // Instant interruption trigger: user started speaking!
                        if (!isDictationActive) {
                            onInterruptionDetected?.invoke()
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                        _rmsLevel.value = normalized

                        // Early audio threshold trigger for instantaneous cancellation
                        if (!isDictationActive && normalized > 0.40f) {
                            onInterruptionDetected?.invoke()
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Nie rozpoznano mowy."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Przekroczono limit czasu oczekiwania na głos."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Błąd sieci podczas rozpoznawania mowy."
                            SpeechRecognizer.ERROR_AUDIO -> "Błąd nagrywania dźwięku."
                            SpeechRecognizer.ERROR_CLIENT -> "Operacja anulowana."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Brak uprawnień mikrofonu."
                            else -> "Błąd rozpoznawania mowy (kod: $error)"
                        }
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && 
                            error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && 
                            error != SpeechRecognizer.ERROR_CLIENT) {
                            _errorMessage.value = msg
                        }
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            onErrorRecovery?.invoke()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()

                        if (text.isNotBlank()) {
                            _partialText.value = text
                            if (isDictationActive) {
                                onDictationResult?.invoke(text)
                            } else {
                                onResult(text)
                            }
                        } else {
                            onErrorRecovery?.invoke()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _partialText.value = text
                            if (isDictationActive) {
                                onDictationPartial?.invoke(text)
                            } else {
                                // Instant interruption trigger upon partial words detected
                                onInterruptionDetected?.invoke()
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _errorMessage.value = "Nie udało się uruchomić mikrofonu: ${e.message}"
            Log.e("SpeechRecognizer", "startListening error", e)
        }
    }

    fun stopListening() {
        mainHandler.post {
            internalStopListening()
        }
    }

    private fun internalStopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("SpeechRecognizer", "destroy error", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }
}
