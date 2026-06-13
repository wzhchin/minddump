package com.chin.minddump.di

import android.content.Context
import androidx.room.Room
import com.chin.minddump.data.MIGRATION_1_2
import com.chin.minddump.data.MIGRATION_2_3
import com.chin.minddump.data.MindDumpDatabase
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.security.CryptoEngine
import com.chin.minddump.security.PasswordStore
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.StoragePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideStoragePreferences(
        @ApplicationContext context: Context,
    ): StoragePreferences = StoragePreferences(context)

    @Provides
    @Singleton
    fun provideFileStorageEngine(
        @ApplicationContext context: Context,
    ): FileStorageEngine = FileStorageEngine(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MindDumpDatabase =
        Room
            .databaseBuilder(
                context,
                MindDumpDatabase::class.java,
                "minddump.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun provideCryptoEngine(): CryptoEngine = CryptoEngine()

    @Provides
    @Singleton
    fun providePasswordStore(
        @ApplicationContext context: Context,
    ): PasswordStore = PasswordStore(context)

    @Provides
    @Singleton
    fun provideRepository(
        database: MindDumpDatabase,
        storageEngine: FileStorageEngine,
        cryptoEngine: CryptoEngine,
        passwordStore: PasswordStore,
    ): MindDumpRepository = MindDumpRepository(database, storageEngine, cryptoEngine, passwordStore)
}
