package app.ladefuchs.android.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.ladefuchs.android.R
import app.ladefuchs.android.chargecards.data.model.Card
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.helper.getPriceFormatter

/**
 * Two-column list of charging cards with their fee.
 *
 */
@Composable
fun ChargingCardTable(
    modifier: Modifier = Modifier,
    acItems: List<Card> = emptyList(),
    dcItems: List<Card> = emptyList(),
) {
    Row(
        modifier = modifier.background(Color.White),
        horizontalArrangement = Arrangement.Center
    ) {
        // AC column
        ChargingColumn(ChargeType.AC, acItems)

        // divider
        Spacer(modifier = Modifier.width(1.0.dp))

        // DC column
        ChargingColumn(ChargeType.DC, dcItems)
    }
}

/**
 * Column with charging items
 *
 * Hint: Using experimental API for `animateItemPlacement` which is a custom behaviour when
 * re-ordering the list items when changing the POC Operator
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.ChargingColumn(
    type: ChargeType,
    items: List<Card>
) {
    val priceFormat = getPriceFormatter()

    Column(
        modifier = Modifier
            .fillMaxHeight(0.6f)
            .weight(0.5f)
    ) {
        // Column title
        PlugView(
            modifier = Modifier.fillMaxWidth(),
            label = when (type) {
                ChargeType.AC -> stringResource(id = R.string.ac)
                ChargeType.DC -> stringResource(id = R.string.dc)
            },
            iconPainter = when (type) {
                ChargeType.AC -> painterResource(id = R.drawable.ic_typ2)
                ChargeType.DC -> painterResource(id = R.drawable.ic_ccs)
            },
            iconContentDescription = when (type) {
                ChargeType.AC -> stringResource(id = R.string.tableheader_ac_desc)
                ChargeType.DC -> stringResource(id = R.string.tableheader_dc_desc)
            }
        )

        // List
        Row(
            modifier = Modifier
                .fillMaxHeight(0.4f)
                .weight(0.5f)
                .background(colorResource(id = R.color.TableColorLight))
        ) {
            LazyColumn(
                contentPadding = PaddingValues(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        item.identifier
                    }
                ) { index, item ->
                    ChargeCardWithFee(
                        modifier = Modifier.animateItemPlacement(),
                        useBrightBackground = index % 2 == 0,
                        showIndicator = item.note.isNotEmpty() || item.blockingFee > 0,
                        text = priceFormat.format(item.price).trim { it <= ' ' }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChargingCardTable() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.UIColorLight))
    ) {
        ChargingCardTable(
            acItems = (1..3).map {
                Card(
                    identifier = it.toString(),
                    name = "",
//                    provider = "fo",
                    price = 0.1f,
//                    updated = 1L,
//                    image = "",
//                    url = "",
                    blockingFeeStart = 0,
                    blockingFee = 0.1f,
                    monthlyFee = 0f,
                    note = "",
                    image = "",
                )
            },
            dcItems = (1..10).map {
                Card(
                    identifier = it.toString(),
                    blockingFee = 0.0f,
                    blockingFeeStart = 0,
                    name = "",
                    price = 0.1f,
                    monthlyFee = 0f,
                    note = "",
                    image = "",
                )
            },
        )
    }
}
