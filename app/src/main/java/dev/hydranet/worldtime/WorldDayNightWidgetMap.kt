package dev.hydranet.worldtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.DisplayMetrics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.res.vectorResource
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import java.time.Instant
import java.time.ZoneId
import kotlin.math.hypot
import kotlin.math.roundToInt

internal fun renderDayNightMap(
    context: Context, width: Int, density: Float, now: Instant, home: ZoneId,
    homeLocation: MapLocation?, colors: ColorScheme,
): Bitmap {
    val bitmap = createBitmap(width, width / 2).apply {
        this.density = (density * DisplayMetrics.DENSITY_DEFAULT).roundToInt()
    }
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val light = colors.surface.luminance() > 0.5f
    val night = if (light) colors.inverseSurface else colors.surfaceDim
    val ocean = if (light) lerp(colors.secondaryContainer, night, 0.22f) else colors.secondaryContainer
    paint.color = ocean.toArgb()
    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), bitmap.height.toFloat()),
        8f * density, 8f * density, paint)
    val clip = android.graphics.Path().apply {
        addRoundRect(RectF(0f, 0f, width.toFloat(), bitmap.height.toFloat()),
            8f * density, 8f * density, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(clip)

    fun drawLayer(resource: Int, color: Color) {
        val drawable = requireNotNull(context.getDrawable(resource)).mutate()
        drawable.setTint(color.toArgb())
        drawable.setBounds(0, 0, width, bitmap.height)
        drawable.draw(canvas)
    }

    drawLayer(R.drawable.world_map_land, lerp(colors.primary, night, 0.18f))
    val sun = SolarPosition.at(now)
    canvas.save()
    canvas.scale(width / 360f, bitmap.height / 180f)
    paint.color = night.copy(alpha = 0.32f).toArgb()
    for (altitude in listOf(0.0, -6.0, -12.0)) {
        val path = nightPath(sun, altitude).asAndroidPath()
        for (wrap in -1..1) {
            canvas.withTranslation(wrap * 360f, 0f) {
                drawPath(path, paint)
            }
        }
    }
    val regions = ImageVector.vectorResource(res = context.resources, resId = R.drawable.world_map_zone_regions)
    val offset = home.rules.getStandardOffset(now).totalSeconds / 60
    paint.color = colors.tertiary.copy(alpha = if (light) 0.22f else 0.18f).toArgb()
    canvas.drawPath(homeRegionPath(regions, offset).asAndroidPath(), paint)
    canvas.restore()
    drawLayer(R.drawable.world_map_time_zones, colors.outline)

    fun point(location: MapLocation): Pair<Float, Float> =
        ((location.longitude + 180.0) / 360.0 * width).toFloat() to
            ((90.0 - location.latitude) / 180.0 * bitmap.height).toFloat()

    fun marker(
        anchor: Pair<Float, Float>, icon: ImageVector, foreground: Color, offsetY: Float = 0f,
    ): Pair<Float, Float> {
        val radius = 11f * density
        val x = anchor.first.coerceIn(radius, width - radius)
        val y = (anchor.second + offsetY).coerceIn(radius, bitmap.height - radius)
        paint.color = foreground.toArgb()
        if (hypot(x - anchor.first, y - anchor.second) > 1f) {
            paint.strokeWidth = density
            canvas.drawLine(anchor.first, anchor.second, x, y, paint)
            canvas.drawCircle(anchor.first, anchor.second, 2f * density, paint)
        }
        paint.color = colors.surfaceContainerHighest.copy(alpha = 0.92f).toArgb()
        canvas.drawCircle(x, y, radius, paint)
        canvas.withSave {
            val iconSize = 17f * density
            translate(x - iconSize / 2f, y - iconSize / 2f)
            scale(iconSize / icon.viewportWidth, iconSize / icon.viewportHeight)
            paint.color = foreground.toArgb()
            for (index in 0 until icon.root.size) {
                val path = icon.root[index] as VectorPath
                drawPath(PathParser().addPathNodes(path.pathData).toPath().asAndroidPath(), paint)
            }
        }
        return x to y
    }

    val sunCenter = marker(point(MapLocation(sun.latitude, sun.longitude)), Icons.Outlined.WbSunny, colors.tertiary)
    val moonAnchor = point(LunarPosition.at(now))
    val moonOffset = if (hypot(moonAnchor.first - sunCenter.first, moonAnchor.second - sunCenter.second) < 24f * density) {
        if (moonAnchor.second + 36f * density < bitmap.height) 24f * density else -24f * density
    } else 0f
    marker(moonAnchor, Icons.Filled.NightlightRound, colors.primary, moonOffset)
    if (homeLocation != null) {
        val (x, y) = point(homeLocation)
        paint.color = colors.surfaceContainerHighest.toArgb()
        canvas.drawCircle(x, y, 4.5f * density, paint)
        paint.color = colors.primary.toArgb()
        canvas.drawCircle(x, y, 2.5f * density, paint)
    }
    return bitmap
}
