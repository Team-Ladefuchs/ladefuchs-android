package app.ladefuchs.android.dataClasses

data class ChargeCards(
    val identifier: String,
    val name: String,
    val provider: String,
    val price: Float,
    val updated: Long,
    val image: String? = null,
    val url: String? = null,
    val blockingFeeStart:Int,
    val monthlyFee:Float,
    val note:String? = null
)
