package com.darmix.darmixpet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.darmix.darmixpet.ui.PermissionsScreen
import com.darmix.darmixpet.ui.SettingsScreen
import com.darmix.darmixpet.ui.SkinSelectionScreen
import com.darmix.darmixpet.ui.components.BottomNavBar
import com.darmix.darmixpet.ui.theme.DarmixPetTheme
import com.darmix.darmixpet.ui.theme.ThemeMode
import com.darmix.darmixpet.ui.theme.ThemePreferences
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }

            DarmixPetTheme(themeMode = themeMode) {
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

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            allGranted = checkAllGranted()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            }
        }
    }
}