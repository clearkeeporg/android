package com.clearkeep.features.chat.presentation.room.roomdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.HeaderTextType
import com.clearkeep.common.presentation.components.base.CKHeaderText
import com.clearkeep.features.chat.presentation.composes.NewFriendListItem
import com.clearkeep.features.chat.presentation.room.RoomViewModel

@Composable
fun GroupMemberScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {
    val groupState = roomViewModel.group.observeAsState()
    val listUserStatusState = roomViewModel.listGroupUserStatus.observeAsState()

    groupState.value?.let { group ->
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Column {
                RoomMemberHeader {
                    navHostController.popBackStack()
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        top = 20.dp,
                        bottom = 20.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                ) {
                    itemsIndexed(group.clientList.filter { it.userState == com.clearkeep.domain.model.UserStateTypeInGroup.ACTIVE.value }) { _, friend ->
                        val newUser = listUserStatusState.value?.find {
                            it.userId == friend.userId
                        }
                        friend.userStatus = newUser?.userStatus
                        friend.avatar = newUser?.avatar
                        NewFriendListItem(Modifier.padding(vertical = 8.dp), friend)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomMemberHeader(onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onCloseView.invoke()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chev_left),
                    contentDescription = null,
                    tint = LocalColorMapping.current.headerText,
                )
            }
            CKHeaderText(
                stringResource(R.string.see_members), modifier = Modifier
                    .weight(1.0f, true), headerTextType = HeaderTextType.Medium
            )
        }
    }
}