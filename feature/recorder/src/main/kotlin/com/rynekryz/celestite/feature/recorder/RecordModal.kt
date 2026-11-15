package com.rynekryz.celestite.feature.recorder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rynekryz.celestite.core.datastore.CelestiteStoreHolder
import com.rynekryz.celestite.core.designsystem.Symbol
import com.rynekryz.celestite.core.designsystem.rememberHaptics
import com.rynekryz.celestite.core.model.AudioMode
import com.rynekryz.celestite.core.model.CelestiteSettings
import com.rynekryz.celestite.core.model.FpsOption
import com.rynekryz.celestite.core.model.QualityOption
import com.rynekryz.celestite.core.model.ResolutionOption
import com.rynekryz.celestite.core.model.VideoFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordModal(
    onDismiss: () -> Unit,
    onRecord: () -> Unit,
) {
    val context = LocalContext.current
    val store = CelestiteStoreHolder.get(context)
    val settings by store.settings.collectAsState(initial = CelestiteSettings())
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun hide(onDone: () -> Unit = onDismiss) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDone()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Record screen", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Symbol(
                    "close",
                    modifier = Modifier
                        .clickable { hide() }
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            OptionLabel("Audio source")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AudioMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.audioMode == mode,
                        onClick = {
                            haptics.click()
                            scope.launch { store.setAudioMode(mode) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, AudioMode.entries.size),
                    ) {
                        Text(mode.label())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("HEVC encoding", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Smaller files, needs a newer device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.format == VideoFormat.H265,
                    onCheckedChange = { checked ->
                        haptics.click()
                        scope.launch {
                            store.setFormat(if (checked) VideoFormat.H265 else VideoFormat.H264)
                        }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            OptionLabel("Resolution")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ResolutionOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.resolution == option,
                        onClick = {
                            haptics.click()
                            scope.launch { store.setResolution(option) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, ResolutionOption.entries.size),
                    ) {
                        Text(option.label())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OptionLabel("Frame rate")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                FpsOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.fps == option,
                        onClick = {
                            haptics.click()
                            scope.launch { store.setFps(option) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, FpsOption.entries.size),
                    ) {
                        Text(option.label())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OptionLabel("Quality")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                QualityOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.quality == option,
                        onClick = {
                            haptics.click()
                            scope.launch { store.setQuality(option) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, QualityOption.entries.size),
                    ) {
                        Text(option.label())
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OptionLabel("Countdown")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val countdowns = listOf(0, 3, 5, 10)
                countdowns.forEachIndexed { index, seconds ->
                    SegmentedButton(
                        selected = settings.countdownSeconds == seconds,
                        onClick = {
                            haptics.click()
                            scope.launch { store.setCountdown(seconds) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, countdowns.size),
                    ) {
                        Text(if (seconds == 0) "Off" else "$seconds s")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    hide(onRecord)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Record", style = MaterialTheme.typography.titleMedium)
            }
        }
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

private fun QualityOption.label(): String = when (this) {
    QualityOption.AUTO -> "Auto"
    QualityOption.HIGH -> "High"
    QualityOption.MEDIUM -> "Medium"
    QualityOption.LOW -> "Low"
}

private fun AudioMode.label(): String = when (this) {
    AudioMode.NONE -> "None"
    AudioMode.MIC -> "Mic"
    AudioMode.DEVICE -> "Device"
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
