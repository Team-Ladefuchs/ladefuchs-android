package app.ladefuchs.android.ui.chargecards

/**
 * Define how the UI interacts with our viewModel rather than passing many references.
 * Every user interaction should be reflected in an event
 */
sealed interface ChargeCardEvent {
    data object LogoClicked: ChargeCardEvent
}
