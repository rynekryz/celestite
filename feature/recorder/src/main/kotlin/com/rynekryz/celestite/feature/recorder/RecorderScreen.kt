package com.rynekryz.celestite.feature.recorder

import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rynekryz.celestite.core.datastore.CelestiteStoreHolder
import com.rynekryz.celestite.core.designsystem.Haptics
import com.rynekryz.celestite.core.designsystem.Symbol
import com.rynekryz.celestite.core.designsystem.rememberHaptics
import com.rynekryz.celestite.core.model.CelestiteSettings
import com.rynekryz.celestite.core.model.FpsOption
import com.rynekryz.celestite.core.model.QualityOption
import com.rynekryz.celestite.core.model.RecordingBus
import com.rynekryz.celestite.core.model.RecordingState
import com.rynekryz.celestite.core.model.ResolutionOption
import com.rynekryz.celestite.core.model.AudioMode
import com.rynekryz.celestite.core.model.VideoFormat
import com.rynekryz.celestite.service.CelestiteService
import kotlinx.coroutines.delay

@Composable
fun RecorderScreen() {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val state by RecordingBus.state.collectAsState()
    val settings by CelestiteStoreHolder.get(context).settings.collectAsState(
        initial = CelestiteSettings(),
    )

    val ticking by produceState(0L, state) {
        if (state is RecordingState.Recording) {
            while (true) {
                value = SystemClock.elapsedRealtime()
                delay(500)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Symbol("screen_record", size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Celestite",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(56.dp))
        RecordButton(
            state,
            haptics,
            onOpenModal = {
                context.startActivity(Intent(context, RecordLauncherActivity::class.java))
            },
            onStop = { CelestiteService.stop(context) },
            onResume = { CelestiteService.resume(context) },
        )
        Spacer(Modifier.height(24.dp))
        val elapsedMs = when (val s = state) {
            is RecordingState.Recording -> ticking - s.startedAtMs
            is RecordingState.Paused -> s.elapsedMs
            else -> 0L
        }
        val statusText = when (val s = state) {
            RecordingState.Idle -> "Ready to record"
            is RecordingState.Counting -> "Starting in ${s.secondsLeft}"
            is RecordingState.Recording -> formatDuration(elapsedMs)
            is RecordingState.Paused -> "Paused at ${formatDuration(elapsedMs)}"
            is RecordingState.Failed -> "Recording failed: ${s.message}"
        }
        AnimatedContent(
            targetState = statusText,
            transitionSpec = {
                (slideInVertically(spring()) { it / 2 } + fadeIn(spring())) togetherWith
                    (slideOutVertically(spring()) { -it / 2 } + fadeOut(spring()))
            },
            label = "record-status",
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = if (state is RecordingState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (state is RecordingState.Recording || state is RecordingState.Paused) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Stop from the floating controls or the notification",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(56.dp))
        QuickOptions(settings, onChange = {
            context.startActivity(Intent(context, RecordLauncherActivity::class.java))
        })
        Spacer(Modifier.weight(1f))
        Text(
            "Recordings save to Movies/Celestite",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RecordButton(
    state: RecordingState,
    haptics: Haptics,
    onOpenModal: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
) {
    val mode = when (state) {
        is RecordingState.Recording -> Mode.Recording
        is RecordingState.Paused -> Mode.Paused
        is RecordingState.Counting -> Mode.Counting
        else -> Mode.Idle
    }
    val color by animateColorAsState(
        when (mode) {
            Mode.Idle -> MaterialTheme.colorScheme.primaryContainer
            Mode.Counting -> MaterialTheme.colorScheme.tertiaryContainer
            Mode.Recording -> MaterialTheme.colorScheme.errorContainer
            Mode.Paused -> MaterialTheme.colorScheme.tertiaryContainer
        },
    )
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))) togetherWith
                (fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleOut(targetScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
        },
        label = "record-state",
    ) { current ->
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier
                .size(160.dp)
                .clickable(enabled = current != Mode.Counting) {
                    when (current) {
                        Mode.Recording -> {
                            haptics.click()
                            onStop()
                        }
                        Mode.Paused -> {
                            haptics.click()
                            onResume()
                        }
                        else -> {
                            haptics.confirm()
                            onOpenModal()
                        }
                    }
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (current) {
                    Mode.Idle -> Image(
                        painter = painterResource(R.drawable.ic_record_glyph),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.size(72.dp),
                    )
                    else -> Symbol(
                        when (current) {
                            Mode.Counting -> "hourglass"
                            Mode.Recording -> "stop"
                            else -> "play_arrow"
                        },
                        size = 56.dp,
                    )
                }
            }
        }
    }
}

private enum class Mode {
    Idle,
    Counting,
    Recording,
    Paused,
}

@Composable
private fun QuickOptions(settings: CelestiteSettings, onChange: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChange),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recording options", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Symbol("chevron_right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                OptionCell(settings.resolution.label(), "Resolution")
                OptionCell(settings.fps.label(), "Frame rate")
                OptionCell(settings.audioMode.label(), "Audio")
                OptionCell(settings.format.label(), "Codec")
            }
        }
    }
}

@Composable
private fun OptionCell(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ResolutionOption.label(): String = when (this) {
    ResolutionOption.NATIVE -> "Native"
    ResolutionOption.FHD -> "1080p"
    ResolutionOption.HD -> "720p"
}

private fun FpsOption.label(): String = when (this) {
    FpsOption.AUTO -> "Auto"
    FpsOption.S60 -> "60 fps"
    FpsOption.S30 -> "30 fps"
}

private fun AudioMode.label(): String = when (this) {
    AudioMode.NONE -> "None"
    AudioMode.MIC -> "Mic"
    AudioMode.DEVICE -> "Device"
}

private fun VideoFormat.label(): String = when (this) {
    VideoFormat.H264 -> "H.264"
    VideoFormat.H265 -> "H.265"
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
