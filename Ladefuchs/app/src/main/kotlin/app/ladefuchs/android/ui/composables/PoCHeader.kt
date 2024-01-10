package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.ladefuchs.android.R
import java.util.Locale

@Composable
fun PoCHeader(
    modifier: Modifier = Modifier
) {
    Text(
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.UIColorDark))
            .padding(top = 10.dp, bottom = 12.dp),
        text = stringResource(id = R.string.pocHeader).uppercase(Locale.US),
        fontWeight = FontWeight.Bold,
    )
}

@Preview
@Composable
fun PoCHeader() {
    PoCHeader(modifier = Modifier)
}
