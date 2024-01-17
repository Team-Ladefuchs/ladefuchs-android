package app.ladefuchs.android.helper

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.widget.*
import androidx.navigation.NavController
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import java.io.File
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Semaphore
import kotlin.math.min
import kotlin.math.roundToInt


private val pricesSemaphore = Semaphore(1)

var currentDialog: Dialog? = null

/**
Function to log while in Debug mode
 */
fun printLog(message: String, type: String = "info") {
    if (BuildConfig.DEBUG) {
        val logPrefix = "🦊"
        var typeIcon = "ℹ️"
        when (type) {
            "error" -> {
                typeIcon = "🛑"
            }

            "warning" -> {
                typeIcon = "⚠️"
            }

            "heart" -> {
                typeIcon = "❤️"
            }

            "network" -> {
                typeIcon = "⏬️"
            }
        }
        println("$logPrefix $typeIcon $message")
    }
}


/**
 * Returns the screens width
 */
fun getScreenWidth(): Int {
    return Resources.getSystem().displayMetrics.widthPixels
}

/**
 * This function does some fancy color manipulation god knows what for
 */
fun manipulateColor(color: Int, factor: Float): Int {
    val a = Color.alpha(color)
    val r = (Color.red(color) * factor).roundToInt()
    val g = (Color.green(color) * factor).roundToInt()
    val b = (Color.blue(color) * factor).roundToInt()
    return Color.argb(
        a,
        min(r, 255),
        min(g, 255),
        min(b, 255)
    )
}

/**
 * This function retrieves the prices for a specific operator
 */
fun getPricesByOperatorId(
    pocOperator: Operator,
    context: Context,
    view: View,
    forceDownload: Boolean = false,
): Boolean {
    //Load Prices JSON from File
    pricesSemaphore.acquire();
    printLog("Getting prices for $pocOperator")
    val (_, acCards, dcCards) = retrieveCards(
        pocOperator.identifier,
        context,
        forceDownload
    )
    printLog("Re-Filling Cards for $pocOperator")

    val allCards = mapOf(
        ChargeType.AC to acCards,
        ChargeType.DC to dcCards
    )
    val needsRefresh = fillCards(
        pocOperator,
        allCards,
        view,
    )
    pricesSemaphore.release()
    return needsRefresh
}

fun getImagePath(imageUrl: URL, context: Context): File {
    val cardChecksum = imageUrl.path.substring(imageUrl.path.lastIndexOf('/') + 1)
    return File("${context.filesDir}/image_$cardChecksum")
}

fun cleanupOldImageFiles(context: Context) {
    if (isOffline(context)) {
        return
    }

    printLog("Execute cleanupOldImageFiles")

    val folder = context.filesDir
    if (folder.exists() && folder.isDirectory) {
        val files = folder.listFiles()
        try {
            val currentTime = Instant.now()

            files?.filter { file ->
                file != null &&
                        file.isFile &&
                        file.name.startsWith("image_")
            }?.forEach { file ->
                val lastModifiedTime = Instant.ofEpochMilli(file.lastModified())
                val daysDifference = ChronoUnit.DAYS.between(lastModifiedTime, currentTime)

                if (daysDifference > 100) {
                    if (file.delete()) {
                        printLog("Deleted file: ${file.name}", "error")
                    } else {
                        printLog("Failed to delete file: ${file.name}", "error")
                    }
                }
            }

        } catch (e: Exception) {
            printLog("Couldn't cleanup old PNG files: ${e.message}", "error")
        }
    } else {
        println("Folder does not exist or is not a directory.")
    }
}


