package com.eyecare.lookaway.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.eyecare.lookaway.R
import com.eyecare.lookaway.service.ReminderEngine
import com.eyecare.lookaway.service.ReminderScheduler
import com.eyecare.lookaway.service.ReminderService
import com.eyecare.lookaway.service.RunState

/**
 * Quick Settings tile: one tap starts reminders (when off) or toggles pause
 * (when on). The running service refreshes the tile via
 * [TileService.requestListeningState].
 */
class PauseTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (RunState.isEnabled(this)) {
            ReminderScheduler.sendAction(this, ReminderService.ACTION_TOGGLE)
        } else {
            ReminderScheduler.startReminders(this)
        }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val enabled = RunState.isEnabled(this)
        val paused = ReminderEngine.state.value.paused

        tile.state = if (enabled && !paused) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    !enabled -> R.string.tile_off
                    paused -> R.string.tile_paused
                    else -> R.string.tile_running
                },
            )
        }
        tile.updateTile()
    }
}
