package dev.hydranet.worldtime

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.drawable.BitmapDrawable
import android.graphics.Matrix
import android.os.Bundle
import android.util.SizeF
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.TextView
import android.view.View.MeasureSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class WorldDayNightWidgetTest {
    @Test
    fun launcherCanBindRenderAndResizeMapWidget() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 20261021)
        val provider = WorldDayNightWidgetProvider()
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        try {
            val id = host.allocateAppWidgetId()
            assertTrue(manager.bindAppWidgetIdIfAllowed(id,
                ComponentName(context, WorldDayNightWidgetProvider::class.java)))
            val info = manager.getAppWidgetInfo(id)
            assertEquals(context.getString(R.string.day_night_widget_name), info.loadLabel(context.packageManager))
            assertTrue(info.resizeMode and 3 == 3)

            val sizes = arrayListOf(SizeF(180f, 40f), SizeF(180f, 64f), SizeF(320f, 96f), SizeF(320f, 240f), SizeF(600f, 300f))
            val options = Bundle().apply {
                putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes)
            }
            manager.updateAppWidgetOptions(id, options)
            provider.onAppWidgetOptionsChanged(context, manager, id, options)
            instrumentation.runOnMainSync {
                val view = host.createView(context, id, info)
                for (size in sizes) {
                    val density = context.resources.displayMetrics.density
                    view.measure(
                        MeasureSpec.makeMeasureSpec((size.width * density).roundToInt() +
                            view.paddingLeft + view.paddingRight, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec((size.height * density).roundToInt() +
                            view.paddingTop + view.paddingBottom, MeasureSpec.EXACTLY),
                    )
                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                    val map = view.findViewById<ImageView>(R.id.day_night_widget_map)
                    assertNotNull(map)
                    val drawable = map.drawable as BitmapDrawable
                    val bitmap = drawable.bitmap
                    assertEquals(bitmap.width / 2, bitmap.height)
                    assertTrue(map.contentDescription.contains("UTC"))
                    assertTrue(view.findViewById<TextView>(R.id.day_night_widget_time).text.endsWith("UTC"))
                    assertTrue(map.width > 0 && map.height > 0)
                    val matrix = FloatArray(9)
                    map.imageMatrix.getValues(matrix)
                    val bitmapScale = matrix[Matrix.MSCALE_X] * drawable.intrinsicWidth / bitmap.width
                    val iconSizeDp = 17f * bitmap.density / DisplayMetrics.DENSITY_DEFAULT * bitmapScale / density
                    assertEquals("Sun and Moon icons must retain their app size at $size", 17f, iconSizeDp, 0.15f)
                }
            }
        } finally {
            host.deleteHost()
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
