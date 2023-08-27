package app.ladefuchs.android.helper

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.TextPaint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import java.io.File
import java.net.URL
import kotlin.math.ceil


private var cardWidth: Int = getScreenWidth() / 5
private var cardHeight: Int = 176 * cardWidth / 280

private const val cardMarginLeft: Int = 50
private const val cardMarginRight: Int = 50
private const val cardMarginTop: Int = 30
private const val cardMarginBottom: Int = 30
private const val globalCornerRadius: Float = 15F

/**
 * This function will draw the scrollable list of chargecards
 */
fun drawChargeCard(
    textToDraw: String = "N/A",
    textSize: Float = 8F,
    textColor: Int = Color.BLACK,
    rectangleColor: Int = Color.LTGRAY,
    backgroundColor: Int = Color.WHITE,
    paintStroke: Boolean = false
): Bitmap? {
    val scaleFactor = 2
    val cornerRadius = globalCornerRadius * scaleFactor
    val strokeWidth = 3F * scaleFactor
    val textCardWidth = cardWidth * scaleFactor
    val textCardHeight = cardHeight * scaleFactor
    val cardTextSize = textSize * scaleFactor

    val bitmap = Bitmap.createBitmap(
        textCardWidth,
        textCardHeight,
        Bitmap.Config.ARGB_8888
    )

    // canvas to drawing
    val canvas = Canvas(bitmap)

    // draw rounded rectangle on canvas
    val rectF = RectF(
        strokeWidth,
        strokeWidth,
        canvas.width - strokeWidth,
        canvas.height - strokeWidth
    )

    if (paintStroke) {
        canvas.drawRoundRect(
            rectF,
            cornerRadius,
            cornerRadius,
            Paint().apply { color = rectangleColor })
    }

    val fillPaint = Paint()
    val strokePaint = Paint()

    // fill
    fillPaint.style = Paint.Style.FILL
    fillPaint.color = manipulateColor(backgroundColor, 1.25f)

    // stroke
    strokePaint.style = Paint.Style.STROKE
    strokePaint.color = rectangleColor
    strokePaint.strokeWidth = strokeWidth

    // Second rectangle
    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, fillPaint)    // fill
    if (paintStroke) {
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)  // stroke
    }

    // text paint to draw text
    val textPaint = TextPaint().apply {
        color = textColor
        textAlign = Paint.Align.LEFT
        this.textSize = cardTextSize - strokeWidth / scaleFactor
        isAntiAlias = true
    }
    textPaint.typeface = Typeface.create("Roboto", Typeface.BOLD)

    //calculating if breaking is needed in order to correctly position text on the Y Axis
    val numOfChars = textPaint.breakText(
        textToDraw,
        true,
        rectF.width(),
        null
    )

    val textLines = ceil(textToDraw.length.toDouble() / numOfChars.toDouble())

    val textPositionYOffset =
        canvas.height.toDouble() / 2.0 - (textLines * cardTextSize.toDouble() / 2.0 + 2.0 * strokeWidth.toDouble())

    // draw multiline card
    canvas.drawMultilineText(
        textToDraw,
        textPaint,
        textCardWidth,
        (textCardWidth / 7 - 12).toFloat(),
        textPositionYOffset.toFloat()
    )

    return bitmap
}

/**
 * This function will fill a chargecard with its content
 */

