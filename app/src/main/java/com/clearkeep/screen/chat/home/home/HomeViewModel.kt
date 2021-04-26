package com.clearkeep.screen.chat.home.home

import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.repo.*
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val roomRepository: GroupRepository,
): ViewModel() {
    init {
        // TODO: load channel and select first channel as default
        selectChannel(1)
    }

    val servers: LiveData<List<Server>> = liveData {
        emit(listOf(Server(1, "CK Development", "")))
    }

    val groups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    val chatGroups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    val directGroups: LiveData<List<ChatGroup>> = roomRepository.getAllRooms()

    fun searchGroup(text: String) {}

    fun selectChannel(channelId: Long) {}
}
