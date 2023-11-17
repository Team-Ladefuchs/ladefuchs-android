package app.ladefuchs.android.dataClasses

import android.content.Context
import java.nio.file.Path
import java.nio.file.Paths

data class Banner(
    val id: String,
    val link: String,
    val image: String,
    val frequency: Int,
    val updated: Int,
    val isAffiliate: Boolean,
    val filename: String
)

