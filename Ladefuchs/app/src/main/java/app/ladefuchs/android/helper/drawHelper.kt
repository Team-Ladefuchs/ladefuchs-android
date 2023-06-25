package app.ladefuchs.android.helper

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import java.io.File
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
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
    context: Context,
    parentView: View,
    api: API,
): Boolean {
    val types = listOf(ChargeType.AC, ChargeType.DC)
    var cardsDownloaded = false
    val priceFormat = getPriceFormatter()
    var columnIsEven = false
    types.forEach { chargeType ->
        columnIsEven = true
        printLog("Filling cards for $chargeType")
        val column: LinearLayout =
            parentView.findViewById(
                context.resources.getIdentifier(
                    "chargeCardsTableHolder${chargeType.toString().uppercase()}",
                    "id",
                    context.packageName
                )
            ) ?: return false;
        column.removeAllViews()

        val chargeCards = allChargeCards.getOrDefault(chargeType, emptyList())
        chargeCards.forEach cards@{ currentCard ->
            val cell = createSingleCell(context, chargeType, columnIsEven)
            columnIsEven = !columnIsEven

            val cardView = CardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    cardWidth, cardHeight
                ).apply {
                    setMargins(cardMarginLeft, cardMarginTop, cardMarginRight, cardMarginBottom)
                }
                cardElevation = 20F
                radius = 15F
                setCardBackgroundColor(Color.WHITE)
            }

            val frameLayout = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            frameLayout.addView(cardView)

            val imageView = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                setBackgroundColor(Color.WHITE)
                layoutParams = ViewGroup.LayoutParams(cardWidth, cardHeight)
            }
            cardView.addView(imageView)


            if (currentCard.note.isNotEmpty() || currentCard.blockingFee > 0) {
                addHuedchen(context, cardView, frameLayout)
            }

            cell.addView(frameLayout)
            val (imageDrawable, downloaded) = getCardImageDrawable(currentCard, api, context)
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
                    textColor = Color.BLACK,
                    backgroundColor = Color.WHITE,
                )
                imageView.background = BitmapDrawable(context.resources, cardBitmap)
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
                    api,
                    context
                )
            }

            val textviewPrice = TextView(context).apply {
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
        val column: LinearLayout =
            parentView.findViewById(
                context.resources.getIdentifier(
                    "chargeCardsTableHolder${pair.first.toString().uppercase()}",
                    "id",
                    context.packageName
                )
            ) ?: return false;
        for (i in 0..diff) {
            val cell = createSingleCell(context, pair.first!!, columnIsEven)
            columnIsEven = !columnIsEven
            val textView = TextView(context).apply {
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

    return cardsDownloaded
}

private fun addHuedchen(
    context: Context,
    cardView: CardView,
    frameLayout: FrameLayout
) {
    val redImageView = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(32, 32).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 28, 48, 0)
        }
        setImageResource(R.drawable.huetchen)
        elevation = cardView.cardElevation + 4F
    }

    val shadowImage = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(34, 34).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 28, 48, 0)
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


    val columnSide = if (chargeType == ChargeType.DC) "right" else "left"
    val backgroundUri: String = if (!isEven) {
        "@drawable/border_light_bg_$columnSide"
    } else {
        "@drawable/border_dark_bg_$columnSide"

    }
    cell.setBackgroundResource(
        context.resources.getIdentifier(
            backgroundUri,
            "drawable",
            context.packageName
        )
    )
    return cell
}


private fun getPriceFormatter(): NumberFormat {
    val priceNumberFormat = NumberFormat.getCurrencyInstance()
    val decimalFormatSymbols: DecimalFormatSymbols =
        (priceNumberFormat as DecimalFormat).decimalFormatSymbols
    decimalFormatSymbols.currencySymbol = ""
    priceNumberFormat.decimalFormatSymbols = decimalFormatSymbols
    return priceNumberFormat
}


fun getCardImageDrawable(card: ChargeCards, api: API, context: Context): Pair<Drawable?, Boolean> {

    if (card.image.isNullOrEmpty()) {
        return null to false
    }
    val cardUri = URL(card.image)
    var cardImagePath: File? = getImagePath(cardUri, context)

    if (cardImagePath?.exists() == false) {
        api.downloadImageToInternalStorage(
            cardUri
        )
    }

    if (cardImagePath?.exists() == true) {
        try {
            cardImagePath = getImagePath(cardUri, context)
            return Drawable.createFromPath(cardImagePath.absolutePath) to false
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }
    return null to false
}

