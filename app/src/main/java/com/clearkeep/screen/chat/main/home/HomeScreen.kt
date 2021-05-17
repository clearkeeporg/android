package com.clearkeep.screen.chat.main.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKHeaderText
import com.clearkeep.components.base.CKSearchBox
import com.clearkeep.components.base.HeaderTextType
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.main.home.composes.CircleAvatarStatus
import com.clearkeep.screen.chat.main.home.composes.CircleAvatarWorkSpace
import com.clearkeep.screen.chat.main.profile.ProfileViewModel
import com.clearkeep.screen.chat.main.home.composes.SiteMenuScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavController,
    gotoSearch: (() -> Unit),
    createGroupChat: ((isDirectGroup: Boolean) -> Unit),
    gotoRoomById: ((idRoom: Long) -> Unit),
    onlogout: (() -> Unit),
) {
    val rooms = homeViewModel.groups.observeAsState()
    val rememberStateSiteMenu = remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxSize()
    ) {
        Box(
            Modifier
                .width(84.dp)
        ) {
            LeftMenu(homeViewModel)
        }
        Column(
            Modifier.fillMaxSize()
        ) {
            WorkSpaceView(
                homeViewModel,
                gotoSearch,
                createGroupChat,
                gotoProfile = {
                    rememberStateSiteMenu.value = true
                },
                onItemClickListener = gotoRoomById
            )
        }

    }
    AnimatedVisibility(
        visible = rememberStateSiteMenu.value,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 3 },
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { 200 },
            animationSpec = spring(stiffness = Spring.StiffnessHigh)
        ) + fadeOut()

    ) {
        Column(
            Modifier
                .fillMaxSize()
                .focusable(true)

        ) {
            SiteMenuScreen(homeViewModel,profileViewModel,navController, closeSiteMenu = {
                rememberStateSiteMenu.value = false
            }, onLogout = onlogout)
        }
    }

}


