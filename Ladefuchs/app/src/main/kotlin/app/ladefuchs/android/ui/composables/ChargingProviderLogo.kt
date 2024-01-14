package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.ladefuchs.android.R

/**
 * Charging provider logo as a card with rounded corners and an optional red triangle as
 * indicator in the top-right corner
 */
@Composable
internal fun ChargingProviderLogo(
    showIndicator: Boolean = false
) {
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(5.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.width(100.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(5.dp)),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = R.drawable.card_adac),
                contentDescription = null,
            )

            // indicator for blocking fee or additional info
            if (showIndicator) {
                TopRightCornerIndicator()
            }
        }
    }
}

@Preview
@Composable
private fun ChargingProviderLogo() {
    ChargingProviderLogo(
        showIndicator = true
    )
}
