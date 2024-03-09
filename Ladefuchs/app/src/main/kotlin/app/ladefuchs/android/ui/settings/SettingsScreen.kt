package app.ladefuchs.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.ladefuchs.android.R

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    SettingsScreen(
        modifier = modifier,
        onNavUpClick = {
            navController.popBackStack()
        }
    )
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavUpClick: () -> Unit,
    ) {
    Column(
        modifier = modifier
            .background(colorResource(id = R.color.UIColorLight))
            .fillMaxWidth()
    ) {
        Text(text = "Settings")
    }
}
