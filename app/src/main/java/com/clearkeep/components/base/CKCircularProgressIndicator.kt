package com.clearkeep.components.base

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun CKCircularProgressIndicator(
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colors.surface,
        strokeWidth: Dp = ProgressIndicatorConstants.DefaultStrokeWidth
) {
    CircularProgressIndicator(
            modifier = modifier,
            color = color,
            strokeWidth = strokeWidth
    )
}