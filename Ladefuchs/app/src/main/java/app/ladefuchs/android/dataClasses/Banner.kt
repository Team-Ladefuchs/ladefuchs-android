package app.ladefuchs.android.dataClasses

import com.beust.klaxon.Json

sealed class Banner(
    open val image: String,
    open val link: String,
) {
    data class FuchsBanner(
        @Json(name = "affiliateLinkUrl")
        override val link: String,
        @Json(name = "identifier")
        val id: String,
        @Json(name = "imageUrl")
        override val image: String,
        val frequency: Int,
        val isAffiliate: Boolean,
        val lastUpdatedDate: String,
    ): Banner(image, link)
    data class ChargePriceBanner(
        @Json(name = "imageUrl")
        override val image: String,
        @Json(name = "affiliateLinkUrl")
        override val link: String,
    ): Banner(image, link)
}
