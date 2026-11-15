package com.rynekryz.celestite.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rynekryz.celestite.core.model.AudioMode
import com.rynekryz.celestite.core.model.CelestiteSettings
import com.rynekryz.celestite.core.model.FpsOption
import com.rynekryz.celestite.core.model.QualityOption
import com.rynekryz.celestite.core.model.ResolutionOption
import com.rynekryz.celestite.core.model.VideoFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "celestite")

private fun <T : Enum<T>> String?.toEnum(default: T): T =
    try {
        java.lang.Enum.valueOf(default.getEnumClass(), this) as T
    } catch (_: Exception) {
        default
    }

@Suppress("UNCHECKED_CAST")
private fun <T : Enum<T>> T.getEnumClass(): Class<T> = javaClass as Class<T>

class CelestiteStore(private val context: Context) {
    private val store get() = context.dataStore

    val settings: Flow<CelestiteSettings> = store.data.map { p ->
        CelestiteSettings(
            audioMode = p[AUDIO].toEnum(AudioMode.MIC),
            format = p[FORMAT].toEnum(VideoFormat.H264),
            resolution = p[RESOLUTION].toEnum(ResolutionOption.NATIVE),
            fps = p[FPS].toEnum(FpsOption.AUTO),
            quality = p[QUALITY].toEnum(QualityOption.AUTO),
            countdownSeconds = p[COUNTDOWN] ?: 3,
        )
    }

    suspend fun current(): CelestiteSettings = settings.first()

    suspend fun setAudioMode(mode: AudioMode) = store.edit { it[AUDIO] = mode.name }
    suspend fun setFormat(format: VideoFormat) = store.edit { it[FORMAT] = format.name }
    suspend fun setResolution(resolution: ResolutionOption) = store.edit { it[RESOLUTION] = resolution.name }
    suspend fun setFps(fps: FpsOption) = store.edit { it[FPS] = fps.name }
    suspend fun setQuality(quality: QualityOption) = store.edit { it[QUALITY] = quality.name }
    suspend fun setCountdown(seconds: Int) = store.edit { it[COUNTDOWN] = seconds }

    val onboarded: Flow<Boolean> = store.data.map { it[ONBOARDED] ?: false }

    suspend fun setOnboarded() = store.edit { it[ONBOARDED] = true }

    val overlayControls: Flow<Boolean> = store.data.map { it[OVERLAY_CONTROLS] ?: false }

    suspend fun setOverlayControls(enabled: Boolean) = store.edit { it[OVERLAY_CONTROLS] = enabled }

    private companion object {
        val AUDIO = stringPreferencesKey("audio_mode")
        val FORMAT = stringPreferencesKey("video_format")
        val RESOLUTION = stringPreferencesKey("resolution")
        val FPS = stringPreferencesKey("fps")
        val QUALITY = stringPreferencesKey("quality")
        val COUNTDOWN = intPreferencesKey("countdown_seconds")
        val ONBOARDED = booleanPreferencesKey("oobe_done")
        val OVERLAY_CONTROLS = booleanPreferencesKey("overlay_controls")
    }
}

object CelestiteStoreHolder {
    @Volatile
    private var store: CelestiteStore? = null

    fun get(context: Context): CelestiteStore =
        store ?: synchronized(this) {
            store ?: CelestiteStore(context.applicationContext).also { store = it }
        }
}
