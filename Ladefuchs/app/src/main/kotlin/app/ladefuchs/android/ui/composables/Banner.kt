package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.ladefuchs.android.R

@Composable
fun Banner(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.banner_thg),
        contentDescription = stringResource(id = R.string.img_btn_desc)
    )
}

@Preview
@Composable
private fun Banner() {
    Banner(modifier = Modifier)
}
