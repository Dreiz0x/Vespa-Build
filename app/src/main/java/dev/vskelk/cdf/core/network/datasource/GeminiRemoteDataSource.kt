package dev.vskelk.cdf.core.network.datasource

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRemoteDataSource @Inject constructor() : LlmRemoteDataSource {

    override suspend fun sendMessage(
        apiKey: String,
        prompt: String,
        pdfBytes: ByteArray?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-pro",
                apiKey = apiKey
            )

            val response = if (pdfBytes != null) {
                generativeModel.generateContent(
                    content {
                        blob("application/pdf", pdfBytes)
                        text(prompt)
                    }
                )
            } else {
                generativeModel.generateContent(prompt)
            }

            val resultText = response.text
            if (resultText != null) {
                Result.success(resultText)
            } else {
                Result.failure(Exception("Gemini regresó una respuesta vacía"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
