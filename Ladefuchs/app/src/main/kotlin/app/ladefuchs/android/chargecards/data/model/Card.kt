package app.ladefuchs.android.chargecards.data.model

/**
 * Mapping the backend model into an internal model to reduce changes on all layers for each backend change.
 */
data class Card(
    val identifier: String,
    val name: String,
    val blockingFeeStart: Int,
    val blockingFee: Float,
    val monthlyFee: Float,
    val note: String,
    val price: Float,
    val image: String,
)
