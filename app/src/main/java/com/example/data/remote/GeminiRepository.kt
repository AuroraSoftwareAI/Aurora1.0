package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.KnowledgeDao
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ApiDiagnosticStatus {
    SUCCESS,
    UNAUTHORIZED,
    NOT_FOUND,
    QUOTA_EXCEEDED,
    MISSING_KEY,
    NETWORK_ERROR,
    SERVER_ERROR
}

data class ApiDiagnosticResult(
    val isSuccess: Boolean,
    val status: ApiDiagnosticStatus,
    val httpCode: Int?,
    val modelTested: String,
    val latencyMs: Long,
    val headline: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PersonaSettings(
    val humorLevel: Float = 0.8f, // 0.0 to 1.0
    val boldnessLevel: Float = 0.9f, // 0.0 to 1.0
    val temperature: Float = 0.85f,
    val selectedModel: String = "gemini-3.5-flash-lite",
    val autoLearn: Boolean = true,
    val speechFriendlyOutput: Boolean = true
)

class GeminiRepository(
    private val knowledgeDao: KnowledgeDao
) {
    companion object {
        fun resolveApiKey(): String {
            val k1 = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()
            if (!k1.isNullOrBlank() && k1 != "MY_GEMINI_API_KEY" && !k1.startsWith("MY_")) {
                return k1.trim()
            }
            val k2 = runCatching { BuildConfig.API_KEY }.getOrNull()
            if (!k2.isNullOrBlank() && k2 != "MY_API_KEY" && !k2.startsWith("MY_")) {
                return k2.trim()
            }
            return ""
        }
    }

    suspend fun generateResponse(
        messages: List<Pair<String, String>>, // role to text for current session
        settings: PersonaSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        val hasValidKey = apiKey.isNotBlank()

        try {
            val activeSources = knowledgeDao.getActiveSources()
            // Strictly exclude any legacy support memory
            val activeFacts = knowledgeDao.getFactsExcludingCategory(FactCategory.SUPPORT_MEMORY)

            if (!hasValidKey) {
                // If API key is not configured, provide helpful grounded response based on active sources
                val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
                val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, activeSources, activeFacts, settings)
                return@withContext Result.success(fallbackReply)
            }

            val systemInstructionText = buildSystemInstruction(activeSources, activeFacts, settings)

            val contents = messages.map { (role, text) ->
                GeminiContent(
                    role = if (role == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = text))
                )
            }

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstructionText))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = settings.temperature,
                    topP = 0.95f,
                    topK = 40,
                    maxOutputTokens = 2048
                )
            )

            val candidateModels = listOf(
                settings.selectedModel,
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite-preview",
                "gemini-3.8-flash",
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview"
            ).distinct()

            var successfulText: String? = null
            var lastErrorMsg: String? = null

            for (modelToTry in candidateModels) {
                try {
                    val response = GeminiApiClient.service.generateContent(
                        model = modelToTry,
                        apiKey = apiKey,
                        request = request
                    )

                    val parsed = GeminiErrorHandler.handleResponse(response, modelToTry) { resp ->
                        resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    }

                    if (parsed is GeminiResult.Success) {
                        successfulText = parsed.data
                        break
                    } else if (parsed is GeminiResult.Error) {
                        lastErrorMsg = parsed.userMessage
                    }
                } catch (t: Throwable) {
                    val handled = GeminiErrorHandler.handleException(t)
                    lastErrorMsg = handled.userMessage
                }
            }

            if (successfulText != null) {
                if (settings.autoLearn) {
                    val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second
                    if (lastUserMsg != null && lastUserMsg.length > 8) {
                        tryAutoExtractFact(lastUserMsg)
                    }
                }
                Result.success(successfulText)
            } else {
                val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
                val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, activeSources, activeFacts, settings)
                Result.success(fallbackReply)
            }
        } catch (e: Throwable) {
            val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second.orEmpty()
            val fallbackReply = generateOfflineGroundedResponse(lastUserMsg, emptyList(), emptyList(), settings)
            Result.success(fallbackReply)
        }
    }

    private fun buildSystemInstruction(
        sources: List<KnowledgeSource>,
        facts: List<LearnedFact>,
        settings: PersonaSettings
    ): String {
        return buildString {
            append("Nazywasz się Aurora AI (lub po prostu Aurora). ")
            append("Jesteś jednym spójnym, trwałym asystentem osobistym AI.\n")
            append("GŁÓWNA ZASADA ARCHITEKTURY: TO UŻYTKOWNIK KONTROLUJE KONTEKST.\n\n")

            append("=== CHARAKTER, STYL I ZASADY KOMUNIKACJI ===\n")
            append("- Cel: Żywa, naturalna rozmowa, pomoc merytoryczna, codzienne tematy, hobby, programowanie, technologia i refleksje.\n")
            append("- Styl konwersacji: Dopasowuj się dynamicznie do klimatu i stylu wypowiedzi użytkownika. ")
            when {
                settings.humorLevel > 0.7f -> append("Bądź dowcipny, bystry, używaj inteligentnych ripost i ciętego humoru. ")
                settings.humorLevel > 0.3f -> append("Bądź przyjazny, ciepły, z naturalnym poczuciem humoru. ")
                else -> append("Bądź wyważony, rzeczowy i precyzyjny. ")
            }
            if (settings.boldnessLevel > 0.6f) {
                append("Bądź bezpośredni, szczery, bez sztucznego owijania w bawełnę. ")
            }
            append("Jeśli użytkownik używa slangu, potocznych zwrotów czy luźnego języka, naturalnie przyjmij ten klimat.\n")

            if (settings.speechFriendlyOutput) {
                append("- Mowa i Live Voice: Odpowiedzi są odtwarzane przez syntezator mowy (TTS). Formułuj płynne, naturalnie brzmiące zdania. Unikaj zbędnych bloków kodu, markdownowych tabel i przesadnych wyliczeń w rozmowie głosowej, chyba że użytkownik wyraźnie o to zapyta.\n")
            }

            // Adaptive style facts vs learned user facts
            val styleFacts = facts.filter { it.category == FactCategory.STYLE || it.category == FactCategory.VOCABULARY || it.category == FactCategory.HUMOR }
            val generalFacts = facts.filter { it.category == FactCategory.PREFERENCE || it.category == FactCategory.GENERAL }

            if (styleFacts.isNotEmpty()) {
                append("\n--- PROFIL STYLU ADAPTACYJNEGO (ZAPAMIĘTANY W PAMIĘCI LOKALNEJ) ---\n")
                append("Model adaptacyjny nauczył się następujących cech stylu konwersacji:\n")
                styleFacts.forEach { f ->
                    append("• ${f.fact}\n")
                }
            }

            if (generalFacts.isNotEmpty()) {
                append("\n--- FAKTY O UŻYTKOWNIKU I PREFERENCJE (DO ZAPAMIĘTANIA) ---\n")
                generalFacts.forEach { f ->
                    append("• [${f.category.name}]: ${f.fact}\n")
                }
            }

            if (sources.isNotEmpty()) {
                append("\n--- BAZA WIEDZY I DOKUMENTY UŻYTKOWNIKA ---\n")
                sources.forEach { s ->
                    append("### ${s.title} (${s.sourceType.name})\n")
                    append("${s.content}\n\n")
                }
            }
        }
    }

    private suspend fun tryAutoExtractFact(userMessage: String) {
        val lower = userMessage.lowercase()
        val preferencePhrases = listOf(
            "lubię " to (FactCategory.PREFERENCE to "Użytkownik lubi: "),
            "wolę " to (FactCategory.PREFERENCE to "Preferencja: "),
            "mój ulubiony " to (FactCategory.PREFERENCE to "Ulubiony element użytkownika: "),
            "moja ulubiona " to (FactCategory.PREFERENCE to "Ulubiona rzecz użytkownika: "),
            "moje ulubione " to (FactCategory.PREFERENCE to "Ulubione rzeczy użytkownika: "),
            "nazywam się " to (FactCategory.GENERAL to "Imię użytkownika: "),
            "pracuję jako " to (FactCategory.GENERAL to "Zawód użytkownika: "),
            "zajmuję się " to (FactCategory.GENERAL to "Zajęcie / specjalizacja: "),
            "pamiętaj, że " to (FactCategory.STYLE to "Zasada dla asystenta: "),
            "zwracaj się do mnie " to (FactCategory.STYLE to "Forma zwracania się: "),
            "mów do mnie " to (FactCategory.STYLE to "Forma zwracania się: "),
            "nie znoszę " to (FactCategory.PREFERENCE to "Użytkownik nie toleruje: ")
        )

        for ((phrase, config) in preferencePhrases) {
            val idx = lower.indexOf(phrase)
            if (idx != -1) {
                val rawValue = userMessage.substring(idx + phrase.length)
                    .take(90)
                    .replace(Regex("[!?.\n].*"), "")
                    .replace(Regex("(xd|lol|haha|hehe)", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (rawValue.length in 2..80) {
                    val formattedFact = "${config.second}$rawValue"
                    val existing = knowledgeDao.getActiveFacts()
                    val isDuplicate = existing.any {
                        it.fact.contains(rawValue, ignoreCase = true) ||
                        formattedFact.contains(it.fact, ignoreCase = true)
                    }
                    if (!isDuplicate) {
                        knowledgeDao.insertFact(
                            LearnedFact(
                                fact = formattedFact,
                                category = config.first
                            )
                        )
                    }
                }
                break
            }
        }
    }

    private fun generateOfflineGroundedResponse(
        prompt: String,
        sources: List<KnowledgeSource>,
        facts: List<LearnedFact>,
        settings: PersonaSettings
    ): String {
        val lower = prompt.lowercase()
        val matchingSource = sources.firstOrNull { source ->
            source.title.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) } ||
            source.content.lowercase().split(" ").any { word -> word.length > 4 && lower.contains(word) }
        }

        return when {
            matchingSource != null -> {
                "Na podstawie Twojego źródła '${matchingSource.title}': ${matchingSource.content.take(280)}..."
            }
            lower.contains("źródł") || lower.contains("wiedz") || lower.contains("baza") -> {
                val list = sources.joinToString(", ") { it.title }
                "W Twojej lokalnej Bazie Wiedzy jest ${sources.size} aktywnych źródeł: $list. Mogę z nich swobodnie czerpać!"
            }
            lower.contains("kim jesteś") || lower.contains("co potrafisz") -> {
                "Jestem Aurora AI – Twoim osobistym kompanem! Rozmawiam na żywo, uczę się Twojego unikalnego stylu w trybie adaptacyjnym i natychmiast czerpię z Twojej Bazy Wiedzy."
            }
            lower.contains("dowcip") || lower.contains("żart") -> {
                "Dlaczego programiści wolą ciemny motyw? Bo światło przyciąga bugi! 🐛"
            }
            facts.isNotEmpty() && (lower.contains("mnie") || lower.contains("styl") || lower.contains("pamięt")) -> {
                val factsSummary = facts.take(3).joinToString("; ") { it.fact }
                "Pamiętam w pamięci adaptacyjnej: $factsSummary. Mój styl jest dopasowany do Ciebie!"
            }
            else -> {
                "Słyszę Cię doskonale! Tryb Live z natychmiastowym przerywaniem i lokalna baza wiedzy są gotowe. O czym chcesz teraz pogadać?"
            }
        }
    }

    suspend fun testConnection(model: String = "gemini-3.5-flash-lite"): ApiDiagnosticResult = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        val hasValidKey = apiKey.isNotBlank()

        if (!hasValidKey) {
            return@withContext ApiDiagnosticResult(
                isSuccess = false,
                status = ApiDiagnosticStatus.MISSING_KEY,
                httpCode = null,
                modelTested = model,
                latencyMs = 0L,
                headline = "Klucz API nie jest skonfigurowany",
                details = "Brak klucza GEMINI_API_KEY lub wartość zastępcza. Wprowadź klucz w panelu Secrets w AI Studio."
            )
        }

        val startTime = System.currentTimeMillis()
        val candidateModels = listOf(
            model,
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.8-flash",
            "gemini-3.5-flash"
        ).distinct()

        val testRequest = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "Aurora API test probe. Odpowiedz krótko 'OK'."))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                maxOutputTokens = 16,
                temperature = 0.2f
            )
        )

        var lastCode: Int? = null
        var lastError: String? = null

        for (targetModel in candidateModels) {
            try {
                val response = GeminiApiClient.service.generateContent(
                    model = targetModel,
                    apiKey = apiKey,
                    request = testRequest
                )
                val latency = System.currentTimeMillis() - startTime
                lastCode = response.code()

                if (response.isSuccessful && response.body()?.candidates?.firstOrNull() != null) {
                    return@withContext ApiDiagnosticResult(
                        isSuccess = true,
                        status = ApiDiagnosticStatus.SUCCESS,
                        httpCode = 200,
                        modelTested = targetModel,
                        latencyMs = latency,
                        headline = "Połączenie z Gemini API działa prawidłowo",
                        details = "Klucz API zweryfikowany pomyślnie. Czas odpowiedzi: ${latency} ms. Model: $targetModel."
                    )
                } else {
                    val errorRaw = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    lastError = errorRaw
                    if (response.code() == 401 || response.code() == 403) {
                        return@withContext ApiDiagnosticResult(
                            isSuccess = false,
                            status = ApiDiagnosticStatus.UNAUTHORIZED,
                            httpCode = response.code(),
                            modelTested = targetModel,
                            latencyMs = latency,
                            headline = "Błąd autoryzacji klucza API (HTTP ${response.code()})",
                            details = "Podany klucz GEMINI_API_KEY został odrzucony przez serwery Google. Upewnij się, że klucz jest poprawny w Secrets."
                        )
                    } else if (response.code() == 429) {
                        return@withContext ApiDiagnosticResult(
                            isSuccess = false,
                            status = ApiDiagnosticStatus.QUOTA_EXCEEDED,
                            httpCode = 429,
                            modelTested = targetModel,
                            latencyMs = latency,
                            headline = "Limit zapytań wyczerpany (HTTP 429)",
                            details = "Klucz jest poprawny, ale przekroczono limit zapytań (Quota Exceeded). Spróbuj ponownie za chwilę."
                        )
                    }
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                val handled = GeminiErrorHandler.handleException(e)
                return@withContext ApiDiagnosticResult(
                    isSuccess = false,
                    status = ApiDiagnosticStatus.NETWORK_ERROR,
                    httpCode = null,
                    modelTested = targetModel,
                    latencyMs = latency,
                    headline = "Błąd sieci lub timeout",
                    details = handled.userMessage
                )
            }
        }

        return@withContext ApiDiagnosticResult(
            isSuccess = false,
            status = ApiDiagnosticStatus.SERVER_ERROR,
            httpCode = lastCode,
            modelTested = model,
            latencyMs = System.currentTimeMillis() - startTime,
            headline = "Nie udało się nawiązać połączenia",
            details = lastError ?: "Serwer zwrócił błąd HTTP $lastCode"
        )
    }
}
