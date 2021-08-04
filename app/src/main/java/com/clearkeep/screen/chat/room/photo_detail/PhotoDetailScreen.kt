package com.clearkeep.screen.chat.room.photo_detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.imageLoader
import com.clearkeep.components.colorDialogScrim
import com.clearkeep.components.colorLightBlue
import com.clearkeep.screen.chat.room.RoomViewModel
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun PhotoDetailScreen(roomViewModel: RoomViewModel, onDismiss: () -> Unit) {
    val systemUiController = rememberSystemUiController()
    val imagesList = roomViewModel.imageDetailList.observeAsState()
    val senderName = roomViewModel.imageDetailSenderName.observeAsState()
    val selectedImageUri = remember { mutableStateOf("") }
    val isShareDialogOpen = remember { mutableStateOf(false) }
    val context = LocalContext.current
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }

    if (!imagesList.value.isNullOrEmpty() && selectedImageUri.value.isEmpty()) {
        selectedImageUri.value = imagesList.value!![0]
    }

    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        val (imageId, imageListId) = createRefs()
        Box(
            Modifier
                .fillMaxHeight(0.875f)
                .constrainAs(imageId) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(imageListId.top)
                }) {
            IconButton(
                onClick = { onDismiss.invoke() },
                Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = Color.White
                )
            }
            Text(senderName.value ?: "", Modifier.align(Alignment.TopCenter).padding(top = 12.dp), color = Color.White)
            Image(
                rememberCoilPainter(selectedImageUri.value, context.imageLoader),
                null,
                Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = {
                    isShareDialogOpen.value = true
                }, Modifier
                    .align(Alignment.BottomStart)
                    .wrapContentSize()
            ) {
                Icon(
                    Icons.Default.IosShare,
                    null,
                    tint = Color.White
                )
            }
        }
        BottomImageList(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .constrainAs(imageListId) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            imagesList.value ?: emptyList(),
            selectedImageUri.value,
            onSelect = {
                selectedImageUri.value = it
            }
        )
        ShareImageDialog(isShareDialogOpen.value, onDismiss = {
            isShareDialogOpen.value = false
        }, onClickSave = {
            roomViewModel.downloadFile(context, selectedImageUri.value)
            isShareDialogOpen.value = false
        })
    }
}

@Composable
fun SelectableImageItem(
    modifier: Modifier = Modifier,
    uri: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier.then(
            Modifier
                .clip(RectangleShape)
                .aspectRatio(1f)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onSelect.invoke(uri)
                }
                .then(if (isSelected) Modifier.border(1.dp, Color.White) else Modifier)
        )
    ) {
        Image(
            rememberCoilPainter(uri, context.imageLoader),
            null,
            contentScale = ContentScale.Crop,
            modifier = modifier.then(Modifier.align(Alignment.Center)),
        )
    }
}

@Composable
fun ShareImageDialog(isOpen: Boolean, onDismiss: () -> Unit, onClickSave: () -> Unit) {
    if (isOpen) {
        Box {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colorDialogScrim)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        onDismiss()
                    })
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                ) {
                    Text(
                        "Save",
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                onClickSave.invoke()
                            },
                        textAlign = TextAlign.Center, color = colorLightBlue
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box {
                    Text(
                        "Cancel", modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                onDismiss()
                            }, textAlign = TextAlign.Center, color = colorLightBlue
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun BottomImageList(
    modifier: Modifier = Modifier,
    imagesList: List<String>,
    selectedImageUri: String,
    onSelect: (String) -> Unit
) {
    println("imagesList $imagesList")
    LazyRow(modifier) {
        itemsIndexed(imagesList) { _, uri: String ->
            val isSelected = uri == selectedImageUri
            SelectableImageItem(Modifier.padding(horizontal = 2.dp), uri, isSelected, onSelect)
        }
    }
}