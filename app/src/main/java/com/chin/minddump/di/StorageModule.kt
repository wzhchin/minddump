package com.chin.minddump.di

import android.content.Context
import androidx.room3.Room
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
    ): MindDumpDatabase {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("minddump.db")
        // Room3 has no legacy SupportSQLiteDatabase Migration-callback API, and the
        // filesystem is the source of truth (Room is an index cache). Any schema change
        // — including an upgrade from the Room 2.x DB an existing user may already have
        // on disk — is handled by a destructive rebuild repopulated from disk via the
        // database-rebuild capability, rather than hand-written migrations.
        return Room
            .databaseBuilder<MindDumpDatabase>(context, dbFile.absolutePath)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

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
