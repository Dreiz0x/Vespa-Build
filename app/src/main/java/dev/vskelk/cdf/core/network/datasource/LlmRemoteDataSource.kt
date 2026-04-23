package dev.vskelk.cdf.core.network.datasource

interface LlmRemoteDataSource {
    suspend fun sendMessage(
        apiKey: String,
        prompt: String,
        pdfBytes: ByteArray? = null
    ): Result<String>
}
