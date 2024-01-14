package app.ladefuchs.android.ui.chargecards

import app.ladefuchs.android.dataClasses.ChargeCards

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
    val acItems: List<ChargeCards> = emptyList(),
    val dcItems: List<ChargeCards> = emptyList(),
)
