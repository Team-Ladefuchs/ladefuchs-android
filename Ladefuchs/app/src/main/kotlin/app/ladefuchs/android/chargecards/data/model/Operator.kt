package app.ladefuchs.android.chargecards.data.model


data class Operator(
    val identifier: String,
    val displayName: String,
    val types: List<PlugType>
) {
    // override to be used in the wheel picker
    override fun toString(): String = displayName
}
