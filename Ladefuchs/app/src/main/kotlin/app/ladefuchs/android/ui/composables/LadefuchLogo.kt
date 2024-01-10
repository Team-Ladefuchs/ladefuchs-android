package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.ladefuchs.android.R

@Composable
fun LadefuchsLogo(
    modifier: Modifier = Modifier,
    showNerdGlasses: Boolean = false
) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier
                .width(84.dp)
                .height(78.dp),
            painter = painterResource(id = R.drawable.ic_ladefuchs_logo),
            contentDescription = stringResource(id = R.string.ladefuchs_logo_description),
        )
        if (showNerdGlasses) {
            Image(
                modifier = Modifier
                    .padding(start = 22.dp, top = 20.dp)
                    .width(62.dp)
                    .height(28.dp),
                painter = painterResource(id = R.drawable.glasses),
                contentDescription = stringResource(id = R.string.ladefuchs_logo_description),
            )
        }
    }
}

@Preview
@Composable
private fun Logo() {
    LadefuchsLogo()
}

@Preview
@Composable
private fun LogoWithGlasses() {
    LadefuchsLogo(showNerdGlasses = true)
}
