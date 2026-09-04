package com.khatibstudio.noyza.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.khatibstudio.noyza.data.local.dao.CustomActivityDao
import com.khatibstudio.noyza.data.local.dao.NoiseSampleDao
import com.khatibstudio.noyza.data.local.dao.PlaceDao
import com.khatibstudio.noyza.data.local.dao.SessionDao
import com.khatibstudio.noyza.data.local.entity.CustomActivityEntity
import com.khatibstudio.noyza.data.local.entity.NoiseSampleEntity
import com.khatibstudio.noyza.data.local.entity.PlaceEntity
import com.khatibstudio.noyza.data.local.entity.SessionEntity

@Database(
    entities = [
        PlaceEntity::class,
        SessionEntity::class,
        NoiseSampleEntity::class,
        CustomActivityEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NoyZaDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun sessionDao(): SessionDao
    abstract fun noiseSampleDao(): NoiseSampleDao
    abstract fun customActivityDao(): CustomActivityDao

    companion object {
        const val DATABASE_NAME = "noyza_db"
    }
}
