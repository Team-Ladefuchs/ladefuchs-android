package app.ladefuchs.android.dataClasses

data class AllCardsResponse(
    val operator: String,
    var ac: List<ChargeCards>,
    var dc: List<ChargeCards>
)
