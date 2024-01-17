package app.ladefuchs.android.chargecards.data

import app.ladefuchs.android.chargecards.api.ChargeCardsApi
import app.ladefuchs.android.chargecards.data.model.Card
import app.ladefuchs.android.chargecards.data.model.Operator
import app.ladefuchs.android.chargecards.data.model.PlugType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import app.ladefuchs.android.chargecards.api.entity.PlugType as networkPlugType

class ChargeCardsRepositoryImpl(
    private val chargeCardsApi: ChargeCardsApi,
) : ChargeCardsRepository {
    private var cachedOperators: List<Operator> = emptyList()
    private var cachedCards: HashMap<PlugType, List<Card>> = hashMapOf(
        PlugType.AC to emptyList(),
        PlugType.DC to emptyList()
    )

    override fun getOperators(force: Boolean): Flow<List<Operator>> = flow {
        if (force.not()) {
            // return cached
            emit(cachedOperators)
        }

        cachedOperators = chargeCardsApi.fetchOperators().map {
            Operator(
                identifier = it.identifier,
                displayName = it.displayName,
                types = it.types.map { plugType ->
                    when (plugType) {
                        networkPlugType.AC -> PlugType.AC
                        networkPlugType.DC -> PlugType.DC
                    }
                }
            )
        }

        emit(cachedOperators)
    }.flowOn(Dispatchers.IO)

    override fun getCards(force: Boolean, operatorId: String): Flow<HashMap<PlugType, List<Card>>> =
        flow {
            if (force.not()) {
                // return cached
                emit(cachedCards)
            }

            cachedCards[PlugType.AC] = chargeCardsApi.fetchCards(operatorId, PlugType.AC).map {
                Card(
                    identifier = it.identifier,
                    name = it.name,
                    blockingFeeStart = it.blockingFeeStart,
                    blockingFee = it.blockingFee,
                    monthlyFee = it.monthlyFee,
                    note = it.note,
                    price = it.price,
                    image = it.image ?: "",
                )
            }
            cachedCards[PlugType.DC] = chargeCardsApi.fetchCards(operatorId, PlugType.DC).map {
                Card(
                    identifier = it.identifier,
                    name = it.name,
                    blockingFeeStart = it.blockingFeeStart,
                    blockingFee = it.blockingFee,
                    monthlyFee = it.monthlyFee,
                    note = it.note,
                    price = it.price,
                    image = it.image ?: "",
                )
            }

            emit(cachedCards)
        }.flowOn(Dispatchers.IO)
}
