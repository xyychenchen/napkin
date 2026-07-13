package com.imageflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imageflow.app.ui.screen.HistoryDetailScreen
import com.imageflow.app.ui.screen.HomeScreen
import com.imageflow.app.ui.screen.SettingsScreen
import com.imageflow.app.ui.theme.ImageFlowTheme
import com.imageflow.app.viewmodel.GenerateViewModel
import com.imageflow.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageFlowTheme {
                ImageFlowApp()
            }
        }
    }
}

@Composable
fun ImageFlowApp() {
    val navController = rememberNavController()
    val generateVm: GenerateViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    generateVm = generateVm,
                    settingsVm = settingsVm,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenHistory = { id -> navController.navigate("history/$id") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    vm = settingsVm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                HistoryDetailScreen(
                    vm = generateVm,
                    historyId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
