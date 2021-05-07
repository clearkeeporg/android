package com.clearkeep.components.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.clearkeep.components.grayscaleBlack
import com.clearkeep.components.grayscaleOffWhite

@Composable
fun CKHeaderText(
    text: String,
    modifier: Modifier = Modifier,
    headerTextType: HeaderTextType = HeaderTextType.Normal,
    color: Color = grayscaleBlack
) {
    Text(
        text = text,
        modifier = modifier,
        style = getTypography(headerTextType).copy(
            color = if (isSystemInDarkTheme()) grayscaleOffWhite else color
        )
    )
}



@Composable
fun getTypography(headerTextType: HeaderTextType): TextStyle {
    return when (headerTextType) {
        HeaderTextType.Normal -> MaterialTheme.typography.h6
        HeaderTextType.Medium -> MaterialTheme.typography.h5
        HeaderTextType.Large -> MaterialTheme.typography.h4
    }
}

enum class HeaderTextType {
    Normal,
    Medium,
    Large
}