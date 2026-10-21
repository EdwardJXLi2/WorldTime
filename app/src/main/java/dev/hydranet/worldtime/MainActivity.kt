package dev.hydranet.worldtime

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        WorldTimeWidgetProvider.refreshAll(this)
        WorldDayNightWidgetProvider.refreshAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WorldTimeTheme {
                val sharedView = remember {
                    if (intent.action == Intent.ACTION_VIEW) WorldTimeShare.parse(intent.data) else null
                }
                var zones by remember {
                    mutableStateOf(sharedView?.zones ?: WorldTimeData.loadZones(this))
                }
                var homeZone by remember {
                    val saved = sharedView?.homeZone ?: WorldTimeData.loadHomeZone(this)
                    mutableStateOf(saved.takeIf { it in zones } ?: zones.first())
                }
                var selectedMillis by rememberSaveable {
                    mutableLongStateOf(sharedView?.instant?.toEpochMilli() ?: System.currentTimeMillis())
                }
                var followingNow by rememberSaveable { mutableStateOf(sharedView == null) }
                val selected = Instant.ofEpochMilli(selectedMillis)

                LaunchedEffect(followingNow) {
                    while (followingNow) {
                        val now = System.currentTimeMillis()
                        selectedMillis = now
                        delay(60_000L - now % 60_000L)
                    }
                }

                WorldTimeScreen(
                    zones = zones,
                    homeZone = homeZone,
                    selected = selected,
                    followingNow = followingNow,
                    onSelectTime = { instant ->
                        selectedMillis = instant.toEpochMilli()
                        followingNow = false
                    },
                    onNow = {
                        selectedMillis = System.currentTimeMillis()
                        followingNow = true
                    },
                    onZonesChange = { updated ->
                        if (updated.isNotEmpty()) {
                            zones = updated
                            WorldTimeData.saveZones(this, updated)
                            if (homeZone !in updated) {
                                homeZone = updated.first()
                                WorldTimeData.saveHomeZone(this, homeZone)
                            }
                        }
                    },
                    onHomeZoneChange = { zone ->
                        homeZone = zone
                        WorldTimeData.saveHomeZone(this, zone)
                    },
                    onShare = {
                        val summary = WorldTimeShare.message(
                            selected, homeZone, zones, DateFormat.is24HourFormat(this),
                        )
                        startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summary)
                            },
                            "Share this view",
                        ))
                    },
                )
            }
        }
    }
}
