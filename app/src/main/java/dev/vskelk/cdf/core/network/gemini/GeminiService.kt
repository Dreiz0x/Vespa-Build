package dev.vskelk.cdf.core.network.gemini

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com"
    private val modelName = "gemini-1.5-flash"

    suspend fun generateWithVision(
        apiKey: String,
        prompt: String,
        pdfBytes: ByteArray?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key no configurada"))
            }

            val parts = mutableListOf<GeminiPart>()

            if (pdfBytes != null) {
                val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                parts.add(
                    GeminiPart(
                        inlineData = InlineData(
                            mimeType = "application/pdf",
                            data = base64Pdf
                        )
                    )
                )
            }

            val fullPrompt = buildString {
                append(prompt)
                append("\n\n---\n")
                append("Instrucciones adicionales:\n")
                append("- Analiza el documento página por página como imágenes.\n")
                append("- Extrae tablas y datos estructurados con precisión.\n")
                append("- Responde en formato JSON válido.\n")
                append("- No incluyas markdown ni comentarios fuera del JSON.")
            }

            parts.add(GeminiPart(text = fullPrompt))

            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = parts)
                ),
                generationConfig = GenerationConfig(
                    maxOutputTokens = 8192,
                    temperature = 0.2f,
                    topK = 40,
                    topP = 0.95f
                )
            )

            val json = gson.toJson(requestBody)
            val request = Request.Builder()
                .url("$baseUrl/v1beta/models/$modelName:generateContent?key=$apiKey")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                return@withContext Result.failure(Exception("Error HTTP ${response.code}: $errorBody"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Respuesta vacía"))
            val geminiResponse = gson.fromJson(body, GeminiResponse::class.java)

            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("Gemini regresó una respuesta vacía"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateStructuredJson(
        apiKey: String,
        prompt: String,
        pdfBytes: ByteArray?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API Key no configurada"))
            }

            val parts = mutableListOf<GeminiPart>()

            if (pdfBytes != null) {
                val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                parts.add(
                    GeminiPart(
                        inlineData = InlineData(
                            mimeType = "application/pdf",
                            data = base64Pdf
                        )
                    )
                )
            }

            val fullPrompt = buildString {
                append("Responde ÚNICAMENTE con un objeto JSON válido. No incluyas explicaciones ni markdown.\n\n")
                append(prompt)
                if (pdfBytes != null) {
                    append("\n\nAnaliza el documento PDF página por página como imágenes.")
                    append("\nPresta especial atención a las tablas y datos estructurados.")
                }
            }

            parts.add(GeminiPart(text = fullPrompt))

            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = parts)
                ),
                generationConfig = GenerationConfig(
                    maxOutputTokens = 8192,
                    temperature = 0.1f,
                    topK = 1,
                    topP = 1.0f
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "Eres un asistente especializado en extraer información estructurada. Responde SOLO con JSON válido."
                        )
                    )
                )
            )

            val json = gson.toJson(requestBody)
            val request = Request.Builder()
                .url("$baseUrl/v1beta/models/$modelName:generateContent?key=$apiKey")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                return@withContext Result.failure(Exception("Error HTTP ${response.code}: $errorBody"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Respuesta vacía"))
            val geminiResponse = gson.fromJson(body, GeminiResponse::class.java)

            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("Gemini regresó una respuesta vacía"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Data classes internas para evitar fugas de serialización
    data class GeminiRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GenerationConfig,
        val systemInstruction: GeminiContent? = null
    )

    data class GeminiContent(
        val parts: List<GeminiPart>
    )

    data class GeminiPart(
        val text: String? = null,
        val inlineData: InlineData? = null
    )

    data class InlineData(
        val mimeType: String,
        val data: String
    )

    data class GenerationConfig(
        val maxOutputTokens: Int,
        val temperature: Float,
        val topK: Int,
        val topP: Float
    )

    data class GeminiResponse(
        val candidates: List<GeminiCandidate>?
    )

    data class GeminiCandidate(
        val content: GeminiContent?
    )
}
