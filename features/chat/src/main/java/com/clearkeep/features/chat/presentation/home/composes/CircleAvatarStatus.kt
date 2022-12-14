package com.clearkeep.features.chat.presentation.home.composes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.clearkeep.common.presentation.components.*
import com.clearkeep.common.presentation.components.base.CKText
import com.clearkeep.domain.model.UserStatus
import com.clearkeep.common.utilities.sdp

@Composable
fun CircleAvatarStatus(
    url: String?,
    name: String,
    size: Dp = 24.sdp(),
    status: String,
    sizeIndicator: Dp = 8.sdp()
) {
    val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name
    val color = when (status) {
        UserStatus.ONLINE.value -> {
            colorSuccessDefault
        }
        UserStatus.OFFLINE.value -> {
            grayscale3
        }
        UserStatus.BUSY.value -> {
            errorDefault
        }
        else -> {
            grayscale3
        }
    }
    Box(
        modifier = Modifier
            .size(size)
    ) {
        Column(Modifier.size(size)) {
            if (!url.isNullOrEmpty()) {
                Image(
                    rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(
                        "$url" //Force reload when cache key changes
                    ).apply(block = fun ImageRequest.Builder.() {
                        memoryCachePolicy(CachePolicy.DISABLED)
                    }).build()
                    ),
                    null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
            } else
                Column(
                    modifier = Modifier
                        .background(
                            shape = CircleShape,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    backgroundGradientStart,
                                    backgroundGradientEnd
                                )
                            )
                        )
                        .size(size),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CKText(
                        displayName.capitalize(), style = MaterialTheme.typography.caption.copy(
                            color = MaterialTheme.colors.onSurface,
                        )
                    )
                }
        }
        StatusIndicator(color, sizeIndicator)
    }

}

@Composable
fun BoxScope.StatusIndicator(color: Color, size: Dp = 8.sdp()) {
    Box(
        Modifier
            .size(size)
            .background(color, CircleShape)
            .align(Alignment.BottomEnd)
    )
}