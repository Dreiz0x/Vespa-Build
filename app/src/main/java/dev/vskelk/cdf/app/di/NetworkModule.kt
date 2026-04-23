package dev.vskelk.cdf.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vskelk.cdf.core.network.resilience.CircuitBreaker
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * NetworkModule - Módulo de inyección para red
 *
 * Purgado de dependencias inútiles.
 * Preparado EXCLUSIVAMENTE para inyectar el cliente nativo de Gemini (generativeai SDK).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideCircuitBreaker(): CircuitBreaker = CircuitBreaker()

    // Nota: El cliente principal de Gemini (GenerativeModel) no se inyecta como un Singleton estático
    // porque necesita leer la API Key dinámicamente desde el DataStore.
    // La inyección del motor la haremos en un GeminiRemoteDataSource dedicado.
}
