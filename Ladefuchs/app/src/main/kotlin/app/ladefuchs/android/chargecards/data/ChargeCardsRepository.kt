package app.ladefuchs.android.chargecards.data

import app.ladefuchs.android.chargecards.data.model.Card
import app.ladefuchs.android.chargecards.data.model.Operator
import app.ladefuchs.android.chargecards.data.model.PlugType
import kotlinx.coroutines.flow.Flow

interface ChargeCardsRepository {
    fun getOperators(force: Boolean = false): Flow<List<Operator>>

    fun getCards(force: Boolean = false, operatorId: String): Flow<HashMap<PlugType, List<Card>>>
}
