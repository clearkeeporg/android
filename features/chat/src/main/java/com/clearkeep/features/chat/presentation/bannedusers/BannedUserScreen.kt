package com.clearkeep.features.chat.presentation.bannedusers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.base.*
import com.clearkeep.common.presentation.components.grayscale1
import com.clearkeep.common.presentation.components.grayscale2
import com.clearkeep.common.presentation.components.grayscale5
import com.clearkeep.features.chat.presentation.composes.NewFriendListItem
import com.clearkeep.features.chat.presentation.composes.StatusText
import com.clearkeep.common.utilities.sdp

@Composable
fun BannedUserScreen(onCloseView: () -> Unit) {
    val text = rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .padding(horizontal = 16.sdp())
            .fillMaxSize()
    ) {
        HeaderBannedUser(onCloseView)
        Spacer(modifier = Modifier.height(24.sdp()))
        CKSearchBox(
            text,
            Modifier
                .clip(RoundedCornerShape(16.sdp()))
                .background(grayscale5)
        )
        Spacer(modifier = Modifier.height(16.sdp()))
        Text(stringResource(R.string.user_in_contact), color = grayscale2)
        Spacer(modifier = Modifier.height(16.sdp()))
        LazyColumn {
            item {
                BannedUserItem(
                    Modifier.padding(vertical = 8.sdp()),
                    com.clearkeep.domain.model.User("", "Alex Mendes", "", "", "", "", "", "")
                ) {}
                BannedUserItem(
                    Modifier.padding(vertical = 8.sdp()),
                    com.clearkeep.domain.model.User("", "Alissa Baker", "", "", "", "", "", "")
                ) {}
                BannedUserItem(
                    Modifier.padding(vertical = 8.sdp()),
                    com.clearkeep.domain.model.User("", "Barbara Johnson", "", "", "", "", "", "")
                ) {}
            }
        }
    }
}

@Composable
fun HeaderBannedUser(onCloseView: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Spacer(Modifier.size(32.sdp()))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chev_left),
                contentDescription = null,
                modifier = Modifier
                    .clickable {
                        onCloseView.invoke()
                    },
                tint = grayscale1,
            )
        }
        Spacer(modifier = Modifier.size(16.sdp()))
        CKHeaderText("Banned User", headerTextType = HeaderTextType.Medium)
    }
}

@Composable
fun BannedUserItem(modifier: Modifier = Modifier, user: com.clearkeep.domain.model.User, onAction: (user: com.clearkeep.domain.model.User) -> Unit) {
    NewFriendListItem(modifier,
        user,
        { StatusText(user) },
        {
            CKButton(
                "Unbanned",
                { onAction(user) },
                Modifier.width(123.sdp()),
                buttonType = ButtonType.BorderGradient
            )
        }
    )
}
