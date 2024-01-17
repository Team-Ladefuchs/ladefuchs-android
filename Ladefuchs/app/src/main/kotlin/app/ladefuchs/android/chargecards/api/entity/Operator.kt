package app.ladefuchs.android.chargecards.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representing the backend model
 */
@Serializable
data class Operator(
    @SerialName("identifier") val identifier: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("types") val types: List<PlugType>,
    @SerialName("updated") val updated: Long? = null,
    @SerialName("image") val image: String? = null
)

@Serializable
enum class PlugType {
    @SerialName("ac") AC,
    @SerialName("dc") DC,
}
