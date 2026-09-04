package com.khatibstudio.noyza.di

import android.content.Context
import androidx.room.Room
import com.khatibstudio.noyza.data.local.dao.NoiseSampleDao
import com.khatibstudio.noyza.data.local.dao.PlaceDao
import com.khatibstudio.noyza.data.local.dao.SessionDao
import com.khatibstudio.noyza.data.local.database.NoyZaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoyZaDatabase {
        return Room.databaseBuilder(
            context,
            NoyZaDatabase::class.java,
            NoyZaDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaceDao(db: NoyZaDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideSessionDao(db: NoyZaDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideNoiseSampleDao(db: NoyZaDatabase): NoiseSampleDao = db.noiseSampleDao()
}
