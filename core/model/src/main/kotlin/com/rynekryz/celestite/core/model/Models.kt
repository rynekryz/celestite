package com.rynekryz.celestite.core.model

enum class AudioMode { NONE, MIC, DEVICE }

enum class VideoFormat { H264, H265 }

enum class ResolutionOption { NATIVE, FHD, HD }

enum class FpsOption { AUTO, S60, S30 }

enum class QualityOption { AUTO, HIGH, MEDIUM, LOW }

data class CelestiteSettings(
    val audioMode: AudioMode = AudioMode.MIC,
    val format: VideoFormat = VideoFormat.H264,
    val resolution: ResolutionOption = ResolutionOption.NATIVE,
    val fps: FpsOption = FpsOption.AUTO,
    val quality: QualityOption = QualityOption.AUTO,
    val countdownSeconds: Int = 3,
) : java.io.Serializable

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Counting(val secondsLeft: Int) : RecordingState

    data class Recording(val startedAtMs: Long) : RecordingState

    data class Paused(val elapsedMs: Long) : RecordingState

    data class Failed(val message: String) : RecordingState

    val isActive: Boolean
        get() = this !is Idle
}

val RecordingState.isCapturing: Boolean
    get() = this is RecordingState.Recording || this is RecordingState.Paused
