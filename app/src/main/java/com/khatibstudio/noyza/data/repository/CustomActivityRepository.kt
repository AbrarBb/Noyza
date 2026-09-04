package com.khatibstudio.noyza.data.repository

import com.khatibstudio.noyza.data.local.dao.CustomActivityDao
import com.khatibstudio.noyza.data.local.entity.CustomActivityEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomActivityRepository @Inject constructor(
    private val customActivityDao: CustomActivityDao
) {
    fun getAllCustomActivities(): Flow<List<CustomActivityEntity>> =
        customActivityDao.getAllCustomActivities()

    suspend fun getCustomActivityById(id: Long): CustomActivityEntity? =
        customActivityDao.getCustomActivityById(id)

    suspend fun saveCustomActivity(activity: CustomActivityEntity): Long =
        customActivityDao.insertCustomActivity(activity)

    suspend fun deleteCustomActivity(activity: CustomActivityEntity) =
        customActivityDao.deleteCustomActivity(activity)

    suspend fun deleteAllCustomActivities() =
        customActivityDao.deleteAllCustomActivities()
}
