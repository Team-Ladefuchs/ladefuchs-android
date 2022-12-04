package app.ladefuchs.android.dataClasses

data class Banner(
    val id: String,
    val link: String,
    val image: String,
    val frequency: Int,
    val updated: Int,
    val isAffiliate: Boolean,
    val filename: String
)