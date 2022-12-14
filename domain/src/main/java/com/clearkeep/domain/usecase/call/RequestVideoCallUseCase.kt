package com.clearkeep.domain.usecase.call

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.response.ServerResponse
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.domain.repository.VideoCallRepository
import javax.inject.Inject

class RequestVideoCallUseCase @Inject constructor(
    private val videoCallRepository: VideoCallRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(
        groupId: Int,
        isAudioMode: Boolean,
        owner: Owner
    ): ServerResponse? {
        val server = serverRepository.getServer(owner.domain, owner.clientId) ?: return null
        return videoCallRepository.requestVideoCall(groupId, isAudioMode, server)
    }
}