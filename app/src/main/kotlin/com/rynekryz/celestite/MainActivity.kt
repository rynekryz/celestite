package com.rynekryz.celestite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import com.rynekryz.celestite.core.datastore.CelestiteStoreHolder
import com.rynekryz.celestite.core.designsystem.CelestiteTheme
import com.rynekryz.celestite.core.designsystem.Symbol
import com.rynekryz.celestite.feature.gallery.GalleryScreen
import com.rynekryz.celestite.feature.recorder.RecorderScreen
import com.rynekryz.celestite.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

private val enterSpec = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)
private val exitSpec = tween<IntOffset>(durationMillis = 220)
private val fadeSpec = tween<Float>(durationMillis = 180)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CelestiteTheme {
                CelestiteApp()
            }
        }
    }
}

private enum class Destination(val label: String, val symbol: String) {
    Record("Record", "screen_record"),
    Gallery("Gallery", "movie"),
    Settings("Settings", "settings"),
}

@Composable
private fun CelestiteApp() {
    val context = LocalContext.current
    val store = CelestiteStoreHolder.get(context)
    val scope = rememberCoroutineScope()
    val onboarded by store.onboarded.collectAsState(initial = true)
    if (!onboarded) {
        OnboardingScreen(
            onDone = {
                scope.launch { store.setOnboarded() }
            },
        )
        return
    }
    var destination by rememberSaveable { mutableStateOf(Destination.Record) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var playerOpen by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")
    }

    BackHandler(enabled = showLicenses) { showLicenses = false }
    BackHandler(enabled = !showLicenses && destination != Destination.Record) {
        destination = Destination.Record
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = !playerOpen,
                enter = slideInVertically(exitSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutVertically(exitSpec) { it } + fadeOut(fadeSpec),
            ) {
                NavigationBar {
                    Destination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = !showLicenses && destination == dest,
                            onClick = {
                                destination = dest
                                showLicenses = false
                            },
                            icon = { Symbol(dest.symbol) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnimatedContent(
                targetState = showLicenses,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(enterSpec) { it / 3 } + fadeIn(tween(260))) togetherWith
                            fadeOut(fadeSpec)
                    } else {
                        (slideInHorizontally(enterSpec) { -it / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(exitSpec) { it / 5 } + fadeOut(fadeSpec))
                    }
                },
                label = "licenses",
            ) { licenses ->
                if (licenses) {
                    LicensesScreen(onBack = { showLicenses = false })
                } else {
                    AnimatedContent(
                        targetState = destination,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            if (forward) {
                                (slideInHorizontally(enterSpec) { it / 3 } + fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(exitSpec) { -it / 5 } + fadeOut(fadeSpec))
                            } else {
                                (slideInHorizontally(enterSpec) { -it / 3 } + fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(exitSpec) { it / 5 } + fadeOut(fadeSpec))
                            }
                        },
                        label = "tabs",
                    ) { dest ->
                        when (dest) {
                            Destination.Record -> RecorderScreen()
                            Destination.Gallery -> GalleryScreen(onPlayerChanged = { playerOpen = it })
                            Destination.Settings -> SettingsScreen(
                                onOpenLicenses = { showLicenses = true },
                                versionName = versionName,
                            )
                        }
                    }
                }
            }
        }
    }
}
