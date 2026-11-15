package com.rynekryz.celestite.feature.gallery

import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rynekryz.celestite.core.designsystem.Symbol
import com.rynekryz.celestite.core.designsystem.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun GalleryScreen(onPlayerChanged: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<VideoItem?>(null) }
    val videos by produceState(emptyList(), refresh) {
        value = MediaLibrary.query(context)
    }

    val current = selected
    DisposableEffect(current) {
        if (current != null) onPlayerChanged(true)
        onDispose {
            if (current != null) onPlayerChanged(false)
        }
    }
    if (current != null) {
        PlayerScreen(
            video = current,
            onBack = { selected = null },
            onDeleted = {
                selected = null
                refresh++
            },
            onTrimmed = { refresh++ },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Symbol("movie", size = 32.dp)
            Spacer(Modifier.padding(6.dp))
            Text(
                "Gallery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(24.dp))
        if (videos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Symbol("movie_off", size = 64.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text("No recordings yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your screen recordings will show up here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(videos, key = { it.uri }) { video ->
                    VideoRow(video) { selected = video }
                }
            }
        }
    }
}

@Composable
private fun VideoRow(video: VideoItem, onOpen: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val bitmap by produceState<Bitmap?>(null, video.uri) {
        value = MediaLibrary.thumbnail(context, video.uri, 256)
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.click()
                onOpen()
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(Modifier.padding(end = 16.dp)) {
                Text(
                    video.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatTime(video.durationMs)} · ${MediaLibrary.formatSize(video.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onTrimmed: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var showControls by remember { mutableStateOf(true) }
    var showTrimmer by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(video.durationMs.toInt()) }

    val player = remember(video.uri) { MediaPlayer() }

    BackHandler {
        if (showTrimmer) showTrimmer = false else onBack()
    }

    DisposableEffect(video.uri) {
        onDispose { player.release() }
    }

    LaunchedEffect(playing) {
        while (playing) {
            position = runCatching { player.currentPosition }.getOrDefault(position)
            delay(500)
        }
    }

    LaunchedEffect(playing, showControls) {
        if (playing && showControls) {
            delay(3000)
            showControls = false
        }
    }

    val videoAspect = if (video.width > 0 && video.height > 0) {
        video.width.toFloat() / video.height
    } else {
        16f / 9f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { showControls = !showControls }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(videoAspect)
                .align(Alignment.Center),
        ) {
            AndroidView(
                factory = { ctx ->
                    android.view.SurfaceView(ctx).also { surfaceView ->
                        surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                try {
                                    player.reset()
                                    player.setSurface(holder.surface)
                                    player.setDataSource(ctx, video.uri)
                                    player.setOnPreparedListener {
                                        duration = player.duration
                                        player.seekTo(position)
                                        if (playing) player.start()
                                    }
                                    player.setOnCompletionListener { playing = false }
                                    player.prepareAsync()
                                } catch (_: Exception) {
                                }
                            }

                            override fun surfaceChanged(
                                holder: android.view.SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {}

                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                playing = false
                                player.pause()
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                            ),
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        IconButton(onClick = {
                            haptics.click()
                            onBack()
                        }) { Symbol("arrow_back", tint = Color.White) }
                        Text(
                            video.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Surface(
                        onClick = {
                            haptics.click()
                            playing = !playing
                            if (playing) player.start() else player.pause()
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                    ) {
                        Symbol(
                            if (playing) "pause" else "play_arrow",
                            size = 40.dp,
                            tint = Color.White,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            ),
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                formatTime(position.toLong()),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                            Slider(
                                value = if (duration > 0) position.toFloat() / duration else 0f,
                                onValueChange = { fraction ->
                                    position = (fraction * duration).roundToInt()
                                },
                                onValueChangeFinished = {
                                    runCatching { player.seekTo(position) }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatTime(duration.toLong()),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IconButton(onClick = {
                                haptics.click()
                                runCatching {
                                    context.startActivity(MediaLibrary.share(context, video.uri))
                                }
                            }) { Symbol("share", tint = Color.White) }
                            IconButton(onClick = {
                                haptics.click()
                                showTrimmer = true
                            }) { Symbol("content_cut", tint = Color.White) }
                            IconButton(onClick = {
                                haptics.click()
                                MediaLibrary.delete(context, video.uri)
                                onDeleted()
                            }) { Symbol("delete", tint = Color.White) }
                        }
                    }
                }
            }
        }
    }

    if (showTrimmer) {
        ModalBottomSheet(
            onDismissRequest = { showTrimmer = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TrimmerSection(
                video = video,
                durationMs = duration,
                player = player,
                onTrimmed = {
                    showTrimmer = false
                    onTrimmed()
                },
            )
        }
    }
}

@Composable
private fun TrimmerSection(
    video: VideoItem,
    durationMs: Int,
    player: MediaPlayer,
    onTrimmed: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var startFraction by remember(video.uri) { mutableStateOf(0f) }
    var endFraction by remember(video.uri) { mutableStateOf(1f) }
    var busy by remember { mutableStateOf(false) }

    Column {
        Text("Trim", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        RangeSlider(
            value = startFraction..endFraction,
            onValueChange = { range ->
                startFraction = range.start
                endFraction = range.endInclusive
            },
            onValueChangeFinished = {
                runCatching { player.seekTo((startFraction * durationMs).roundToInt()) }
            },
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                formatTime((startFraction * durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatTime((endFraction * durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !busy && endFraction - startFraction > 0.02f,
            onClick = {
                haptics.click()
                busy = true
                val startUs = (startFraction * durationMs * 1000).roundToInt().toLong()
                val endUs = (endFraction * durationMs * 1000).roundToInt().toLong()
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        runCatching { trim(context, video.uri, startUs, endUs) }.getOrNull()
                    }
                    busy = false
                    if (result != null) {
                        haptics.confirm()
                        onTrimmed()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(if (busy) "Saving..." else "Save trimmed copy")
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
