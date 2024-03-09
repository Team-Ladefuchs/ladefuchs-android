package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ladefuchs.android.R
import app.ladefuchs.android.ui.bitterFontFamily

/**
 * Text phrase used for advertisement-like information written in a custom font
 */
@Composable
fun Phrase(
    modifier: Modifier = Modifier,
    phrase: String
) {
    Text(
        modifier = modifier
            .background(colorResource(id = R.color.UIColorDark))
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        text = phrase,
        fontSize = 18.sp,
        fontFamily = bitterFontFamily,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun Phrase() {
    Phrase(phrase = "Hier könnte Ihre Werbung stehen.")
}
