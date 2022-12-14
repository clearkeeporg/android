package com.clearkeep.data.local.clearkeep.userkey

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.clearkeep.domain.model.UserKey

@Entity(tableName = "UserKey", primaryKeys = ["server_domain", "user_id"])
data class UserKeyEntity(
    @ColumnInfo(name = "server_domain")
    val serverDomain: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "salt")
    val salt: String,
    @ColumnInfo(name = "iv")
    val iv: String
)