package com.eyecare.lookaway.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.eyecare.lookaway.tile.PauseTileService
import com.eyecare.lookaway.widget.LookAwayWidgetProvider

/** Keeps the home-screen widget and Quick Settings tile in sync with state. */
object ExternalControls {

    /** Cheap; call on every tick to keep the widget countdown live. */
    fun refreshWidget(context: Context) {
        runCatching { LookAwayWidgetProvider.refresh(context) }
    }

    /** Call on coarse state changes (start/stop/pause) to update both surfaces. */
    fun refreshAll(context: Context) {
        refreshWidget(context)
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, PauseTileService::class.java),
            )
        }
    }
}
