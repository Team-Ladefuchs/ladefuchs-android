package app.ladefuchs.android.chargecards.domain

import app.ladefuchs.android.chargecards.data.ChargeCardsRepository
import app.ladefuchs.android.chargecards.data.model.Operator
import kotlinx.coroutines.flow.Flow

/**
 * Retrieve a list of operators from the backend or return a static list from a file if the device is offline.
 */
class GetOperatorsUseCase(
    private val chargeCardsRepository: ChargeCardsRepository,
) {
    operator fun invoke(force: Boolean = false): Flow<List<Operator>> {
        return chargeCardsRepository.getOperators(force = force)
    }
}
