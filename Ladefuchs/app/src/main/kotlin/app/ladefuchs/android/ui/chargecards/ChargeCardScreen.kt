package app.ladefuchs.android.ui.chargecards

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.ladefuchs.android.R
import app.ladefuchs.android.ui.composables.Banner
import app.ladefuchs.android.ui.composables.ChargeCardsTableHeader
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
//    val viewmodel: ChargeCardViewmodel = getViewModel()
//    val uiState = viewmodel.uiState.collectAsState().value

    ChargeCardScreen(
        modifier = modifier,
        uiState = ChargeCardUiState(),
        onLogoClick = {
            // TODO: increase count
        },
        onSettingsClick = {}//viewModel::navigateUp,
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
    Column(
        modifier = modifier
            .background(colorResource(id = R.color.UIColorLight))
            .fillMaxWidth()
    ) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LadefuchsLogo(
                Modifier.clickable {
                    onLogoClick()
                },
                showNerdGlasses = uiState.isEasterEggEnabled
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

        ChargeCardsTableHeader()

        // TODO: List
        Spacer(Modifier.weight(0.8f))

        PoCHeader()

        val density = LocalDensity.current
        AndroidView(
            modifier = Modifier
                .height(145.dp)
                .fillMaxWidth(),
            factory = { context ->
                com.aigestudio.wheelpicker.WheelPicker(context).apply {
                    curtainColor = R.color.WheelCurtainColor
                    isCurved = true
                    isCyclic = false
                    itemTextColor = R.color.TextColorDisabled
                    itemTextSize = with(density) { 22.dp.toPx().toInt() }
                    selectedItemTextColor = R.color.TextColorDark
                    visibleItemCount = 5
                    setOnClickListener {
                        // TODO: update list
                    }
                }
            }
        )

        Box(contentAlignment = Alignment.BottomCenter) {
            Phrase(phrase = "Hier könnte Ihre Werbung stehen!")
            Banner()
        }
    }
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
