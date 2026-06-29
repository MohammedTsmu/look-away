package com.eyecare.lookaway.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eyecare.lookaway.ui.screens.HomeScreen
import com.eyecare.lookaway.ui.screens.SettingsScreen
import com.eyecare.lookaway.ui.theme.LookAwayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.eyecare.lookaway.util.LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            LookAwayTheme(settings.themeMode, settings.accentIndex) {
                val context = LocalContext.current
                var permTick by remember { mutableIntStateOf(0) }
                OnResume { permTick++ }
                val perms = remember(permTick) { Permissions.snapshot(context) }

                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { permTick++ }

                // Honor "Start when app opens".
                var autoStartChecked by rememberSaveable { mutableStateOf(false) }
                val engineState by viewModel.engineState.collectAsStateWithLifecycle()
                LaunchedEffect(settings.startOnAppOpen, engineState.isRunning) {
                    if (!autoStartChecked && settings.startOnAppOpen && !engineState.isRunning) {
                        autoStartChecked = true
                        viewModel.start()
                    }
                }

                var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
                when (screen) {
                    SCREEN_SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { screen = SCREEN_HOME },
                        onOpenIntent = { intent -> openSafely(intent) },
                    )
                    else -> HomeScreen(
                        viewModel = viewModel,
                        permissions = perms,
                        onOpenSettings = { screen = SCREEN_SETTINGS },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenIntent = { intent -> openSafely(intent) },
                    )
                }
            }
        }
    }

    private fun openSafely(intent: Intent) {
        runCatching { startActivity(intent) }
    }

    companion object {
        private const val SCREEN_HOME = "home"
        private const val SCREEN_SETTINGS = "settings"
    }
}

/** Runs [block] each time the activity resumes (used to refresh permission state). */
@Composable
private fun OnResume(block: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) block()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
