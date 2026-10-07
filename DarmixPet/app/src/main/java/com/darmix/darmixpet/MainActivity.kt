package com.darmix.darmixpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.darmix.darmixpet.monitor.AccessibilityPermissionHelper
import com.darmix.darmixpet.monitor.BatteryOptimizationHelper
import com.darmix.darmixpet.monitor.BrightnessPermissionHelper
import com.darmix.darmixpet.monitor.UsageStatsPermissionHelper
import com.darmix.darmixpet.overlay.NotificationPermissionHelper
import com.darmix.darmixpet.overlay.OverlayPermissionHelper
import com.darmix.darmixpet.overlay.PetOverlayService
import com.darmix.darmixpet.ui.AppListScreen
import com.darmix.darmixpet.ui.AppScreen
import com.darmix.darmixpet.ui.DarmixSplashScreen
import com.darmix.darmixpet.ui.GuideDialog
import com.darmix.darmixpet.ui.GuidePreferences
import com.darmix.darmixpet.ui.PermissionsScreen
import com.darmix.darmixpet.ui.SettingsScreen
import com.darmix.darmixpet.ui.SkinSelectionScreen
import com.darmix.darmixpet.ui.UpdateHost
import com.darmix.darmixpet.ui.components.BottomNavBar
import com.darmix.darmixpet.ui.theme.DarmixPetTheme
import com.darmix.darmixpet.ui.theme.ThemeMode
import com.darmix.darmixpet.ui.theme.ThemePreferences
import com.darmix.darmixpet.update.UpdateManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }

            val darkNow = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(darkNow) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                    ) { darkNow },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                    ) { darkNow }
                )
                onDispose {}
            }

            DarmixPetTheme(themeMode = themeMode) {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                if (showSplash) {
                    DarmixSplashScreen(onFinished = { showSplash = false })
                } else {
                    val lifecycleOwner = LocalLifecycleOwner.current

                    fun checkAllGranted() =
                        OverlayPermissionHelper.hasOverlayPermission(context) &&
                                UsageStatsPermissionHelper.hasUsageStatsPermission(context) &&
                                NotificationPermissionHelper.hasNotificationPermission(context) &&
                                BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context) &&
                                AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context) &&
                                BrightnessPermissionHelper.hasWriteSettingsPermission(context)

                    var allGranted by remember { mutableStateOf(checkAllGranted()) }
                    var selectedTab by remember { mutableStateOf(AppScreen.APPS) }

                    LaunchedEffect(Unit) {
                        UpdateManager.loadCached(context)
                        UpdateManager.checkIfDue(context)
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                allGranted = checkAllGranted()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    var showGuide by remember { mutableStateOf(false) }
                    LaunchedEffect(allGranted) {
                        if (allGranted && !GuidePreferences.hasSeen(context)) showGuide = true
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (allGranted) {
                                BottomNavBar(current = selectedTab, onSelect = { selectedTab = it })
                            }
                        }
                    ) { innerPadding ->
                        if (allGranted) {
                            LaunchedEffect(Unit) {
                                PetOverlayService.start(context)
                            }
                            when (selectedTab) {
                                AppScreen.APPS -> AppListScreen(modifier = Modifier.padding(innerPadding))
                                AppScreen.MASCOT -> SkinSelectionScreen(modifier = Modifier.padding(innerPadding))
                                AppScreen.SETTINGS -> SettingsScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    currentThemeMode = themeMode,
                                    onThemeModeChange = { newMode ->
                                        themeMode = newMode
                                        ThemePreferences.setThemeMode(context, newMode)
                                    }
                                )
                            }
                        } else {
                            PermissionsScreen(onAllGranted = { allGranted = true })
                        }
                    }

                    if (showGuide && allGranted) GuideDialog(onDismiss = { showGuide = false })

                    UpdateHost(suppress = showGuide && allGranted)
                }
            }
        }
    }
}
