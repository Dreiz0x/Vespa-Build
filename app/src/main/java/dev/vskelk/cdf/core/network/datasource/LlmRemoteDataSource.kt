package dev.vskelk.cdf.core.network.datasource

/**
 * LlmRemoteDataSource - Interfaz para servicios de modelos de lenguaje
 */
interface LlmRemoteDataSource {
    suspend fun sendMessage(
        apiKey: String,
        prompt: String,
        pdfBytes: ByteArray? = null
    ): Result<String>
}