fun fillCards(
    operator: Operator,
    allChargeCards: Map<ChargeType, List<ChargeCards>>,
    parentView: View,
): Boolean {
    var cardsDownloaded = false
    val priceFormat = getPriceFormatter()
    var columnIsEven = false
    listOf(ChargeType.AC, ChargeType.DC).forEach { chargeType ->
        columnIsEven = true
        printLog("Filling cards for $chargeType")
        val column: LinearLayout = when (chargeType) {
            ChargeType.AC -> parentView.findViewById(R.id.chargeCardsTableHolderAC)
            ChargeType.DC -> parentView.findViewById(R.id.chargeCardsTableHolderDC)
        }
        column.removeAllViews()
        val chargeCards = allChargeCards.getOrDefault(chargeType, emptyList())
        chargeCards.forEach cards@{ currentCard ->
            val cell = createSingleCell(parentView.context, chargeType, columnIsEven)
            columnIsEven = !columnIsEven

            val cardView = CardView(parentView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    cardWidth, cardHeight
                ).apply {
                    setMargins(cardMarginLeft, cardMarginTop, cardMarginRight, cardMarginBottom)
                }
                cardElevation = 20F
                radius = 15F
                setCardBackgroundColor(Color.WHITE)
            }

            val frameLayout = FrameLayout(parentView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            frameLayout.addView(cardView)

            val imageView = ImageView(parentView.context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                setBackgroundColor(Color.WHITE)
                layoutParams = ViewGroup.LayoutParams(cardWidth, cardHeight)
            }
            cardView.addView(imageView)

            if (currentCard.note.isNotEmpty() || currentCard.blockingFee > 0) {
                addHuetchen(parentView.context, cardView, frameLayout)
            }

            cell.addView(frameLayout)
            val (imageDrawable, downloaded) = downloadOrGetImageDrawable(
                currentCard.image,
                parentView.context
            )
            cardsDownloaded = downloaded
            if (imageDrawable != null) {
                imageView.setImageDrawable(imageDrawable)
            } else {
                var cardText = currentCard.name
                if (currentCard.provider != currentCard.name) {
                    cardText = currentCard.provider
                }
                val cardBitmap = drawChargeCard(
                    textToDraw = cardText,
                    textSize = 29F,
                )
                imageView.background = BitmapDrawable(parentView.context.resources, cardBitmap)
                imageView.outlineProvider = OutlineProvider(10, 10)
            }

            cell.setOnClickListener { view ->
                val currentCardAc: ChargeCards? =
                    if (chargeType == ChargeType.AC) currentCard else allChargeCards[ChargeType.AC]?.find { it.identifier == currentCard.identifier }
                val currentCardDc: ChargeCards? =
                    if (chargeType == ChargeType.DC) currentCard else allChargeCards[ChargeType.DC]?.find { it.identifier == currentCard.identifier }

                createCardDetailPopup(
                    view,
                    currentCard = currentCard,
                    currentCardAc = currentCardAc,
                    currentCardDc = currentCardDc,
                    operator,
                )
            }

            val textviewPrice = TextView(parentView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setTextAppearance(R.style.TableTextView)
                }
                text = priceFormat.format(currentCard.price).trim { it <= ' ' }
            }
            cell.addView(textviewPrice)
            column.addView(cell)
        }
    }

    fillEmptyCells(allChargeCards, parentView, columnIsEven)
    return cardsDownloaded
}

private fun fillEmptyCells(
    allChargeCards: Map<ChargeType, List<ChargeCards>>,
    parentView: View,
    columnIsEven: Boolean
) {
    var columnIsEven1 = columnIsEven
    val acCardCount = allChargeCards[ChargeType.AC]?.size ?: 0
    val dcCardCount = allChargeCards[ChargeType.DC]?.size ?: 0

    val pair: Pair<ChargeType?, Int> = if (acCardCount > dcCardCount) {
        Pair(ChargeType.DC, acCardCount - dcCardCount)
    } else if (acCardCount < dcCardCount) {
        Pair(ChargeType.AC, dcCardCount - acCardCount)
    } else {
        Pair(null, 0)
    }
    val diff = pair.second
    if (pair.first != null && pair.second > 0) {
        val column: LinearLayout = when (pair.first!!) {
            ChargeType.AC -> parentView.findViewById(R.id.chargeCardsTableHolderAC)
            ChargeType.DC -> parentView.findViewById(R.id.chargeCardsTableHolderDC)
        }
        for (i in 0 until diff) {
            val cell = createSingleCell(parentView.context, pair.first!!, columnIsEven1)
            columnIsEven1 = !columnIsEven1
            val textView = TextView(parentView.context).apply {
                text = ""
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    cardHeight + 60
                )
            }
            textView.setPadding(
                cardMarginLeft,
                cardMarginTop,
                cardMarginRight,
                cardMarginBottom
            )
            cell.addView(textView)
            column.addView(cell)
        }
    }
}

private fun addHuetchen(
    context: Context,
    cardView: CardView,
    frameLayout: FrameLayout
) {
    val redImageView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(50, 50).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 26, 46, 0)
        }
        setImageResource(R.drawable.huetchen)
        elevation = cardView.cardElevation + 4F
    }

    val shadowImage = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(53, 53).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 26, 46, 0)
        }
        setImageResource(R.drawable.shadow)
        elevation = cardView.cardElevation + 2F
    }
    frameLayout.addView(shadowImage)
    frameLayout.addView(redImageView)
}

private fun createSingleCell(
    context: Context,
    chargeType: ChargeType,
    isEven: Boolean
): LinearLayout {
    val cell = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
    }

    cell.background = when (chargeType) {
        ChargeType.AC -> {
            if (isEven) {
                ContextCompat.getDrawable(context, R.drawable.border_dark_bg_left)
            } else {
                ContextCompat.getDrawable(context, R.drawable.border_light_bg_left)
            }
        }

        ChargeType.DC -> if (isEven) {
            ContextCompat.getDrawable(context, R.drawable.border_dark_bg_right)
        } else {
            ContextCompat.getDrawable(context, R.drawable.border_light_bg_right)
        }
    }

    return cell
}

