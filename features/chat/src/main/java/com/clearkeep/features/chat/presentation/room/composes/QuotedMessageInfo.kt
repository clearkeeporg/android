package com.clearkeep.features.chat.presentation.room.composes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.presentation.components.base.CKText
import com.clearkeep.common.presentation.components.grayscale2
import com.clearkeep.common.presentation.components.grayscale3
import com.clearkeep.common.utilities.*
import com.clearkeep.features.chat.presentation.room.messagedisplaygenerator.MessageDisplayInfo

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QuotedMessageView(message: MessageDisplayInfo, onClick :()-> Unit) {
    val arrangement = if (message.isOwner) Arrangement.End else Arrangement.Start
    Row(Modifier.fillMaxWidth(), arrangement) {
        if (message.isOwner) {
            Spacer(Modifier.width(18.sdp()))
        }
        Card(onClick = {
            onClick()
        }) {
            ConstraintLayout(Modifier.background(LocalColorMapping.current.quoteMessageBackground)) {
                val (messageContent, quotedMessageIndicator, quotedMessageInfo) = createRefs()

                Column(Modifier.constrainAs(messageContent) {
                    if (message.isOwner) {
                        end.linkTo(parent.end)
                    } else {
                        start.linkTo(parent.start)
                    }
                    top.linkTo(parent.top)
                }) {
                    val quotedMessage = message.quotedMessage

                    if (isImageMessage(quotedMessage)) {
                        ImageMessageContent(
                            Modifier.padding(24.sdp(), 16.sdp()),
                            getImageUriStrings(quotedMessage),
                            true
                        ) {

                        }
                    } else if (isFileMessage(quotedMessage)) {
                        FileMessageContent(getFileUriStrings(quotedMessage), isQuote = true) {
                        }
                    }
                    val messageContent = getMessageContent(quotedMessage)
                    if (messageContent.isNotBlank()) {
                        Row(
                            Modifier
                                .wrapContentHeight()
                        ) {
                            ClickableLinkContent(
                                messageContent,
                                true,
                                quotedMessage.hashCode()
                            ) {
                                Log.d("--- Quote message", "Touched quote message")
                            }
                        }
                    }
                }

                val indicatorWidth = 4.sdp()
                Box(
                    Modifier
                        .fillMaxHeight()
                        .background(
                            grayscale2
                        )
                        .constrainAs(quotedMessageIndicator) {
                            if (message.isOwner) {
                                end.linkTo(parent.end)
                            } else {
                                start.linkTo(parent.start)
                            }
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            height = Dimension.fillToConstraints
                            width = Dimension.value(indicatorWidth)
                        }
                )

                QuotedMessageInfo(Modifier.constrainAs(quotedMessageInfo) {
                    top.linkTo(messageContent.bottom)
                    if (message.isOwner) {
                        end.linkTo(parent.end)
                    } else {
                        start.linkTo(parent.start)
                    }
                }, message)
            }
        }
        if (!message.isOwner) {
            Spacer(Modifier.width(18.sdp()))
        }
    }
}

@Composable
private fun QuotedMessageInfo(modifier: Modifier, message: MessageDisplayInfo) {
    Row(modifier) {
        val time = getHourTimeAsString(message.quotedTimestamp)
        val date = getDateAsString(message.quotedTimestamp)

        Spacer(Modifier.width(20.sdp()))
        CKText("${message.quotedUser} $time $date", color = grayscale3, fontWeight = FontWeight.W400)
        Spacer(Modifier.width(20.sdp()))
    }
}