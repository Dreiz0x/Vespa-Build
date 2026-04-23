package dev.vskelk.cdf.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vskelk.cdf.core.network.datasource.GeminiRemoteDataSource
import dev.vskelk.cdf.core.network.datasource.LlmRemoteDataSource
import dev.vskelk.cdf.core.network.resilience.CircuitBreaker
import kotlinx.serialization.json.Json
import javax.inject.Singleton

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

    @Provides
    @Singleton
    fun provideLlmRemoteDataSource(
        geminiDataSource: GeminiRemoteDataSource
    ): LlmRemoteDataSource {
        return geminiDataSource
    }
}
