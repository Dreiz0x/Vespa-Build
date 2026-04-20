package dev.vskelk.cdf.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.vskelk.cdf.core.datastore.CipherService
import dev.vskelk.cdf.core.datastore.PreferencesDataSource
import dev.vskelk.cdf.core.datastore.UserPreferencesSerializer
import javax.inject.Singleton

/**
 * DataStoreModule - Módulo de inyección para Proto DataStore
 *
 * Proporciona las dependencias para PreferencesDataSource y
 * el servicio de cifrado.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideCipherService(
        @ApplicationContext context: Context
    ): CipherService = CipherService(context)

    @Provides
    @Singleton
    fun provideUserPreferencesSerializer(): UserPreferencesSerializer =
        UserPreferencesSerializer()

    // ⚡ Construimos el DataSource limpio, solo con Context y CipherService.
    @Provides
    @Singleton
    fun providePreferencesDataSource(
        @ApplicationContext context: Context,
        cipherService: CipherService
    ): PreferencesDataSource = PreferencesDataSource(
        context, 
        cipherService
    )
}
