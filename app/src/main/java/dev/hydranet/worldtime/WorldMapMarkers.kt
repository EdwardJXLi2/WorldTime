package dev.hydranet.worldtime

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

internal fun DrawScope.mapPoint(location: MapLocation): Offset = Offset(
    ((location.longitude + 180.0) / 360.0 * size.width).toFloat(),
    ((90.0 - location.latitude) / 180.0 * size.height).toFloat(),
)

internal fun DrawScope.drawCelestialMarker(
    anchor: Offset,
    icon: Painter,
    foreground: Color,
    background: Color,
    offsetY: Float = 0f,
): Offset {
    val radius = 11.dp.toPx()
    val center = Offset(
        anchor.x.coerceIn(radius, size.width - radius),
        (anchor.y + offsetY).coerceIn(radius, size.height - radius),
    )
    // Keep symbols visible at map edges and near conjunction, with a line to the actual position.
    if ((center - anchor).getDistance() > 1f) {
        drawLine(foreground, anchor, center, strokeWidth = 1.dp.toPx())
        drawCircle(foreground, radius = 2.dp.toPx(), center = anchor)
    }
    drawCircle(background.copy(alpha = 0.92f), radius = radius, center = center)
    val iconSize = 17.dp.toPx()
    translate(center.x - iconSize / 2f, center.y - iconSize / 2f) {
        with(icon) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(foreground)) }
    }
    return center
}

