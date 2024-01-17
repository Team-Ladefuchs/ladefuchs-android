package app.ladefuchs.android.chargecards.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representing the backend model
 */
@Serializable
data class Card(
    @SerialName("identifier") val identifier: String,
    @SerialName("name") val name: String,
    @SerialName("provider") val provider: String,
    @SerialName("price") val price: Float,
    @SerialName("updated") val updated: Long,
    @SerialName("image") val image: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("blockingFeeStart") val blockingFeeStart: Int,
    @SerialName("blockingFee") val blockingFee: Float = 0F,
    @SerialName("monthlyFee") val monthlyFee: Float = 0F,
    @SerialName("note") val note: String = "",
    @SerialName("msp") val msp: String? = null
)
