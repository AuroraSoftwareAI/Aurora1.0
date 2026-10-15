package com.example

import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GeminiResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class GeminiApiRetrofitTest {

    @Test
    fun testGeminiRequestSerializationAndHelpers() {
        val request = GeminiRequest.withSystemInstruction(
            systemPrompt = "You are Aurora, an empathetic assistant.",
            userPrompt = "Hello there!"
        )

        assertEquals(1, request.contents.size)
        assertEquals("user", request.contents[0].role)
        assertEquals("Hello there!", request.contents[0].parts[0].text)
        assertNotNull(request.systemInstruction)
        assertEquals("You are Aurora, an empathetic assistant.", request.systemInstruction?.parts?.get(0)?.text)

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(GeminiRequest::class.java)
        val json = adapter.toJson(request)
        assertTrue(json.contains("Aurora, an empathetic assistant"))
        assertTrue(json.contains("Hello there!"))

        val parsedBack = adapter.fromJson(json)
        assertNotNull(parsedBack)
        assertEquals("Hello there!", parsedBack?.contents?.get(0)?.parts?.get(0)?.text)
    }

    @Test
    fun testGeminiResponseDeserialization() {
        val sampleResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Greetings from Aurora!"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP",
                  "index": 0
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 5,
                "candidatesTokenCount": 6,
                "totalTokenCount": 11
              },
              "modelVersion": "gemini-3.5-flash-lite"
            }
        """.trimIndent()

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(GeminiResponse::class.java)
        val response = adapter.fromJson(sampleResponseJson)

        assertNotNull(response)
        assertEquals("Greetings from Aurora!", response?.text())
        assertEquals(11, response?.usageMetadata?.totalTokenCount)
        assertEquals("gemini-3.5-flash-lite", response?.modelVersion)
    }

    @Test
    fun testRetrofitAuthenticationHeadersIncluded() = runBlocking {
        var capturedApiKeyHeader: String? = null
        var capturedContentTypeHeader: String? = null
        var capturedPath: String? = null

        val testApiKey = "test-api-key-12345"
        val mockInterceptor = Interceptor { chain ->
            val request = chain.request()
            capturedApiKeyHeader = request.header("x-goog-api-key")
            capturedContentTypeHeader = request.header("Content-Type") ?: request.body?.contentType()?.toString()
            capturedPath = request.url.encodedPath

            val mockJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{ "text": "Auth header test response" }],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
            """.trimIndent()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(testOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(GeminiApiService::class.java)

        val response = service.generateContent(
            model = "gemini-3.5-flash-lite",
            apiKey = testApiKey,
            request = GeminiRequest.fromPrompt("Test prompt")
        )

        // Verify HTTP Headers for Authentication
        assertEquals("test-api-key-12345", capturedApiKeyHeader)
        assertTrue(capturedContentTypeHeader?.contains("application/json") == true)
        assertEquals("/v1beta/models/gemini-3.5-flash-lite:generateContent", capturedPath)

        assertTrue(response.isSuccessful)
        assertEquals("Auth header test response", response.body()?.text())
    }

    @Test
    fun testCustomAuthHeadersMapHelper() {
        val headers = GeminiApiClient.createAuthHeaders("custom-key-xyz")
        assertEquals("custom-key-xyz", headers[GeminiApiClient.HEADER_API_KEY])
        assertEquals("application/json", headers["Content-Type"])
    }
}
