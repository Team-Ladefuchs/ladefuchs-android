package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.ladefuchs.android.R

/**
 * colored triangle in the top right corner
 */
@Composable
internal fun TopRightCornerIndicator() {
    // indicator shadow
    Image(
        modifier = Modifier
            .size(20.dp)
            .padding(top = 1.dp, end = 1.dp),
        painter = painterResource(id = R.drawable.shadow), contentDescription = null
    )
    // indicator
    Image(
        modifier = Modifier.size(20.dp),
        painter = painterResource(id = R.drawable.huetchen),
        contentDescription = null
    )
}
