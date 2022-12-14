package com.clearkeep.data.local.signal.prekey

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.clearkeep.data.local.signal.prekey.SignalPreKey

@Dao
interface SignalPreKeyDAO {
    @Insert(onConflict = REPLACE)
    fun insert(preKey: SignalPreKey)

    @Query("SELECT * FROM signalprekey WHERE is_signed_key = 0 LIMIT 1")
    suspend fun getFirstUnSignedPreKey(): SignalPreKey?

    @Query("SELECT * FROM signalprekey WHERE is_signed_key = 1 LIMIT 1")
    suspend fun getFirstSignedPreKey(): SignalPreKey?

    @Query("SELECT * FROM signalprekey WHERE preKeyId =:preKeyId AND is_signed_key = 0 AND domain = :domain AND user_id = :userId LIMIT 1")
    fun getUnSignedPreKey(preKeyId: Int, domain: String, userId: String): SignalPreKey?

    @Query("SELECT * FROM signalprekey WHERE is_signed_key = 0")
    fun getAllUnSignedPreKey(): List<SignalPreKey>

    @Query("SELECT * FROM signalprekey WHERE preKeyId =:preKeyId AND is_signed_key = 1 LIMIT 1")
    fun getSignedPreKey(preKeyId: Int): SignalPreKey?

    @Query("SELECT * FROM signalprekey WHERE is_signed_key = 1")
    fun getAllSignedPreKey(): List<SignalPreKey>

    @Query("DELETE FROM signalprekey WHERE domain =:domain AND user_id = :userId")
    suspend fun deleteSignalSenderKey(domain: String, userId: String): Int
}