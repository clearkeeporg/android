package com.clearkeep.data.local.clearkeep.user

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.clearkeep.domain.model.User

@Dao
interface UserDAO {
    @Insert(onConflict = REPLACE)
    suspend fun insert(people: UserEntity)

    @Insert(onConflict = REPLACE)
    suspend fun insertPeopleList(people: List<UserEntity>)

    @Query("UPDATE UserEntity SET avatar =:avatar WHERE user_id =:userId")
    suspend fun updateAvatar(
        avatar: String,
        userId: String
    )

    @Query("UPDATE UserEntity SET avatar =:userName WHERE user_id =:userId")
    suspend fun updateUserName(
        userName: String,
        userId: String
    )

    @Query("SELECT * FROM userentity WHERE user_id =:userId AND domain = :domain AND owner_domain = :ownerDomain AND owner_client_id = :ownerClientId LIMIT 1")
    suspend fun getFriend(
        userId: String,
        domain: String,
        ownerDomain: String,
        ownerClientId: String
    ): UserEntity?

    @Query("SELECT * FROM userentity WHERE user_id =:userId LIMIT 1")
    suspend fun getFriendFromUserId(userId: String): UserEntity?

    @Query("SELECT * FROM userentity WHERE owner_domain = :ownerDomain AND owner_client_id = :ownerClientId")
    fun getFriends(ownerDomain: String, ownerClientId: String): LiveData<List<UserEntity>>

    @Query("DELETE  FROM userentity WHERE  user_id = :ownerId")
    suspend fun deleteFriend(ownerId: String): Int

    @Query("SELECT * FROM userentity")
    suspend fun getListUserEntity(): List<UserEntity>
}