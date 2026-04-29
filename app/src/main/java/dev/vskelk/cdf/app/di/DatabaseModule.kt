package dev.vskelk.cdf.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.vskelk.cdf.core.database.AppDatabase
import dev.vskelk.cdf.core.database.dao.*
import dev.vskelk.cdf.core.database.dao.OntologyDao
import javax.inject.Singleton

/**
 * DatabaseModule - Módulo de inyección para Room Database
 *
 * Proporciona la base de datos singleton y todos los DAOs.
 *
 * Per spec: FallbackToDestructiveMigration durante desarrollo.
 * Migración explícita antes de release.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // FallbackToDestructiveMigration durante desarrollo
            // Per spec: Migración explícita antes de release
            .fallbackToDestructiveMigration()
            .build()
    }

    // ===== DAOs =====

    @Provides
    @Singleton
    fun provideOntologyDao(database: AppDatabase): OntologyDao = database.ontologyDao()

    // No change needed here, already correct

    @Provides
    fun provideNormativeDao(database: AppDatabase): NormativeDao = database.normativeDao()

    @Provides
    fun provideReactivoDao(database: AppDatabase): ReactivoDao = database.reactivoDao()

    @Provides
    fun provideUserMasteryDao(database: AppDatabase): UserMasteryDao = database.userMasteryDao()

    @Provides
    fun provideStudySessionDao(database: AppDatabase): StudySessionDao = database.studySessionDao()

    // This line is not present, so we add it

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideCuarentenaDao(database: AppDatabase): QuarantineDao = database.cuarentenaDao()
}
