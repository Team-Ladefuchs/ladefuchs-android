package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.ladefuchs.android.R

/**
 * Advertisement-like banner
 */
@Composable
fun Banner(
    modifier: Modifier = Modifier,
    imagePainter: Painter = painterResource(id = R.drawable.banner_thg),
    contentDescription: String = stringResource(id = R.string.img_btn_desc)
) {
    Image(
        modifier = modifier,
        painter = imagePainter,
        contentDescription = contentDescription
    )
}

@Preview
@Composable
private fun Banner() {
    Banner(modifier = Modifier)
}
