package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.ladefuchs.android.R

@Composable
fun ChargeCardsTableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.heightIn(60.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        PlugView(
            modifier = Modifier.weight(0.5f),
            label = stringResource(id = R.string.ac),
            iconPainter = painterResource(id = R.drawable.ic_typ2),
            iconContentDescription = stringResource(id = R.string.tableheader_ac_desc)
        )
        Spacer(modifier = Modifier.width(0.5.dp))
        PlugView(
            modifier = Modifier.weight(0.5f),
            label = stringResource(id = R.string.dc),
            iconPainter = painterResource(id = R.drawable.ic_ccs),
            iconContentDescription = stringResource(id = R.string.tableheader_dc_desc)
        )
    }
}

@Preview()
@Composable
private fun TableHeaderPreview() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.UIColorLight))
    ) {
        ChargeCardsTableHeader()
    }
}
