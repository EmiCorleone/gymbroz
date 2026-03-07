package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile): Long

    @Update
    suspend fun update(profile: UserProfile)

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun getProfileCount(): Int

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
