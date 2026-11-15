package com.rynekryz.celestite.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rynekryz.celestite.core.datastore.CelestiteStoreHolder
import com.rynekryz.celestite.core.model.AudioMode
import com.rynekryz.celestite.core.model.RecordingBus
import com.rynekryz.celestite.core.model.RecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CelestiteService : Service() {

    companion object {
        const val ACTION_START = "com.rynekryz.celestite.action.START"
        const val ACTION_STOP = "com.rynekryz.celestite.action.STOP"
        const val ACTION_PAUSE = "com.rynekryz.celestite.action.PAUSE"
        const val ACTION_RESUME = "com.rynekryz.celestite.action.RESUME"

        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "celestite_recording"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "Celestite"

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, CelestiteService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CelestiteService::class.java).setAction(ACTION_STOP))
        }

        fun pause(context: Context) {
            context.startService(Intent(context, CelestiteService::class.java).setAction(ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(Intent(context, CelestiteService::class.java).setAction(ACTION_RESUME))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var projection: MediaProjection? = null
    private var pipeline: CapturePipeline? = null
    private var countdownJob: Job? = null
    private val countdownOverlay = CountdownOverlay(this)
    private val floatingControls = FloatingControls(
        this,
        onPause = { pause(this) },
        onResume = { resume(this) },
        onStop = { stop(this) },
    )
    private var startedAtMs = 0L
    private var elapsedBeforePause = 0L
    private var overlayEnabled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> scope.launch { stopRecording() }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        if (RecordingBus.state.value.isActive || projection != null) return
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        } ?: return
        val manager = getSystemService(MediaProjectionManager::class.java) ?: return

        val notification = buildNotification(getString(R.string.notif_preparing), null)
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        try {
            projection = manager.getMediaProjection(resultCode, resultData)?.also {
                it.registerCallback(projectionCallback, handler)
            } ?: error("No screen capture access")
        } catch (t: Throwable) {
            Log.w(TAG, "projection failed", t)
            fail(t.message ?: "Screen capture was denied")
            return
        }
        Log.d(TAG, "projection ok")

        countdownJob = scope.launch {
            val store = CelestiteStoreHolder.get(applicationContext)
            overlayEnabled = store.overlayControls.first()
            val countdown = store.current().countdownSeconds
            if (countdown > 0) {
                val showVisual = canShowOverlay(applicationContext)
                for (left in countdown downTo 1) {
                    RecordingBus.post(RecordingState.Counting(left))
                    if (showVisual) {
                        withContext(Dispatchers.Main) {
                            countdownOverlay.show()
                            countdownOverlay.update(left.toString())
                        }
                    }
                    delay(1000)
                }
                if (showVisual) {
                    withContext(Dispatchers.Main) { countdownOverlay.hide() }
                }
            }
            startCapture()
        }
    }

    private suspend fun startCapture() {
        if (projection == null || pipeline != null) return
        try {
            val settings = CelestiteStoreHolder.get(applicationContext).current()
            val size = CaptureMath.targetSize(displayBounds(this), settings.resolution)
            val fps = CaptureMath.fps(displayRefreshRate(this), settings.fps)
            val bitrate = CaptureMath.bitrate(size.first, size.second, fps, settings.quality, settings.format)
            val audioMode = if (settings.audioMode == AudioMode.MIC && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                AudioMode.NONE
            } else {
                settings.audioMode
            }
            startedAtMs = SystemClock.elapsedRealtime()
            elapsedBeforePause = 0L
            val created: CapturePipeline = if (audioMode == AudioMode.DEVICE) {
                CodecPipeline(this, projection!!, size.first, size.second, fps, bitrate, settings.format).also { p ->
                    p.onFailed = { message -> fail(message) }
                    p.start()
                }
            } else {
                MediaRecorderPipeline(this, projection!!, size.first, size.second, fps, bitrate, settings.format, audioMode).also { p ->
                    p.start()
                }
            }
            pipeline = created
            val supportsPause = created.supportsPause
            RecordingBus.post(RecordingState.Recording(startedAtMs))
            updateNotification(getString(R.string.notif_recording), supportsPause)
            if (overlayEnabled && canShowOverlay(this)) {
                handler.post { floatingControls.show(withPause = supportsPause) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "capture failed", t)
            fail(t.message ?: "Could not start recording")
        }
    }

    private fun pauseRecording() {
        val current = pipeline ?: return
        runCatching { current.pause() }
        elapsedBeforePause += SystemClock.elapsedRealtime() - startedAtMs
        RecordingBus.post(RecordingState.Paused(elapsedBeforePause))
        updateNotification(getString(R.string.notif_paused), true)
        if (overlayEnabled) handler.post { floatingControls.setPaused(true) }
    }

    private fun resumeRecording() {
        val current = pipeline ?: return
        runCatching { current.resume() }
        startedAtMs = SystemClock.elapsedRealtime()
        RecordingBus.post(RecordingState.Recording(startedAtMs))
        updateNotification(getString(R.string.notif_recording), true)
        if (overlayEnabled) handler.post { floatingControls.setPaused(false) }
    }

    private suspend fun stopRecording(markSaved: Boolean = true) {
        countdownJob?.cancel()
        countdownJob = null
        val current = pipeline
        pipeline = null
        var savedName: String? = null
        if (current != null) {
            withContext(Dispatchers.Main) { floatingControls.hide() }
            savedName = try {
                withContext(Dispatchers.Default) { current.stop() }
            } catch (t: Throwable) {
                null
            }
            if (savedName == null) {
                withContext(Dispatchers.Default) { current.abort() }
            }
        }
        projection?.stop()
        projection = null
        withContext(Dispatchers.Main) { countdownOverlay.hide() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (markSaved && savedName != null) {
            RecordingBus.markSaved(savedName)
        } else {
            RecordingBus.post(RecordingState.Idle)
        }
        stopSelf()
    }

    private fun fail(message: String) {
        Log.w(TAG, "fail: $message")
        RecordingBus.post(RecordingState.Failed(message))
        scope.launch { stopRecording(markSaved = false) }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            projection = null
            scope.launch { stopRecording(markSaved = true) }
        }
    }

    private fun updateNotification(text: String, withPause: Boolean?) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text, withPause))
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownOverlay.hide()
        floatingControls.hide()
        scope.cancel()
    }

    private fun buildNotification(text: String, withPause: Boolean?): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, CelestiteService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record_glyph)
            .setContentTitle("Celestite")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .addAction(0, getString(R.string.action_stop), stopIntent)
        if (withPause == true) {
            val pauseIntent = PendingIntent.getService(
                this, 2,
                Intent(this, CelestiteService::class.java).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val resumeIntent = PendingIntent.getService(
                this, 3,
                Intent(this, CelestiteService::class.java).setAction(ACTION_RESUME),
                PendingIntent.FLAG_IMMUTABLE,
            )
            if (RecordingBus.state.value is RecordingState.Paused) {
                builder.addAction(0, getString(R.string.action_resume), resumeIntent)
            } else {
                builder.addAction(0, getString(R.string.action_pause), pauseIntent)
            }
        }
        return builder.build()
    }
}
