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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.clearkeep.features.chat.R
import com.clearkeep.common.presentation.components.backgroundGradientEnd
import com.clearkeep.common.presentation.components.backgroundGradientStart
import com.clearkeep.common.presentation.components.base.CKText

@Composable
fun CircleAvatarSite(
    url: String?,
    name: String,
    size: Dp = dimensionResource(R.dimen._56sdp),
    status: String,
    cacheKey: String = ""
) {
    val displayName = if (name.isNotBlank() && name.length >= 2) name.substring(0, 1) else name

    Column(Modifier.size(size)) {
        if (!url.isNullOrEmpty()) {
            Image(
                rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(
                    "$url?cache=$cacheKey" //Force reload when cache key changes
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
        } else {
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
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen._10sdp))
                    .background(
                        shape = CircleShape,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Red
                            )
                        )
                    )
            )
        }
    }
}
