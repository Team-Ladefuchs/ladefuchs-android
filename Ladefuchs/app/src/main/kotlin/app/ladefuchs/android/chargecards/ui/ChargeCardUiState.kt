package app.ladefuchs.android.chargecards.ui

import app.ladefuchs.android.chargecards.data.model.Card
import app.ladefuchs.android.chargecards.data.model.Operator

/**
 * Single Source of Truth (SSOT) for the charging card screen.
 *
 * By using unidirectional data-flow, all interactions with the UI will always update the
 * properties within this state which in turn triggers a recomposition.
 */
data class ChargeCardUiState(
    val isLoading: Boolean = true,
    val onboardingFinished: Boolean = false,
    val easterEggCount: Int = 0,
    val isEasterEggEnabled: Boolean = true,
    val pocOperators: List<Operator> = emptyList(),
    val acItems: List<Card> = emptyList(),
    val dcItems: List<Card> = emptyList(),
)
