package com.clearkeep.features.chat.presentation.composes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.base.ButtonType
import com.clearkeep.common.presentation.components.base.CKButton
import com.clearkeep.common.presentation.components.colorSuccessDefault
import com.clearkeep.domain.model.User
import com.clearkeep.common.utilities.sdp

@Composable
fun FriendListItemAction(
    actionLabel: String,
    friend: User,
    onAction: (profile: User) -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable { onAction(friend) }
            .padding(vertical = 8.sdp())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                emptyList(),
                friend.userName,
                size = 64.sdp()
            )
            Column(
                Modifier
                    .padding(start = 16.sdp())
                    .weight(1.0f, true)
            ) {
                Text(
                    text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    ), overflow = TextOverflow.Ellipsis, maxLines = 1
                )
                Text(stringResource(R.string.online), color = colorSuccessDefault)
            }
            Spacer(Modifier.width(16.sdp()))
            CKButton(
                actionLabel,
                {},
                Modifier.width(123.sdp()),
                buttonType = ButtonType.BorderGradient
            )
        }
    }
}