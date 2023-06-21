package app.ladefuchs.android.helper

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import androidx.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.CardMetaData
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import com.beust.klaxon.Klaxon
import java.io.File
import java.net.URL
import java.util.concurrent.Semaphore
import kotlin.math.min
import kotlin.math.roundToInt

private val pricesSemaphore = Semaphore(1)
var currentPopup: PopupWindow? = null

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
 * Stores a file in internal storage
 */
fun storeFileInInternalStorage(
    input: String,
    internalStorageFileName: String,
    context: Context
) {
    if (input.isEmpty()) {
        return
    }
    try {
        val file = File(context.getFileStreamPath(internalStorageFileName).toString())
        file.writeText(input)
        printLog("Writing File: $internalStorageFileName to ${context.filesDir}");
    } catch (e: Exception) {
        printLog("could not save file $internalStorageFileName", "error")
        e.printStackTrace()
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
 * A function to safely open other pages
 */
fun NavController.safeNavigate(actionId: Int) {
    currentDestination?.getAction(actionId)?.run { navigate(actionId) }
}

/**
 * This function retrieves the prices for a specific operator
 */
fun getPricesByOperatorId(
    pocOperator: Operator,
    context: Context,
    api: API,
    view: View,
    resources: Resources,
    forceDownload: Boolean = false,
): Boolean {
    //Load Prices JSON from File
    pricesSemaphore.acquire();
    printLog("Getting prices for $pocOperator")
    val (_, acCards, dcCards) = api.retrieveCards(
        pocOperator.identifier,
        forceDownload
    )
    printLog("Re-Filling Cards for $pocOperator")
    val maxListLength = maxOf(acCards.size, dcCards.size)
    pricesSemaphore.release()
    return fillCards(pocOperator, acCards, dcCards, maxListLength, context, view, api, resources)

}

fun readCardMetadata(context: Context): List<CardMetaData>? {
    //Load Metadata JSON from File
    printLog("Reading de-card_metadata.json")
    val cardMetadata = context.assets?.open("de-card_metadata.json")?.let {
        Klaxon().parseArray<CardMetaData>(
            it
        )
    }
    return cardMetadata
}

fun getImagePath(cardUri: URL, context: Context, cpo: Boolean = false): File {
    val cardChecksum = cardUri.path.substring(cardUri.path.lastIndexOf('/') + 1)
    return File("${context.filesDir}/${if (cpo) "card" else "cpo"}_${cardChecksum}.jpg")
}

fun createPopup(
    view: View,
    currentCard: ChargeCards,
    chargeCardsAC: List<ChargeCards>,
    chargeCardsDC: List<ChargeCards>,
    currentType: ChargeType,
    cardImageDrawable: Drawable?,
    cardBitmap: Bitmap?,
    operator: Operator,
    api: API,
    context: Context
) {
    // inflate the layout of the popup window

    currentPopup?.dismiss()

    val inflater =
        view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val popupView: View =
        inflater.inflate(R.layout.card_detail_dialog, null)

    // create the popup window
    val width = view.context.resources.displayMetrics.widthPixels
    val statusbarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insets = view.rootWindowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        insets.top
    } else {
        val statusbarResId =
            context.resources?.getIdentifier("status_bar_height", "dimen", "android")
        if (statusbarResId != null) context.resources?.getDimensionPixelSize(statusbarResId)!! else 0
    }
    printLog("StatusbarHeight is: ${statusbarHeight}")
    val height =
        view.context.resources.displayMetrics.heightPixels - if (statusbarHeight > 110) 0 else 110
    val focusable = true // lets taps outside the popup also dismiss it
    currentPopup = PopupWindow(popupView, width, height, focusable)
    currentPopup?.isOutsideTouchable = false
    currentPopup?.animationStyle = R.style.popup_window_animation;
    // show the popup window
    currentPopup?.showAtLocation(view, Gravity.BOTTOM, 0, 0)

    // set onClick Listeners for backButtons
    popupView.findViewById<ImageButton>(R.id.back_button)
        .setOnClickListener {
            currentPopup?.dismiss()
        }

    // Retrieve and set Operator Image

    // Set card Image
    popupView.findViewById<ImageView>(R.id.card_logo).background =
        if (cardImageDrawable !== null) cardImageDrawable else BitmapDrawable(
            context.resources,
            cardBitmap
        )
    // Set Card Details
    val currentCardAc: ChargeCards? =
        if (currentType == ChargeType.AC) currentCard else chargeCardsAC.find { it.identifier == currentCard.identifier }
    val currentCardDc: ChargeCards? =
        if (currentType == ChargeType.DC) currentCard else chargeCardsDC.find { it.identifier == currentCard.identifier }
    popupView.findViewById<TextView>(R.id.detail_header1).text =
        currentCard.name
    popupView.findViewById<TextView>(R.id.detail_header2).text =
        currentCard.provider
    if (currentCardAc !== null) {
        popupView.findViewById<TextView>(R.id.priceAC).text =
            java.text.DecimalFormat("#,##0.00").format(currentCardAc.price)
        popupView.findViewById<TextView>(R.id.blockFeeAC).text =
            "> ab Min. ${currentCardAc.blockingFeeStart}\n> ${currentCardAc.blockingFee} € /Min."
        popupView.findViewById<TextView>(R.id.monthlyFeeContent).text =
            if (currentCardAc.monthlyFee == 0.0f) "keine" else "${currentCardAc.monthlyFee} €"
        if (currentCardAc.blockingFee == 0.0f)
            popupView.findViewById<ImageView>(R.id.huetchen_ac).visibility = View.GONE
        else
            popupView.findViewById<ImageView>(R.id.huetchen_ac).visibility = View.VISIBLE
    }
    if (currentCardDc !== null) {
        popupView.findViewById<TextView>(R.id.priceDC).text =
            java.text.DecimalFormat("#,##0.00").format(currentCardDc.price)
        popupView.findViewById<TextView>(R.id.blockFeeDC).text =
            "> ab Min. ${currentCardDc.blockingFeeStart}\n> ${currentCardDc.blockingFee} € /Min."
        popupView.findViewById<TextView>(R.id.monthlyFeeContent).text =
            if (currentCardDc.monthlyFee == 0.0f) "keine" else "${currentCardDc.monthlyFee} €"
        if (currentCardDc.blockingFee == 0.0f)
            popupView.findViewById<ImageView>(R.id.huetchen_dc).visibility = View.GONE
        else
            popupView.findViewById<ImageView>(R.id.huetchen_dc).visibility = View.VISIBLE
    }

    if (!currentCard.note.isNullOrEmpty()) {
        popupView.findViewById<ConstraintLayout>(R.id.notes).visibility = View.VISIBLE
        popupView.findViewById<ImageView>(R.id.huetchenNotes).visibility = View.VISIBLE
        popupView.findViewById<TextView>(R.id.notesText).text = currentCard.note;
    }

    popupView.findViewById<Button>(R.id.getCard).setOnClickListener {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(currentCard.url.toString())
        )
        context.startActivity(urlIntent)
    }

    if (currentCard.url == null) {
        popupView.findViewById<Button>(R.id.getCard).visibility = View.INVISIBLE
    }

    // Retrieve Operator Image
    var operatorImage: Drawable? = null
    if (!operator.image.isNullOrEmpty()) {
        val imgPath = getImagePath(URL(operator.image), context, true)
        if (!imgPath.exists())
            api.downloadImageToInternalStorage(imageURL = URL(operator.image), cpo = true)
        try {
            operatorImage = Drawable.createFromPath(imgPath.absolutePath)!!
        } catch (e: Exception) {
            //Download was to slow or failed no need for error handling because generated image will be used
        }
    }
    // creating an own image
    if (operatorImage == null) {
        // TODO add text to placeholder
        operatorImage = AppCompatResources.getDrawable(context, R.drawable.cpo_generic)
    }
    popupView.findViewById<ImageView>(R.id.cpo_logo).setImageDrawable(operatorImage)
}