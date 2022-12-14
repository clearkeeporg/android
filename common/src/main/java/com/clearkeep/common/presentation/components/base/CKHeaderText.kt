package com.clearkeep.common.presentation.components.base

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.clearkeep.common.presentation.components.LocalColorMapping
import com.clearkeep.common.utilities.sdp
import com.clearkeep.common.utilities.toNonScalableTextSize

@Composable
fun CKHeaderText(
    text: String,
    modifier: Modifier = Modifier,
    headerTextType: HeaderTextType = HeaderTextType.Normal,
    color: Color = LocalColorMapping.current.headerText
) {
    Text(
        text = text,
        modifier = modifier,
        style = getTypography(headerTextType).copy(
            color = color,
        ),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun getTypography(headerTextType: HeaderTextType): TextStyle {
    return when (headerTextType) {
        HeaderTextType.Normal -> MaterialTheme.typography.h6.copy(
            fontSize = 16.sdp().toNonScalableTextSize()
        )
        HeaderTextType.Medium -> MaterialTheme.typography.h5.copy(
            fontSize = 20.sdp().toNonScalableTextSize()
        )
        HeaderTextType.Large -> MaterialTheme.typography.h4.copy(
            fontSize = 24.sdp().toNonScalableTextSize()
        )
    }
}

enum class HeaderTextType {
    Normal,
    Medium,
    Large
}