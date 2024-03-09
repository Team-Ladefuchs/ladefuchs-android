package app.ladefuchs.android.chargecards.api

import app.ladefuchs.android.chargecards.api.entity.Card
import app.ladefuchs.android.chargecards.api.entity.Operator
import app.ladefuchs.android.chargecards.data.model.PlugType
import app.ladefuchs.android.dataClasses.Banner
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ChargeCardsApiImpl(
    private val httpClient: HttpClient,
): ChargeCardsApi {
    override suspend fun fetchOperators(): List<Operator> {
        return httpClient.get(urlString = "/v2/operators/enabled").body()
    }

    override suspend fun fetchCards(pocOperatorId: String, plugType: PlugType): List<Card> {
        return httpClient.get(urlString = "/v2/cards/de/$pocOperatorId/${plugType.name.uppercase()}").body()
    }

    override suspend fun retrieveBanners(): Banner? {
        return httpClient.get(urlString = "/v2/banners").body()
    }
}
