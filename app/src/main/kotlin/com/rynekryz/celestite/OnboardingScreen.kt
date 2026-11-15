@file:OptIn(ExperimentalMaterial3Api::class)

package com.rynekryz.celestite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rynekryz.celestite.core.designsystem.Symbol
import com.rynekryz.celestite.core.designsystem.rememberHaptics
import kotlinx.coroutines.launch

private data class OobePage(
    val icon: String,
    val container: Color,
    val onContainer: Color,
    val title: String,
    val body: String,
    val lines: List<String> = emptyList(),
)

@Composable
private fun oobePages() = MaterialTheme.colorScheme.run {
    listOf(
        OobePage(
            icon = "screen_record",
            container = primaryContainer,
            onContainer = onPrimaryContainer,
            title = "Welcome to Celestite",
            body = "Screen recording without strings. Native speed, zero bloat and fully offline.",
        ),
        OobePage(
            icon = "graphic_eq",
            container = secondaryContainer,
            onContainer = onSecondaryContainer,
            title = "Capture it your way",
            body = "Tune every take before it starts, right from the record modal.",
            lines = listOf(
                "Mic or device audio, or none at all",
                "H.264 everywhere or H.265 for smaller files",
                "Native, 1080p or 720p at up to your refresh rate",
            ),
        ),
        OobePage(
            icon = "touch_app",
            container = tertiaryContainer,
            onContainer = onTertiaryContainer,
            title = "Control from anywhere",
            body = "A floating pill follows you around, invisible in the recording.",
            lines = listOf(
                "Draggable floating pause and stop controls",
                "Optional 3, 5 or 10 second countdown",
                "Record straight from the Quick Settings tile",
            ),
        ),
        OobePage(
            icon = "palette",
            container = primaryContainer,
            onContainer = onPrimaryContainer,
            title = "Made for you",
            body = "Celestite wears the colors of your wallpaper and adapts to your day.",
            lines = listOf(
                "Material You colors from your system theme",
                "Themed app icon that follows your palette",
                "Expressive Material 3 motion throughout",
            ),
        ),
        OobePage(
            icon = "lock",
            container = secondaryContainer,
            onContainer = onSecondaryContainer,
            title = "Private by default",
            body = "No account. No tracking. No ads. Nothing ever leaves your device.",
            lines = listOf(
                "No internet permission at all",
                "Recordings stay in Movies/Celestite",
                "Source code is open under GPLv3",
            ),
        ),
    )
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = oobePages()
    val pager = rememberPagerState { pages.size }
    var hasNotifications by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotifications = granted || Build.VERSION.SDK_INT < 33
    }
    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        hasOverlay = Settings.canDrawOverlays(context)
    }

    fun nextPage() {
        scope.launch {
            val target = pager.currentPage + 1
            if (target < pages.size) pager.animateScrollToPage(target) else onDone()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        haptics.tick()
                        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                    },
                    enabled = pager.currentPage > 0,
                ) { Text("Back") }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        haptics.tick()
                        onDone()
                    },
                    enabled = pager.currentPage < pages.size - 1,
                ) { Text("Skip") }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                val p = pages[page]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(44.dp),
                        color = p.container,
                        modifier = Modifier.size(148.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Symbol(p.icon, size = 64.dp, tint = p.onContainer)
                        }
                    }
                    Text(
                        p.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                    Text(
                        p.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp).widthIn(max = 320.dp),
                    )
                    if (p.lines.isNotEmpty()) {
                        Column(
                            Modifier.padding(top = 20.dp).widthIn(max = 340.dp),
                        ) {
                            p.lines.forEach { line ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Symbol("check_circle", size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Text(line, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val offset = pager.currentPage + pager.currentPageOffsetFraction
                pages.indices.forEach { i ->
                    val selected = i == offset.toInt()
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "dot-width",
                    )
                    val color by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        label = "dot-color",
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pager.currentPage == pages.size - 1) {
                    if (!hasNotifications && Build.VERSION.SDK_INT >= 33) {
                        FilledTonalButton(
                            onClick = {
                                haptics.click()
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Symbol("notifications", size = 20.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Allow notifications")
                        }
                    }
                    if (!hasOverlay) {
                        FilledTonalButton(
                            onClick = {
                                haptics.click()
                                overlayLauncher.launch(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Symbol("picture_in_picture", size = 20.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Allow overlay")
                        }
                    }
                    Button(
                        onClick = {
                            haptics.confirm()
                            onDone()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Get started") }
                } else {
                    Text(
                        "You can change everything later in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        haptics.click()
                        nextPage()
                    }) { Text("Next") }
                }
            }
        }
    }
}
