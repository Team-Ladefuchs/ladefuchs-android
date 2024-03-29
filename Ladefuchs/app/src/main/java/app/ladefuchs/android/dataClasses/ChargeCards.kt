package app.ladefuchs.android.dataClasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChargeCards(
    val identifier: String,
    val name: String,
    val provider: String,
    val price: Float,
    val updated: Long,
    val image: String? = null,
    val url: String? = null,
    val blockingFeeStart: Int,
    val blockingFee: Float = 0F,
    val monthlyFee: Float = 0F,
    val note: String = "",
    val msp: String? = null
) : Parcelable
