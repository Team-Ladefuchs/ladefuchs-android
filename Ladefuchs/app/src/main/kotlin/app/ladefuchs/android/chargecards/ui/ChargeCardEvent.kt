package app.ladefuchs.android.chargecards.ui

import app.ladefuchs.android.chargecards.data.model.Operator


/**
 * Define how the UI interacts with our viewModel rather than passing many references.
 * Every user interaction should be reflected in an event
 */
sealed interface ChargeCardEvent {
    data object LogoClicked: ChargeCardEvent
    data class SelectOperator(val operator: Operator) : ChargeCardEvent
}
