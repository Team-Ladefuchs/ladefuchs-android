package app.ladefuchs.android.chargecards.api

import app.ladefuchs.android.chargecards.api.entity.Card
import app.ladefuchs.android.chargecards.api.entity.Operator
import app.ladefuchs.android.chargecards.data.model.PlugType
import app.ladefuchs.android.dataClasses.Banner

interface ChargeCardsApi {
    suspend fun fetchOperators(): List<Operator>

    suspend fun fetchCards(pocOperatorId: String, plugType: PlugType): List<Card>

    suspend fun retrieveBanners(): Banner?
}