fun createAboutPopup(context: Context, view: View) {
    currentDialog?.dismiss()

    val popUpView: View = LayoutInflater.from(context).inflate(R.layout.fragment_about, null)
    currentDialog = createDialog(popUpView, view)
    currentDialog?.show()

    popUpView.findViewById<ImageButton>(R.id.back_button)
        .setOnClickListener {
            currentDialog?.dismiss()
        }

    aboutPopUpSetUp(popUpView)
}

@SuppressLint("SetTextI18n")
fun createCardDetailPopup(
    view: View,
    currentCard: ChargeCards,
    currentCardAc: ChargeCards?,
    currentCardDc: ChargeCards?,
    operator: Operator,
) {
    currentDialog?.dismiss()

    val overlayView = View(view.context)
    overlayView.setBackgroundColor(Color.parseColor("#80000000"))
    val params = view.layoutParams
    val parentViewGroup = view.parent as ViewGroup
    parentViewGroup.addView(overlayView, params)
    val inflater =
        view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val popupView: View =
        inflater.inflate(R.layout.card_detail_dialog, null)

    currentDialog = createDialog(popupView, view)
    currentDialog?.show()
    // set onClick Listeners for backButtons
    popupView.findViewById<ImageButton>(R.id.back_button)
        .setOnClickListener {
            currentDialog?.dismiss()
        }

    // Set card Image
    val (image, _) = downloadOrGetImageDrawable(currentCard.image, view.context)
    if (image != null) {
        val imageView = popupView.findViewById<ImageView>(R.id.card_logo)
        imageView.setImageDrawable(image)
        imageView.scaleType = ImageView.ScaleType.FIT_XY;
    }
    // Set Card Details
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

    if (currentCard.note.isNotEmpty()) {
        popupView.findViewById<ConstraintLayout>(R.id.notes).visibility = View.VISIBLE
        popupView.findViewById<ImageView>(R.id.huetchenNotes).visibility = View.VISIBLE
        popupView.findViewById<TextView>(R.id.notesText).text = currentCard.note;
    }

    popupView.findViewById<Button>(R.id.getCard).setOnClickListener {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(currentCard.url.toString())
        )
        view.context.startActivity(urlIntent)
    }

    if (currentCard.url == null) {
        popupView.findViewById<Button>(R.id.getCard).visibility = View.INVISIBLE
    }

    // retrieve operator image
    val (operatorImage, _) = downloadOrGetImageDrawable(operator.image, view.context)
    val operatorImageView = popupView.findViewById<ImageView>(R.id.cpo_logo)

    if (operatorImage == null) {
        // set operator placeholder image
        operatorImageView.setImageDrawable(
            AppCompatResources.getDrawable(
                view.context,
                R.drawable.cpo_generic
            )
        )
    } else {
        // set operator image
        operatorImageView.setImageDrawable(operatorImage)
    }

}

@SuppressLint("DiscouragedApi")
private fun createDialog(
    dialogView: View,
    anchorView: View,
): Dialog {

    val statusBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insets = anchorView.rootWindowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        insets.top
    } else {
        val resourceId =
            dialogView.context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId != 0) {
            dialogView.context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    val height =
        anchorView.context.resources.displayMetrics.heightPixels - if (statusBarHeight > 110) 0 else 110
    val width = anchorView.context.resources.displayMetrics.widthPixels

    val dialog = Dialog(dialogView.context)
    (dialogView.parent as? ViewGroup)?.removeView(dialogView) // Remove view from its current parent
    dialog.setContentView(dialogView)
    dialog.window?.setLayout(width, height)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.attributes?.windowAnimations = R.style.popup_window_animation

    return dialog
}


fun downloadOrGetImageDrawable(imageUriStr: String?, context: Context): Pair<Drawable?, Boolean> {

    if (imageUriStr.isNullOrEmpty()) {
        return null to false
    }

    val uri = URL(imageUriStr)
    var imagePath: File? = getImagePath(uri, context)

    if (imagePath?.exists() == false) {
        downloadImageToInternalStorage(
            uri,
            context
        )
    }

    if (imagePath?.exists() == true) {
        try {
            imagePath = getImagePath(uri, context)
            return Drawable.createFromPath(imagePath.absolutePath) to false
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }
    return null to false
}

