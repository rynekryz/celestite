package com.rynekryz.celestite.feature.settings

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun SettingsScreen(
    onOpenLicenses: () -> Unit,
    versionName: String,
) {
    val context = LocalContext.current
    val store = CelestiteStoreHolder.get(context)
    val settings by store.settings.collectAsState(initial = CelestiteSettings())
    val overlayControls by store.overlayControls.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Symbol("settings", size = 32.dp)
            Spacer(Modifier.padding(6.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(20.dp))

        SectionTitle("Capture")
        SectionCard {
            OptionSegmented(
                label = "Resolution",
                options = ResolutionOption.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = ResolutionOption.entries.indexOf(settings.resolution),
                onSelect = { index ->
                    haptics.click()
                    scope.launch { store.setResolution(ResolutionOption.entries[index]) }
                },
            )
            OptionSegmented(
                label = "Frame rate",
                options = listOf("Auto", "60 fps", "30 fps"),
                selectedIndex = FpsOption.entries.indexOf(settings.fps),
                onSelect = { index ->
                    haptics.click()
                    scope.launch { store.setFps(FpsOption.entries[index]) }
                },
            )
            OptionSegmented(
                label = "Quality",
                options = listOf("Auto", "High", "Medium", "Low"),
                selectedIndex = QualityOption.entries.indexOf(settings.quality),
                onSelect = { index ->
                    haptics.click()
                    scope.launch { store.setQuality(QualityOption.entries[index]) }
                },
            )
            OptionSegmented(
                label = "Audio source",
                options = listOf("None", "Mic", "Device"),
                selectedIndex = AudioMode.entries.indexOf(settings.audioMode),
                onSelect = { index ->
                    haptics.click()
                    scope.launch { store.setAudioMode(AudioMode.entries[index]) }
                },
            )
            ToggleRow(
                label = "HEVC encoding",
                subtitle = "Smaller files, needs a newer device",
                checked = settings.format == VideoFormat.H265,
                onChecked = { checked ->
                    haptics.click()
                    scope.launch { store.setFormat(if (checked) VideoFormat.H265 else VideoFormat.H264) }
                },
            )
            OptionSegmented(
                label = "Countdown",
                options = listOf("Off", "3 s", "5 s", "10 s"),
                selectedIndex = listOf(0, 3, 5, 10).indexOf(settings.countdownSeconds),
                onSelect = { index ->
                    haptics.click()
                    scope.launch { store.setCountdown(listOf(0, 3, 5, 10)[index]) }
                },
            )
            ToggleRow(
                label = "Floating controls",
                subtitle = "Pill to pause and stop while recording. It shows as a black box in the video, so it is off by default",
                checked = overlayControls,
                onChecked = { checked ->
                    haptics.click()
                    scope.launch { store.setOverlayControls(checked) }
                },
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Permissions")
        SectionCard {
            PermissionRow(
                label = "Overlay",
                subtitle = "Needed for the countdown display and the optional floating controls",
                granted = Settings.canDrawOverlays(context),
                fix = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}"),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
            PermissionRow(
                label = "Microphone",
                subtitle = "Needed for mic audio in recordings",
                granted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED,
                fix = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:${context.packageName}"),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("About")
        SectionCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Celestite", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "v$versionName · by Ryne",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Symbol("screen_record", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.click()
                        onOpenLicenses()
                    }
                    .padding(vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Open source licenses", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "GPLv3 and the libraries Celestite uses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Symbol("chevron_right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.click()
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/rynekryz/celestite/blob/main/LICENSE"),
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                    .padding(vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("License", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "GNU General Public License v3.0\nFree, open source software. You may use, study, share and improve it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Symbol("open_in_new", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun OptionSegmented(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp, top = 8.dp),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(option)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun PermissionRow(
    label: String,
    subtitle: String,
    granted: Boolean,
    fix: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (granted) {
            Symbol("check_circle", tint = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                "Grant",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { fix() }
                    .padding(8.dp),
            )
        }
    }
}
