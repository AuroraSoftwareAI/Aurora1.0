package com.example.data.remote

import com.squareup.moshi.Moshi
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class GeminiResult<out T> {
    data class Success<out T>(val data: T) : GeminiResult<T>()
    data class Error(val code: Int?, val userMessage: String, val technicalDetails: String? = null) : GeminiResult<Nothing>()
}

object GeminiErrorHandler {
    private val moshi = Moshi.Builder().build()
    private val errorAdapter = moshi.adapter(GeminiResponse::class.java)

    fun <T> handleResponse(
        response: Response<GeminiResponse>,
        modelName: String,
        transform: (GeminiResponse) -> T?
    ): GeminiResult<T> {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                val data = transform(body)
                if (data != null) {
                    return GeminiResult.Success(data)
                }
            }
            return GeminiResult.Error(
                code = response.code(),
                userMessage = "Model Gemini zwrócił pustą odpowiedź.",
                technicalDetails = "Puste ciało odpowiedzi przy kodzie 200"
            )
        }

        val httpCode = response.code()
        var serverMessage: String? = null

        try {
            val errorRaw = response.errorBody()?.string()
            if (!errorRaw.isNullOrBlank()) {
                val parsed = errorAdapter.fromJson(errorRaw)
                serverMessage = parsed?.error?.message
            }
        } catch (_: Exception) {
            // Error body could not be parsed as json
        }

        val userMessage = when (httpCode) {
            400 -> "Nieprawidłowe parametry zapytania (HTTP 400). ${serverMessage ?: "Sprawdź format wiadomości."}"
            401, 403 -> "Błąd autoryzacji (HTTP $httpCode): Twój klucz GEMINI_API_KEY jest nieprawidłowy lub nieaktywny. Sprawdź klucz w panelu Secrets."
            404 -> "Model '$modelName' nie został odnaleziony lub nie jest dostępny (HTTP 404). Zmień model w ustawieniach."
            429 -> "Przekroczono limit zapytań Gemini API (HTTP 429 Quota Exceeded). Odczekaj chwilę przed wysłaniem kolejnej wiadomości."
            500, 502, 503, 504 -> "Serwery Google Gemini są chwilowo niedostępne lub przeciążone (HTTP $httpCode). Spróbuj ponownie za moment."
            else -> "Błąd połączenia z Gemini API (HTTP $httpCode): ${serverMessage ?: "Nieoczekiwana odpowiedź serwera."}"
        }

        return GeminiResult.Error(
            code = httpCode,
            userMessage = userMessage,
            technicalDetails = serverMessage
        )
    }

    fun handleException(throwable: Throwable): GeminiResult.Error {
        val userMessage = when (throwable) {
            is UnknownHostException -> "Brak połączenia z internetem. Sprawdź swoje połączenie sieciowe."
            is SocketTimeoutException -> "Przekroczono czas oczekiwania na odpowiedź serwera Gemini (Timeout). Spróbuj ponownie."
            is IOException -> "Błąd transmisji danych: ${throwable.localizedMessage ?: "Problem z siecią"}"
            else -> "Nieoczekiwany błąd: ${throwable.localizedMessage ?: "Błąd aplikacji"}"
        }

        return GeminiResult.Error(
            code = null,
            userMessage = userMessage,
            technicalDetails = throwable.message
        )
    }
}
