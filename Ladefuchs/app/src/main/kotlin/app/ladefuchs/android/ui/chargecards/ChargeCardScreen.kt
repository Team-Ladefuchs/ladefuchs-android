package app.ladefuchs.android.ui.chargecards

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.ladefuchs.android.R
import app.ladefuchs.android.ui.composables.Banner
import app.ladefuchs.android.ui.composables.ChargingCardTable
import app.ladefuchs.android.ui.composables.LadefuchsLogo
import app.ladefuchs.android.ui.composables.Phrase
import app.ladefuchs.android.ui.composables.PoCHeader


/**
 * stateful
 */
@Composable
fun ChargeCardScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val viewModel: ChargeCardViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsState().value

    ChargeCardScreen(
        modifier = modifier,
        uiState = uiState,
        onLogoClick = { viewModel.handleEvent(ChargeCardEvent.LogoClicked) },
        onSettingsClick = { navController.navigate(R.id.navigation_settings) }
    )
}

/**
 * stateless
 */
@Composable
fun ChargeCardScreen(
    modifier: Modifier = Modifier,
    uiState: ChargeCardUiState,
    onLogoClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    // TODO: onboarding
    Column(
        modifier = modifier
            .background(colorResource(id = R.color.UIColorLight))
            .fillMaxWidth()
    ) {
        // Header
        Header(
            onLogoClick = onLogoClick,
            isEasterEggEnabled = uiState.isEasterEggEnabled,
            onSettingsClick = onSettingsClick
        )

        // TODO: onclick List item -> createCardDetailPopup
        ChargingCardTable(
            acItems = uiState.acItems,
            dcItems = uiState.dcItems,
        )

        PoCHeader()

        WheelPicker(
            onItemSelected = { index ->
                // TODO: handle selection
            }
        )

        Box(contentAlignment = Alignment.BottomCenter) {
            Phrase(phrase = "Hier könnte Ihre Werbung stehen!")
            Banner()
        }
    }
}

@Composable
private fun Header(
    onLogoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isEasterEggEnabled: Boolean = false,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        LadefuchsLogo(
            Modifier.clickable {
                onLogoClick()
            },
            showNerdGlasses = isEasterEggEnabled
        )
        Image(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(50.dp)
                .clickable {
                    onSettingsClick()
                },
            painter = painterResource(id = R.drawable.ic_settings),
            contentDescription = stringResource(id = R.string.settings_button),
        )
    }
}

@Composable
private fun WheelPicker(
    onItemSelected: (index: Int) -> Unit,
) {
    val density = LocalDensity.current
    AndroidView(
        modifier = Modifier
            .height(145.dp)
            .fillMaxWidth(),
        factory = { context ->
            com.aigestudio.wheelpicker.WheelPicker(context).apply {
                setAtmospheric(true)
                curtainColor = R.color.WheelCurtainColor
                isCurved = true
                isCyclic = false
                itemTextColor = R.color.TextColorDisabled
                itemTextSize = with(density) { 22.dp.toPx().toInt() }
                selectedItemTextColor = R.color.TextColorDark
                visibleItemCount = 5
                setOnClickListener {
                    // TODO: update list
                    onItemSelected(it.id)
                }
            }
        }
    )
}

@Preview(showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChargeCard() {
    ChargeCardScreen(
        uiState = ChargeCardUiState(isEasterEggEnabled = true),
        onLogoClick = {},
        onSettingsClick = {}
    )
}
