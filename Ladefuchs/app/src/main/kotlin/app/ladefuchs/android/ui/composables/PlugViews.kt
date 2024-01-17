package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ladefuchs.android.R

/**
 * Charging type in uppercase letters next to its most-known connector as icon
 */
@Composable
fun PlugView(
    modifier: Modifier = Modifier,
    label: String = stringResource(id = R.string.ac),
    iconPainter: Painter = painterResource(id = R.drawable.ic_typ2),
    iconContentDescription: String = stringResource(id = R.string.tableheader_ac_desc)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.UIColorDark))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            modifier = Modifier.size(28.dp),
            painter = iconPainter,
            contentDescription = iconContentDescription,
            alpha = 0.4f
        )
    }
}

@Preview
@Composable
private fun ACPlug() {
    PlugView(
        label = stringResource(id = R.string.ac),
        iconPainter = painterResource(id = R.drawable.ic_typ2),
        iconContentDescription = stringResource(id = R.string.tableheader_ac_desc)
    )
}

@Preview
@Composable
private fun DCPlug() {
    PlugView(
        label = stringResource(id = R.string.dc),
        iconPainter = painterResource(id = R.drawable.ic_ccs),
        iconContentDescription = stringResource(id = R.string.tableheader_dc_desc)
    )
}
