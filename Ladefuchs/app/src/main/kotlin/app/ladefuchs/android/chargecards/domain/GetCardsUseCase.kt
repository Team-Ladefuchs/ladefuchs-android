package app.ladefuchs.android.chargecards.domain

import app.ladefuchs.android.chargecards.api.entity.Operator
import app.ladefuchs.android.chargecards.data.ChargeCardsRepository
import app.ladefuchs.android.chargecards.data.model.Card
import app.ladefuchs.android.chargecards.data.model.PlugType
import kotlinx.coroutines.flow.Flow

/**
 * Retrieve a list of operators from the backend or return a static list from a file if the device is offline.
 */
class GetCardsUseCase(
    private val chargeCardsRepository: ChargeCardsRepository,
) {
    operator fun invoke(
        force: Boolean = false,
        operatorId: String,
    ): Flow<HashMap<PlugType, List<Card>>> {
        return chargeCardsRepository.getCards(
            force = force,
            operatorId = operatorId
        )
    }
}
