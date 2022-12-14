package com.clearkeep.features.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clearkeep.common.utilities.EXTRA_GROUP_ID
import com.clearkeep.common.utilities.EXTRA_OWNER_CLIENT
import com.clearkeep.common.utilities.EXTRA_OWNER_DOMAIN
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.domain.usecase.group.GetGroupByIdUseCase
import com.clearkeep.domain.usecase.message.GetUnreadMessageUseCase
import com.clearkeep.domain.usecase.preferences.GetUserPreferenceUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShowSummaryNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var getUserPreferenceUseCase: GetUserPreferenceUseCase

    @Inject
    lateinit var getGroupByIdUseCase: GetGroupByIdUseCase

    @Inject
    lateinit var getUnreadMessageUseCase: GetUnreadMessageUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        val ownerClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        val ownerDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN) ?: ""
        if (ownerClientId.isNotBlank() && ownerDomain.isNotBlank() && groupId != 0L) {
            handleShowMessageSummary(context, groupId, ownerClientId, ownerDomain)
        }
    }

    private fun handleShowMessageSummary(
        context: Context,
        groupId: Long,
        ownerClientId: String,
        ownerDomain: String
    ) {
        GlobalScope.launch {
            val unreadMessages =
                getUnreadMessageUseCase(groupId, ownerDomain, ownerClientId)
            val group = getGroupByIdUseCase(groupId, ownerDomain, ownerClientId)
            if (group?.data != null && unreadMessages.isNotEmpty()) {
                val me = group.data!!.clientList.find { it.userId == ownerClientId }
                    ?: com.clearkeep.domain.model.User(
                        userId = ownerClientId,
                        userName = "me",
                        domain = ownerDomain
                    )
                val userPreference =
                    getUserPreferenceUseCase(ownerDomain, ownerClientId)
                showMessageNotificationToSystemBar(
                    context,
                    me,
                    group.data!!,
                    unreadMessages,
                    userPreference ?: UserPreference.getDefaultUserPreference("", "", false)
                )
            }
        }
    }
}