package com.clearkeep.screen.chat.home.home

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.*
import com.clearkeep.components.base.CKTextInputField
import com.clearkeep.db.clear_keep.model.Server
import com.clearkeep.screen.chat.home.home.composes.CircleAvatarWorkSpace

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
) {
    val rooms = homeViewModel.groups.observeAsState()
    Row(
        Modifier
            .fillMaxSize()
            .background(grayscaleOffWhite)
    ) {
        Box(
            Modifier
                .width(88.dp)
                .background(Color.White)
        ) {
            LeftMenu(homeViewModel)
        }
        Column(
            Modifier.fillMaxSize()
        ) {
            WorkSpaceView(homeViewModel)
        }
    }

}


@Composable
fun LeftMenu(mainViewModel: HomeViewModel) {
    val workSpaces = mainViewModel.servers.observeAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier
                .weight(0.66f)
                .background(
                    shape = RoundedCornerShape(topEnd = 30.dp),
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    )
                )
        ) {
            Column {
                workSpaces.value?.let { item ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 30.dp,
                            end = 20.dp,
                            start = 20.dp,
                            bottom = 30.dp
                        ),
                    ) {
                        itemsIndexed(item) { _, workSpace ->
                            itemListWorkSpace(workSpace)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(2.dp))

        Column(
            modifier = Modifier
                .height(98.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundGradientStart,
                            backgroundGradientEnd
                        )
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
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

@Composable
fun itemListWorkSpace(item: Server) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .border(1.dp, Color.Red)
            .size(56.dp)
    ) {
        CircleAvatarWorkSpace(url = item.serverAvatar, name = item.serverName)
    }
}

@Composable
fun ChatGroupItemView(chatGroup: ChatGroupDummy) {
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
    ) {
        Row() {
            Text(
                text = chatGroup.name, modifier = Modifier
                    .weight(0.66f), style = TextStyle(
                    color = grayscale2, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        shape = CircleShape,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Red
                            )
                        )
                    )
                    .size(24.dp)
            ) {
                Text(
                    text = chatGroup.numberUnread.toString(),
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )

            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatGroupView(viewModel: HomeViewModel) {
    val chatGroupDummy = viewModel.chatGroupDummy.observeAsState()
    val rememberitemGroup = remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.wrapContentHeight()
    ) {
        Row(modifier = Modifier,verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(0.66f)
            ) {
                Text(
                    text = "Group Chat (${chatGroupDummy.value?.size})", style = TextStyle(
                        color = grayscaleBlack, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                )
                Box(modifier = Modifier
                    .clickable {
                        rememberitemGroup.value = !rememberitemGroup.value
                    }
                    .padding(8.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_chev_down),
                        null,
                        alignment = Alignment.Center
                    )
                }

            }
            Box(modifier = Modifier
                .clickable {}
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
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            start = 20.dp,
                            bottom = 16.dp
                        ),
                    ) {
                        itemsIndexed(item) { _, chatGroup ->
                            ChatGroupItemView(chatGroup)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkSpaceView(homeViewModel: HomeViewModel) {
    val searchKey = remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp,end = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))
        Row() {
            Text(
                text = "CK Development", modifier = Modifier
                    .weight(0.66f), style = TextStyle(
                    color = grayscaleBlack, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hamburger),
                    null, alignment = Alignment.Center
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        CKTextInputField(
            "Search",
            singleLine = true,
            leadingIcon = {
                Image(
                    painterResource(R.drawable.ic_search),
                    contentDescription = null
                )
            }, modifier = Modifier, textValue = searchKey
        )
        Spacer(modifier = Modifier.size(24.dp))
        NoteView()
        Spacer(modifier = Modifier.size(24.dp))
        ChatGroupView(homeViewModel)
        ChatGroupView(homeViewModel)
    }
}

@Composable
fun NoteView() {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.ic_notes),
            contentDescription = null,
        )
        Text(
            text = "Notes", modifier = Modifier, style = TextStyle(
                color = grayscaleBlack, fontSize = 24.sp, fontWeight = FontWeight.Bold
            )
        )
    }

}