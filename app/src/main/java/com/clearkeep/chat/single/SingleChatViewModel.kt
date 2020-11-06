package com.clearkeep.chat.single

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.chat.repositories.ChatRepository
import com.clearkeep.chat.repositories.RoomRepository
import com.clearkeep.db.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SingleChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val roomRepository: RoomRepository
): ViewModel() {

    fun getMyClientId() = chatRepository.getMyClientId()

    fun getMessageList(roomId: Int): LiveData<List<Message>> {
        return chatRepository.getMessagesFromSender(roomId)
    }

    fun sendMessage(receiverId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(receiverId, message)
        }
    }

    fun insertSingleRoom(remoteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            roomRepository.insertSingleRoom(remoteId)
        }
    }

    fun getSingleRooms() = roomRepository.getSingleRooms()
}