@Composable
fun LeftMenu(mainViewModel: HomeViewModel) {
    val workSpaces = mainViewModel.servers.observeAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.66f)
                .padding(top = 20.dp)
                .background(
                    shape = RoundedCornerShape(topEnd = 30.dp),
                    brush = Brush.horizontalGradient(
                        //todo disable dark mode
                        colors = listOf(
                            if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                            if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd
                        )
                    )
                ), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.background(
                    color = grayscaleOverlay, shape = RoundedCornerShape(topEnd = 30.dp),
                ), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                workSpaces.value?.let { item ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(
                            top = 20.dp,
                            end = 10.dp,
                            start = 10.dp,
                            bottom = 20.dp
                        ),
                    ) {
                        itemsIndexed(item) { _, workSpace ->
                            Column(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(color = Color.Transparent)
                                    .border(
                                        BorderStroke(1.5.dp, primaryDefault),
                                        shape = CircleShape
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center

                            ) {
                                CircleAvatarWorkSpace(workSpace, mainViewModel.channelIdLive)
                            }
                        }

                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(1.dp))

        Column(
            modifier = Modifier
                .height(98.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        //todo disable dark mode
                        colors = listOf(
                            if (isSystemInDarkTheme()) grayscale1 else backgroundGradientStart,
                            if (isSystemInDarkTheme()) grayscale5 else backgroundGradientEnd
                        )
                    )
                ),

            ) {
            Column(
                Modifier
                    .height(98.dp)
                    .fillMaxWidth()
                    .background(
                        color = grayscaleOverlay,
                    ), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_button_profile),
                    null,
                    Modifier.size(50.dp),
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Composable
fun ItemListDirectMessage(
    modifier: Modifier,
    chatGroup: ChatGroup,
    clintId: String,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable {
                onItemClickListener?.invoke(chatGroup.id)
            }
    ) {
        val roomName = if (chatGroup.isGroup()) chatGroup.groupName else {
            chatGroup.clientList.firstOrNull { client ->
                client.id != clintId
            }?.userName ?: ""
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatarStatus(url = "", name = roomName, status = "")
            Text(
                text = roomName, modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}


@Composable
fun ChatGroupItemView(
    modifier: Modifier,
    chatGroup: ChatGroup,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable {
                onItemClickListener?.invoke(chatGroup.id)
            },
    ) {
        Row(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = chatGroup.groupName, modifier = Modifier.fillMaxWidth(),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatGroupView(
    viewModel: HomeViewModel, createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    val chatGroups = viewModel.chatGroups.observeAsState()
    val rememberitemGroup = remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.wrapContentHeight()
    ) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f), verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.clickable {
                    rememberitemGroup.value = !rememberitemGroup.value
                }, verticalAlignment = Alignment.CenterVertically) {
                    CKHeaderText(
                        text = "Group Chat (${chatGroups.value?.size})",
                        headerTextType = HeaderTextType.Normal, color = grayscale2
                    )

                    Box(modifier = Modifier.padding(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chev_down),
                            null,
                            alignment = Alignment.Center
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .clickable {
                    createGroupChat.invoke(false)
                }
                .size(24.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    null,
                    alignment = Alignment.Center
                )
            }
        }
        Column() {
            AnimatedVisibility(
                visible = rememberitemGroup.value,
                modifier = Modifier.padding(top = 4.dp),
                enter = expandIn(
                    expandFrom = Alignment.BottomStart, initialSize = { IntSize(50, 50) },
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ),
                exit = shrinkOut(
                    shrinkTowards = Alignment.CenterStart,
                    targetSize = { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 10) },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )

            ) {
                chatGroups.value?.let { item ->
                    Column {
                        item.forEach { chatGroup ->
                            ChatGroupItemView(modifier = Modifier.padding(start = 15.dp),chatGroup = chatGroup, onItemClickListener = onItemClickListener)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DirectMessagesView(
    viewModel: HomeViewModel,
    createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)? = null
) {
    val chatGroupDummy = viewModel.directGroups.observeAsState()
    val rememberItemGroup = remember { mutableStateOf(true) }
    Column(modifier = Modifier.wrapContentHeight()) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f), verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            rememberItemGroup.value = !rememberItemGroup.value
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    CKHeaderText(
                        text = "Direct Messages (${chatGroupDummy.value?.size})",
                        headerTextType = HeaderTextType.Normal, color = grayscale2
                    )
                    Box(modifier = Modifier.padding(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chev_down),
                            null,
                            alignment = Alignment.Center
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .clickable {
                    createGroupChat.invoke(true)
                }
                .size(24.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    null,
                    alignment = Alignment.Center
                )
            }
        }
        Column() {
            AnimatedVisibility(
                visible = rememberItemGroup.value,
                modifier = Modifier.padding(bottom = 20.dp),
                enter = expandIn(
                    expandFrom = Alignment.BottomStart, initialSize = { IntSize(50, 50) },
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ),
                exit = shrinkOut(
                    shrinkTowards = Alignment.CenterStart,
                    targetSize = { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 10) },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )

            ) {
                chatGroupDummy.value?.let { item ->
                    Column {
                        item.forEach { chatGroup ->
                            ItemListDirectMessage(
                                modifier = Modifier.padding(start = 16.dp),
                                chatGroup,
                                viewModel.myClintId,
                                onItemClickListener
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkSpaceView(
    homeViewModel: HomeViewModel,
    gotoSearch: () -> Unit,
    createGroupChat: (isDirectGroup: Boolean) -> Unit,
    onItemClickListener: ((Long) -> Unit)?,
    gotoProfile: () -> Unit
) {
    val searchKey = remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 20.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            CKHeaderText(
                text = "CK Development", modifier = Modifier
                    .weight(0.66f), headerTextType = HeaderTextType.Large
            )
            Column(
                modifier = Modifier.clickable { gotoProfile.invoke() },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hamburger),
                    null, alignment = Alignment.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        CKSearchBox(searchKey)
        Spacer(modifier = Modifier.size(24.dp))
        NoteView()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

        ) {
            Spacer(modifier = Modifier.size(24.dp))
            ChatGroupView(
                homeViewModel, createGroupChat = createGroupChat,
                onItemClickListener = onItemClickListener
            )
            Spacer(modifier = Modifier.size(28.dp))
            DirectMessagesView(
                homeViewModel,
                createGroupChat = createGroupChat,
                onItemClickListener = onItemClickListener
            )
        }
    }
}

@Composable
fun NoteView() {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.ic_notes),
            contentDescription = null,
        )
        CKHeaderText(
            text = stringResource(R.string.notes),
            modifier = Modifier.padding(start = 10.dp),
            headerTextType = HeaderTextType.Normal,
            color = grayscale2,
        )
    }
}

