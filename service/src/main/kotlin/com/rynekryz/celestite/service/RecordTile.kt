package com.rynekryz.celestite.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.rynekryz.celestite.core.model.RecordingBus
import com.rynekryz.celestite.core.model.RecordingState

class RecordTile : TileService() {

    override fun onStartListening() {
        update()
    }

    override fun onClick() {
        super.onClick()
        val state = RecordingBus.state.value
        if (state.isActive) {
            CelestiteService.stop(this)
        } else {
            unlockAndRun {
                val intent = Intent(RecordLauncher.ACTION)
                    .setPackage(packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(packageManager) != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val pending = PendingIntent.getActivity(
                            this,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                        startActivityAndCollapse(pending)
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(intent)
                    }
                }
            }
        }
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val state = RecordingBus.state.value
        tile.subtitle = when (state) {
            is RecordingState.Counting -> getString(R.string.notif_countdown, state.secondsLeft)
            is RecordingState.Recording -> getString(R.string.notif_recording)
            is RecordingState.Paused -> getString(R.string.notif_paused)
            else -> getString(R.string.tile_idle)
        }
        tile.state = if (state.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}

internal object RecordLauncher {
    const val ACTION = "com.rynekryz.celestite.action.RECORD_LAUNCHER"
}
