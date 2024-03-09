package app.ladefuchs.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.ladefuchs.android.chargecards.ui.ChargeCardScreen
import app.ladefuchs.android.ui.settings.SettingsScreen

/**
 * Application entry-point
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LadefuchsApp()
        }
    }
}

@Composable
fun LadefuchsApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars), // Is hanled by content
        content = { innerPadding ->
            NavHost(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                navController = navController,
                startDestination = "chargeCards",
            ) {
                composable(route = "chargeCards") {
                    ChargeCardScreen(navController = navController)
                }
                composable(route = "settings") {
                    SettingsScreen(navController = navController)
                }
                composable(route = "cardDetails") {

                }
            }
        }
    )
}
