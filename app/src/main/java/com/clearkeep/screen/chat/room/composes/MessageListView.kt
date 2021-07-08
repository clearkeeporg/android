package com.clearkeep.screen.chat.room.composes

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscale3
import com.clearkeep.components.grayscaleBackground
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.screen.chat.room.message_display_generator.convertMessageList
import com.clearkeep.utilities.getTimeAsString
import kotlinx.coroutines.launch

var mIsNewMessage = true

@ExperimentalFoundationApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageListView(
    messageList: List<Message>,
    clients: List<User>,
    myClientId: String,
    isGroup: Boolean,
    isNewMessage: Boolean = true
) {
    mIsNewMessage = isNewMessage
    MessageListView(
        messageList = messageList,
        clients = clients,
        myClientId = myClientId,
        isGroup = isGroup,
    )
}

@ExperimentalFoundationApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MessageListView(
    messageList: List<Message>,
    clients: List<User>,
    myClientId: String,
    isGroup: Boolean,
) {
    val reversedMessage = messageList.reversed()
    val groupedMessages: Map<String, List<MessageDisplayInfo>> = reversedMessage.groupBy { getTimeAsString(it.createdTime) }.mapValues { entry ->
        convertMessageList(entry.value, clients, myClientId, isGroup)
    }
    Surface(
        color = grayscaleBackground
    ) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        if (listState.firstVisibleItemIndex == 0) {
            mIsNewMessage = false
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            state = listState,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        ) {
            groupedMessages.forEach { (date, messages) ->
                itemsIndexed(messages) { index, item ->
                    Column {
                        if (index == messages.size -1) {
                            DateHeader(date)
                        }
                        if (item.isOwner) MessageByMe(item) else MessageFromOther(item)
                        if (index == 0) Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
        val showButton = remember {
            derivedStateOf {
                val isBottom = listState.firstVisibleItemIndex == 0
                if (isBottom) {
                    mIsNewMessage = false
                }
                !isBottom
            }
        }
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (showButton.value) {
                ScrollToButtonButton(
                    isNewMessage = mIsNewMessage,
                    onClick = {
                        mIsNewMessage = false
                        coroutineScope.launch {
                            listState.scrollToItem(0)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(date, style = MaterialTheme.typography.body2.copy(
            color = grayscale3,
            fontWeight = FontWeight.W600
        ))
    }
}

@Composable
fun ScrollToButtonButton(isNewMessage: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isNewMessage) Text(text = "new message", style = MaterialTheme.typography.caption.copy(color = Color.Blue))
        Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = "", tint = Color.Blue)
    }
}