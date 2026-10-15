package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

/**
 * Retrofit service interface for communication with Google Gemini API endpoints.
 * Ensures the necessary HTTP headers (including 'x-goog-api-key' for authentication) are included.
 */
interface GeminiApiService {

    /**
     * Sends a request to generate content using a specific Gemini model.
     * Authenticates using the 'x-goog-api-key' HTTP header.
     *
     * @param model Model identifier (e.g. gemini-3.5-flash-lite, gemini-3.8-flash, gemini-3.1-pro-preview)
     * @param apiKey The Google Gemini API key sent via the 'x-goog-api-key' HTTP header
     * @param request The structured Gemini request containing contents and generationConfig
     * @return Retrofit Response containing GeminiResponse
     */
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>

    /**
     * Generates content with dynamic custom headers (e.g. custom authentication or tracking headers).
     *
     * @param model Model identifier
     * @param headers Map of HTTP request headers (must include 'x-goog-api-key' or auth header)
     * @param request The structured Gemini request payload
     * @return Retrofit Response containing GeminiResponse
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentWithHeaders(
        @Path("model") model: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>

    /**
     * Streaming content generation using Server-Sent Events (SSE).
     * Authenticates via the 'x-goog-api-key' HTTP header.
     *
     * @param model Model identifier
     * @param apiKey The Google Gemini API key sent via the 'x-goog-api-key' HTTP header
     * @param request The structured Gemini request payload
     * @return Retrofit Response containing ResponseBody stream
     */
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/{model}:streamGenerateContent?alt=sse")
    @Streaming
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<ResponseBody>

    /**
     * Backward-compatible alternative endpoint passing the API key via query parameter (?key=...).
     *
     * @param model Model identifier
     * @param apiKey The Google Gemini API key
     * @param request The structured Gemini request payload
     * @return Retrofit Response containing GeminiResponse
     */
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentWithKeyParam(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

/**
 * Singleton client providing the configured Retrofit instance and GeminiApiService.
 */
object GeminiApiClient {
    const val BASE_URL = "https://generativelanguage.googleapis.com/"
    const val HEADER_API_KEY = "x-goog-api-key"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                if (original.header("Content-Type") == null) {
                    builder.header("Content-Type", "application/json")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Creates custom headers map with standard authentication and content headers.
     */
    fun createAuthHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            HEADER_API_KEY to apiKey,
            "Content-Type" to "application/json"
        )
    }
}
