package com.clearkeep.features.calls.presentation.surfacegenerator.impl

import android.content.Context
import com.clearkeep.features.calls.presentation.surfacegenerator.SurfacePosition

class FiveSurfaceGenerator(context: Context, width: Int, height: Int) : SurfaceGeneratorImpl(
    width,
    height
) {
    override fun getLocalSurface(): SurfacePosition {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val aThirdHeightPx = heightPx / 3
        val halfWidthPx = widthPx / 2
        return SurfacePosition(
            halfWidthPx,
            heightPx - 2 * aThirdHeightPx,
            widthPx - halfWidthPx,
            2 * aThirdHeightPx
        )
    }

    override fun getRemoteSurfaces(): List<SurfacePosition> {
        val heightPx = getHeight()
        val widthPx = getWidth()
        val aThirdHeightPx = heightPx / 3
        val halfWidthPx = widthPx / 2

        return listOf(
            SurfacePosition(
                widthPx,
                aThirdHeightPx,
                0,
                0
            ),
            SurfacePosition(
                halfWidthPx,
                aThirdHeightPx,
                0,
                aThirdHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                aThirdHeightPx,
                widthPx - halfWidthPx,
                aThirdHeightPx
            ),
            SurfacePosition(
                halfWidthPx,
                heightPx - 2 * aThirdHeightPx,
                0,
                2 * aThirdHeightPx
            ),
        )
    }
}