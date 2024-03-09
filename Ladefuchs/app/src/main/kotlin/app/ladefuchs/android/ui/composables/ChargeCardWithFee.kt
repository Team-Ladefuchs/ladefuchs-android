package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ladefuchs.android.R

/**
 * List item with the logo of the charging provider on the left and its
 * current charging fee as string next to it.
 *
 * Opt. with a red triangle in the top-right corner to indicate
 * additional information or a blocking fee.
 */
@Composable
fun ChargeCardWithFee(
    modifier: Modifier = Modifier,
    useBrightBackground: Boolean = true,
    showIndicator: Boolean = true,
    text: String = "N/A"
) {
    val background = if (useBrightBackground) {
        colorResource(id = R.color.TableColorDark)
    } else {
        colorResource(id = R.color.TableColorLight)
    }

    // colored background (even/odd)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // charging provider logo
        ChargingProviderLogo(showIndicator = showIndicator)

        // charging fee
        Text(
            modifier = Modifier.padding(start = 20.dp),
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun ChargeCardWithFee() {
    Column(modifier = Modifier.width(250.dp)) {
        ChargeCardWithFee(
            modifier = Modifier,
            useBrightBackground = true,
            showIndicator = false,
            text = "0.29"
        )
        ChargeCardWithFee(
            modifier = Modifier,
            useBrightBackground = false,
            showIndicator = false,
            text = "0.29"
        )
        ChargeCardWithFee(
            modifier = Modifier,
            useBrightBackground = true,
            showIndicator = true,
            text = "0.29"
        )
    }
}
