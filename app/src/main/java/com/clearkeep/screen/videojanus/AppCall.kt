package com.clearkeep.screen.videojanus

import android.content.Context
import android.content.Intent
import android.util.Log
import com.clearkeep.utilities.*

object AppCall {
    fun call(context: Context, token: String, groupId: String?, ourClientId: String?, userName: String?, avatar: String?, isIncomingCall: Boolean,
             turnUrl: String = "", turnUser: String = "", turnPass: String = "",
             stunUrl: String = ""
    ) {
        printlnCK("token = $token, groupID = $groupId")
        val intent = Intent(context, InCallActivity::class.java)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, isIncomingCall)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        if (turnUrl.isNotEmpty()) {
            intent.putExtra(EXTRA_TURN_URL, turnUrl)
            intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
            intent.putExtra(EXTRA_TURN_PASS, turnPass)
        }
        if (stunUrl.isNotEmpty()) {
            intent.putExtra(EXTRA_STUN_URL, stunUrl)
        }
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun inComingCall(context: Context, token: String, groupId: String?, ourClientId: String?,
                     userName: String?, avatar: String?,
                     turnUrl: String, turnUser: String, turnPass: String,
                     stunUrl: String) {
        printlnCK("token = $token, groupID = $groupId, turnURL= $turnUrl, turnUser=$turnUser, turnPass= $turnPass, stunUrl = $stunUrl")
        val intent = Intent(context, InComingCallActivity::class.java)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_URL, turnUrl)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.putExtra(EXTRA_STUN_URL, stunUrl)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isCallAvailable(context: Context): Boolean {
        /*return isServiceRunning(context, InCallForegroundService::class.java.name)*/
        return false
    }

    fun openCallAvailable(context: Context) {
        val intent = Intent(context, InCallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}