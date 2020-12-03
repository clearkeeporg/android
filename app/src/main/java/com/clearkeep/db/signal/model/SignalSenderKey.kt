package com.clearkeep.db.signal.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SignalSenderKey(
        @ColumnInfo(name = "group_id") val groupId: String,
        @ColumnInfo(name = "sender_name") val senderName: String,
        @ColumnInfo(name = "device_id") val deviceId: Int,
        @ColumnInfo(name = "sender_key") val senderKey: ByteArray,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignalSenderKey

        if (groupId != other.groupId) return false
        if (senderName != other.senderName) return false
        if (deviceId != other.deviceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + senderName.hashCode()
        result = 31 * result + deviceId
        return result
    }
}