private fun TextView.removeLinksUnderline() {
    val spannable = SpannableString(text)
    for (u in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
        spannable.setSpan(object : URLSpan(u.url) {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, spannable.getSpanStart(u), spannable.getSpanEnd(u), 0)
    }
    text = spannable
}


@SuppressLint("SetTextI18n")
fun settingsPopUpSetUp(view: View) {

    val acknowledgementText = view.findViewById(R.id.acknowledgement_text) as TextView
    acknowledgementText.movementMethod = LinkMovementMethod.getInstance()

    val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        view.context.packageManager
            .getPackageInfo(
                view.context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName
    } else {
        view.context.packageManager
            .getPackageInfo(view.context.packageName, 0).versionName
    }
    val versionHolder: TextView = view.findViewById(R.id.version_info)
    versionHolder.text = "Ladefuchs Version $versionName"

    // Making Links in Textviews Clickable... well... really...
    val schlingelSL2 = view.findViewById(R.id.bastiSL2) as TextView
    schlingelSL2.movementMethod = LinkMovementMethod.getInstance()
    schlingelSL2.removeLinksUnderline()
    val schlingelSL3 = view.findViewById(R.id.bastiSL3) as TextView
    schlingelSL3.removeLinksUnderline()

    val malikSL2 = view.findViewById(R.id.malikSL2) as TextView
    malikSL2.movementMethod = LinkMovementMethod.getInstance()
    malikSL2.removeLinksUnderline()
    val malikSL3 = view.findViewById(R.id.malikSL3) as TextView
    malikSL3.removeLinksUnderline()

    val flowinhoSL2 = view.findViewById(R.id.flowinhoSL2) as TextView
    flowinhoSL2.movementMethod = LinkMovementMethod.getInstance()
    flowinhoSL2.removeLinksUnderline()
    val flowinhoSL3 = view.findViewById(R.id.flowinhoSL3) as TextView
    flowinhoSL3.removeLinksUnderline()

    val thorstenSL2 = view.findViewById(R.id.thorstenSL2) as TextView
    thorstenSL2.movementMethod = LinkMovementMethod.getInstance()
    thorstenSL2.removeLinksUnderline()
    val thorstenSL3 = view.findViewById(R.id.thorstenSL3) as TextView
    thorstenSL3.removeLinksUnderline()

    val dominicSL2 = view.findViewById(R.id.dominicSL2) as TextView
    dominicSL2.movementMethod = LinkMovementMethod.getInstance()
    dominicSL2.removeLinksUnderline()

    val roddiSL2 = view.findViewById(R.id.roddiSL2) as TextView
    roddiSL2.movementMethod = LinkMovementMethod.getInstance()
    roddiSL2.removeLinksUnderline()
    val roddiSL3 = view.findViewById(R.id.roddiSL3) as TextView
    roddiSL3.removeLinksUnderline()

    val illuSL2 = view.findViewById(R.id.illufuchsSL) as TextView
    illuSL2.movementMethod = LinkMovementMethod.getInstance()
    illuSL2.removeLinksUnderline()

    // On Click Listeners for Images
    val chargePriceLogo = view.findViewById(R.id.chargeprice_logo) as ImageView
    chargePriceLogo.setOnClickListener {
        opeLinkInBrowser(it.tag.toString(), view.context)
    }

    val audiodDumpLogo = view.findViewById(R.id.podcast_audiodump) as ImageView
    audiodDumpLogo.setOnClickListener {
        opeLinkInBrowser(it.tag.toString(), view.context)
    }

    val bitsundsoLogo = view.findViewById(R.id.podcast_bitsundso) as ImageView
    bitsundsoLogo.setOnClickListener {
        opeLinkInBrowser(it.tag.toString(), view.context)
    }
}

fun opeLinkInBrowser(url: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

fun getPriceFormatter(): NumberFormat {
    val priceNumberFormat = NumberFormat.getCurrencyInstance()
    val decimalFormatSymbols: DecimalFormatSymbols =
        (priceNumberFormat as DecimalFormat).decimalFormatSymbols
    decimalFormatSymbols.currencySymbol = ""
    priceNumberFormat.decimalFormatSymbols = decimalFormatSymbols
    return priceNumberFormat
}

