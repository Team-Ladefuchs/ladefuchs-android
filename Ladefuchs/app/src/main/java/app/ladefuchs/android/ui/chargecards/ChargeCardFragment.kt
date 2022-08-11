package app.ladefuchs.android.ui.chargecards

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.*
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import android.widget.ImageView.ScaleType
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.withTranslation
import androidx.core.util.lruCache
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.R
import app.ladefuchs.android.R.id.action_navigation_chargecards_to_navigation_about
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.CardMetaData
import app.ladefuchs.android.helper.*
import com.aigestudio.wheelpicker.WheelPicker
import com.beust.klaxon.Klaxon
import com.makeramen.roundedimageview.RoundedImageView
import kotlinx.android.synthetic.main.fragment_chargecards.*
import kotlinx.android.synthetic.main.fragment_chargecards.view.*
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import kotlin.math.ceil


//import com.tylerthrailkill.helpers.prettyprint

class ChargeCardFragment : Fragment() {
    private var hasCustomerMaingauPrices: Boolean = false
    private var hasADACPrices: Boolean = false
    private var useBetaAPI: Boolean = false
    private var onboarding: Boolean = true
    private var showBanner: Boolean = true
    private var cardWidth: Int = 0
    private var cardHeight: Int = 0
    private val cardMargin: Int = 20
    private var promoProbabilities: Array<String> = arrayOf(
        "quote",
        "twitter", "twitter",
        "shop", "shop", "shop",
        "thg", "thg", "thg", "thg", "thg",
    )
    private var pocOperatorList: List<String> = listOf("Allego")
    private var currentPoc: String = pocOperatorList[0].lowercase()
    private var api: API? = null
    private var prefs: SharedPreferences? = null

    /**
     * This is the initialisation function that will be called on creation
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Update vars to represent User Preferences
        api = API(requireContext().applicationContext)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        hasADACPrices = prefs!!.getBoolean("specialEnbwAdac", false)
        hasCustomerMaingauPrices = prefs!!.getBoolean("specialMaingauCustomer", false)
        useBetaAPI = prefs!!.getBoolean("useBetaAPI", false)
        onboarding = prefs!!.getBoolean("firstStart", true)
        showBanner = prefs!!.getBoolean("showBanner", true)
        //init card dimensions
        //calculating Card Dimensions
        cardWidth = getScreenWidth() / 4
        cardHeight = 176 * cardWidth / 280
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chargecards, container, false)
    }

    /**
     * This function is called after creation and initialises the UI
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var nerdGlasses = view.findViewById<ImageView>(R.id.nerd_glasses)
        // check whether onboarding should be showed
        if (onboarding) {
            // deactivate the banner while in onboarding
            showBanner = false
        }
        // check whether the beta API is used
        if (useBetaAPI) {
            api?.useBeta()
            phraseView.text = getString(R.string.betaInfoText)
            // add cool nerd glasses
            nerdGlasses.visibility = VISIBLE
        }
        // fix the about navigation which lead to crashed previously
        view.findViewById<ImageButton>(R.id.aboutButton).setOnClickListener {
            findNavController().safeNavigate(action_navigation_chargecards_to_navigation_about)
        }
        // retrieve what shall be shown in the footer
        retrieveFooterContent(view)
        // retrieve all operators
        pocOperatorList = api!!.retrieveOperatorList()
        printLog(pocOperatorList.toString())
        // add easterEggOnclickListener
        easterEgg(view)
        //initialize Price List
        getPrices(
            currentPoc,
            forceDownload = false
        )
        // initialize picker
        val wheelPicker = view.findViewById(R.id.pocSelector) as WheelPicker
        //Switch to a more 3D, iOS-style Look
        wheelPicker.setAtmospheric(true)
        wheelPicker.isCurved = true
        wheelPicker.data = pocOperatorList.toMutableList()
        // Loading the pocList into the Picker Library
        wheelPicker.setOnItemSelectedListener { _, data, _ ->
            view.findViewById<ScrollView>(R.id.cardScroller).fullScroll(ScrollView.FOCUS_UP)
            getPrices(
                data.toString().lowercase(),
                forceDownload = false
            )
            currentPoc = data.toString().lowercase()
        }
        // set the colors of the Pull To Refresh View
        requireContext().let {
            ContextCompat.getColor(
                it, R.color.colorPrimary
            )
        }.let { swipetorefresh.setProgressBackgroundColorSchemeColor(it) }
        swipetorefresh.setColorSchemeColors(Color.WHITE)

        // RefreshListener
        swipetorefresh.setOnRefreshListener {
            getPrices(currentPoc, forceDownload = true)
            swipetorefresh.isRefreshing = false
        }
        // check whether onboarding should be shown
        if (onboarding) {
            onboarding()
        }
    }

    /**
     * This function initialises the click counter for the easter egg when clicking on the Ladefuchs
     */
    private fun easterEgg(view: View) {
        var easterEggClickCounter = 0
        val nerdGlasses = view.findViewById<ImageView>(R.id.nerd_glasses)
        view.findViewById<ImageView>(R.id.ladefuchs_logo).setOnClickListener {
            easterEggClickCounter++
            if (easterEggClickCounter == 42) {
                phraseView.text = getString(R.string.eastereggInfoText)
                easterEggClickCounter = 0
            } else if (easterEggClickCounter == 10) {
                useBetaAPI = !useBetaAPI
                if (useBetaAPI) {
                    api?.useBeta()
                    phraseView.text =
                        getString(R.string.betaInfoText)
                    nerdGlasses.visibility = VISIBLE
                } else {
                    phraseView.text =
                        getString(R.string.prodInfoText)
                    api?.useProd()
                    nerdGlasses.visibility = View.INVISIBLE
                }
                with(prefs!!.edit()) {
                    putBoolean("useBetaAPI", useBetaAPI)
                    apply()
                }
                easterEggClickCounter = 0
            }

        }
    }

    /**
     * Within this function the content of the footer will be determined
     */
    private fun retrieveFooterContent(view: View) {
        val phraseView = view.findViewById<TextView>(R.id.phraseView) as TextView
        val phrases =
            requireContext().applicationContext.assets?.open("phrases.txt")?.bufferedReader()
                .use { it?.readLines() }
        var currentPhrase: String = ""
        if (showBanner) {
            val curBanner = promoProbabilities.random()
            when (curBanner) {
                "shop" -> {
                    printLog("Loading Shop Banner")
                    drawPromoBanner(view, "shop", "https://shop.ladefuchs.app")
                }
                "thg" -> {
                    printLog("Loading THG Banner")
                    drawPromoBanner(
                        view,
                        "thg",
                        "https://api?.ladefuchs.app/affiliate?url=https%3A%2F%2Fgeld-fuer-eauto.de%2Fref%2FLadefuchs&banner=3edae17e-40e3-4842-867b-44529e556b23"
                    )
                }
                "twitter" -> {
                    printLog("Loading Twitter Banner")
                    drawPromoBanner(view, "twitter", "https://twitter.com/ladefuchs")
                }
                else -> {
                    printLog("Falling back on your mom")
                    currentPhrase = phrases?.random() ?: ""
                    phraseView.text = currentPhrase

                }
            }
        } else {
            printLog("Falling back on your mom")
            currentPhrase = phrases?.random() ?: ""
            phraseView.text = currentPhrase
        }
    }

    /**
     * This function draws the banner content
     */
    private fun drawPromoBanner(view: View, promoType: String, promoURL: String) {
        val viewWidth = getScreenWidth()
        val viewHeight = 280 * viewWidth / 1170
        val phraseContainer = view.findViewById<TextView>(R.id.phraseContainer) as LinearLayout
        phraseContainer.removeView(phraseView)
        val phraseContainerParams = phraseContainer.layoutParams
        phraseContainerParams.height = viewHeight - 35
        phraseContainer.setBackgroundColor(Color.parseColor("#FFCEC0AC"))
        phraseContainer.layoutParams = phraseContainerParams

        val bannerView = view.findViewById<TextView>(R.id.bannerView) as LinearLayout
        bannerView.visibility = VISIBLE
        val bannerButton = bannerView.bannerImage

        bannerButton.setImageResource(
            resources.getIdentifier(
                "banner_$promoType", "drawable",
                context?.packageName
            )
        )
        bannerButton.requestLayout()
        bannerView.setBackgroundColor(Color.parseColor("#00FFFFFF"))
        val buttonURL = Uri.parse(promoURL)
        val bannerParams = bannerButton.layoutParams
        bannerParams.width = viewWidth
        bannerParams.height = viewHeight
        bannerButton.scaleType = ScaleType.FIT_XY
        bannerButton.setBackgroundColor(Color.parseColor("#00FFFFFF"))
        bannerButton.layoutParams = bannerParams

        bannerButton.setOnClickListener {
            val intent = Intent(android.content.Intent.ACTION_VIEW)
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = buttonURL
            startActivity(intent)
        }
    }

    /**
     * This function retrieves the prices for a specific operator
     */
    private fun getPrices(
        pocOperator: String,
        forceDownload: Boolean = false
    ): String {
        //Load Prices JSON from File
        printLog("Getting prices for $pocOperator")
        val chargeCardsAC = api!!.readPrices(
            pocOperator,
            "ac",
            forceDownload
        )?.sortedBy { it.price }
        val chargeCardsDC = api!!.readPrices(
            pocOperator,
            "dc",
            forceDownload
        )?.sortedBy { it.price }
        if (chargeCardsAC != null || chargeCardsDC != null) {
            val maxListLength = maxOf(chargeCardsAC!!.size, chargeCardsDC!!.size)
            fillCards("ac", chargeCardsAC, maxListLength)
            fillCards("dc", chargeCardsDC, maxListLength)
        }

        return pocOperator
    }

    @SuppressLint("ResourceAsColor")
    fun fillCards(
        currentType: String,
        chargeCards: List<ChargeCards>,
        maxListLength: Int,
        paintStroke: Boolean = false
    ) {

        val cardMetadata = readCardMetadata()

        // Define Views to attach Card Tables to and required Variables
        var columnSide = "left"
        if (currentType.lowercase() == "dc") {
            columnSide = "right"
        }
        var i = 0
        val columnName = "chargeCardsTableHolder" + currentType.uppercase()
        val chargeCardsColumn: LinearLayout =
            view?.findViewById(
                resources.getIdentifier(
                    columnName,
                    "id",
                    requireContext().packageName
                )
            ) ?: return
        chargeCardsColumn.removeAllViews()
        chargeCards.forEach { currentCard ->

            val cardIdentifier = "card_" + currentCard.identifier
            val cardProviderIdentifier = "card_" + currentCard.provider

            // Skip ADAC card if not enabled
            if (currentCard.identifier == "adac" && !hasADACPrices) {
                printLog("ADAC prices will be skipped")
                return@forEach
            }

            // Skip Maingau Prices if personalized processes are available
            if (currentCard.identifier == "maingau_energie" && hasCustomerMaingauPrices) {
                return@forEach
            }

            var cardMeta = cardMetadata?.find { it.identifier == cardIdentifier }
            if (cardMeta == null) {
                cardMeta = cardMetadata?.find { it.identifier == "default" }
            }


            // Creating a Holder for Card and Price, to lay them out next to each other
            val CardHolderView: LinearLayout = LinearLayout(context)
            chargeCardsColumn.addView(CardHolderView)
            CardHolderView.gravity = Gravity.CENTER_VERTICAL
            CardHolderView.orientation = LinearLayout.HORIZONTAL

            val backgroundUri: String = if (i % 2 == 0) {
                "@drawable/border_light_bg_$columnSide"
            } else {
                "@drawable/border_dark_bg_$columnSide"
            }

            CardHolderView.setBackgroundResource(
                resources.getIdentifier(
                    backgroundUri, "drawable",
                    context?.packageName
                )
            )

            // Creating a View that will Hold the card image as a Background
            val imageView: ImageView = ImageView(context)
            CardHolderView.addView(imageView)
            imageView.requestLayout()
            imageView.layoutParams.width = cardWidth
            imageView.layoutParams.height = cardHeight


            var resourceIdentifier: Int? = context?.resources?.getIdentifier(
                cardIdentifier,
                "drawable",
                context?.packageName
            )

            if (resourceIdentifier == 0) {
                resourceIdentifier = context?.resources?.getIdentifier(
                    cardProviderIdentifier,
                    "drawable",
                    context?.packageName
                )
            }
            val cardImage =
                File(requireContext().filesDir.toString() + "/card_" + currentCard.identifier + ".jpg")
            printLog(cardImage.toString())

            if ((resourceIdentifier != 0 && resourceIdentifier != null) || cardImage.exists()) {
                CardHolderView.removeView(imageView)
                val imageCardView = RoundedImageView(context)
                CardHolderView.addView(imageCardView)
                imageCardView.layoutParams.width = cardWidth
                imageCardView.layoutParams.height = cardHeight
                (imageCardView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    cardMargin,
                    cardMargin,
                    cardMargin,
                    cardMargin
                )
                imageCardView.scaleType = ScaleType.FIT_XY
                imageCardView.cornerRadius = 25.toFloat()
                if (paintStroke) {
                    imageCardView.borderWidth = 4.toFloat()
                    imageCardView.borderColor = Color.DKGRAY
                }
                imageCardView.mutateBackground(true)

                if (cardImage.exists()) {
                    printLog("Setting " + cardImage.absolutePath.toString() + " as background for card: " + cardProviderIdentifier)
                    printLog(
                        "drawable: " + Drawable.createFromPath(cardImage.absolutePath).toString()
                    )
                    var cardImageDrawable: Drawable? = null
                    try {
                        cardImageDrawable =
                            Drawable.createFromPath(cardImage.absolutePath)!! as BitmapDrawable
                    } catch (e: Exception) {
                        //e.printStackTrace()
                    }

                    if (cardImageDrawable != null) {
                        imageCardView.background =
                            Drawable.createFromPath(cardImage.absolutePath)!! as BitmapDrawable
                    }
                } else {
                    resourceIdentifier?.let { imageCardView.setBackgroundResource(it) }
                }
                imageCardView.isOval = false
                imageCardView.tileModeX = Shader.TileMode.CLAMP
                imageCardView.tileModeY = Shader.TileMode.CLAMP
                imageCardView.requestLayout()

            } else {

                api?.downloadImageToInternalStorage(
                    currentCard.identifier,
                    "card_" + currentCard.identifier + ".jpg"
                )
                var cardText = currentCard.name
                if (currentCard.provider != currentCard.name) {
                    cardText = currentCard.provider
                }
                val cardBitmap = drawChargeCard(
                    textToDraw = cardText,
                    textSize = 36F,
                    textColor = Color.parseColor("#" + cardMeta?.textColor),
                    rectangleColor = Color.parseColor("#" + cardMeta?.borderColor),
                    backgroundColor = Color.parseColor("#" + cardMeta?.backgroundColor),
                    paintStroke = paintStroke
                )
                imageView.background = BitmapDrawable(resources, cardBitmap)
                imageView.elevation = 0.5F
                (imageView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    cardMargin,
                    cardMargin,
                    cardMargin,
                    cardMargin
                )
            }

            // Format the price according to the user set locale
            val priceNumberFormat = NumberFormat.getCurrencyInstance()
            val decimalFormatSymbols: DecimalFormatSymbols =
                (priceNumberFormat as DecimalFormat).decimalFormatSymbols
            decimalFormatSymbols.currencySymbol = ""
            priceNumberFormat.decimalFormatSymbols = decimalFormatSymbols
            (priceNumberFormat.format(currentCard.price).trim { it <= ' ' })

            // Creating the TextView that will hold the Price
            val textviewPrice: TextView = TextView(context)
            CardHolderView.addView(textviewPrice)
            textviewPrice.text = priceNumberFormat.format(currentCard.price).trim { it <= ' ' }
            textviewPrice.setTextAppearance(R.style.TableTextView)
            textviewPrice.gravity = Gravity.CENTER
            textviewPrice.width = cardWidth
            i++
        }

        //Filling Column with empty Cells if necessary
        if (i < maxListLength - 1) {
            while (i < maxListLength - 1) {
                // Creating a Holder for Card and Price, to lay them out next to each other
                var CardHolderView: LinearLayout = LinearLayout(context)
                chargeCardsColumn.addView(CardHolderView)
                CardHolderView.gravity = Gravity.CENTER_VERTICAL
                CardHolderView.layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                CardHolderView.orientation = LinearLayout.HORIZONTAL
                if (i % 2 == 0) {
                    //TableColorLight
                    CardHolderView.setBackgroundColor(Color.parseColor("#F2EBE1"))
                } else {
                    // TableColorDark
                    CardHolderView.setBackgroundColor(Color.parseColor("#CEC0AC"))
                }
                CardHolderView.setPadding(cardMargin, cardMargin, cardMargin, cardMargin)


                // Creating the TextView that will hold the Price
                val textview: TextView = TextView(context)
                CardHolderView.addView(textview)

                textview.text = ("")
                textview.setPadding(cardMargin, cardMargin, cardMargin, cardMargin)
                textview.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
                textview.setTextAppearance(R.style.TableTextViewDisabled)
                textview.setTextColor(R.color.TextColorDisabled)
                textview.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                textview.height = cardHeight

                i++
            }
        }

    }


    private fun readCardMetadata(): List<CardMetaData>? {
        //Load Metadata JSON from File
        printLog("Reading de-card_metadata.json")
        val cardMetadata = activity?.assets?.open("de-card_metadata.json")?.let {
            Klaxon().parseArray<CardMetaData>(
                it
            )
        }
        return cardMetadata
    }


    private fun drawChargeCard(
        textToDraw: String = "N/A",
        textSize: Float = 8F,
        textColor: Int = Color.BLACK,
        rectangleColor: Int = Color.LTGRAY,
        backgroundColor: Int = Color.WHITE,
        paintStroke: Boolean = false
    ): Bitmap? {
        val scaleFactor = 2
        val cornerRadius = 25F * scaleFactor
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

        var textPositionYOffset =
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

    fun showCardDetails(title: String = "", message: String = "") {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.yes, null)
        builder.show()
    }

    // this is blatantly stolen from https://medium.com/over-engineering/drawing-multiline-text-to-canvas-on-android-9b98f0bfa16a (credit @ricknout)
    @RequiresApi(Build.VERSION_CODES.O)
    fun Canvas.drawMultilineText(
        text: CharSequence,
        textPaint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        start: Int = 0,
        end: Int = text.length,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
        spacingMult: Float = 1f,
        spacingAdd: Float = 0f,
        includePad: Boolean = true,
        ellipsizedWidth: Int = width,
        ellipsize: TextUtils.TruncateAt? = null,
        maxLines: Int = Int.MAX_VALUE,
        breakStrategy: Int = LineBreaker.BREAK_STRATEGY_SIMPLE,
        hyphenationFrequency: Int = Layout.HYPHENATION_FREQUENCY_NONE,
        justificationMode: Int = LineBreaker.JUSTIFICATION_MODE_NONE
    ) {

        val cacheKey = "$text-$start-$end-$textPaint-$width-$alignment-$textDir-" +
                "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize-" +
                "$maxLines-$breakStrategy-$hyphenationFrequency-$justificationMode"

        val staticLayout = StaticLayoutCache[cacheKey] ?: StaticLayout.Builder.obtain(
            text,
            start,
            end,
            textPaint,
            width
        )
            .setAlignment(alignment)
            .setTextDirection(textDir)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(includePad)
            .setEllipsizedWidth(ellipsizedWidth)
            .setEllipsize(ellipsize)
            .setMaxLines(maxLines)
            .setBreakStrategy(breakStrategy)
            .setHyphenationFrequency(hyphenationFrequency)
            .setJustificationMode(justificationMode)
            .build().apply { StaticLayoutCache[cacheKey] = this }

        staticLayout.draw(this, x, y)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun Canvas.drawMultilineText(
        text: CharSequence,
        textPaint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        start: Int = 0,
        end: Int = text.length,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
        spacingMult: Float = 1f,
        spacingAdd: Float = 0f,
        includePad: Boolean = true,
        ellipsizedWidth: Int = width,
        ellipsize: TextUtils.TruncateAt? = null,
        maxLines: Int = Int.MAX_VALUE,
        breakStrategy: Int = LineBreaker.BREAK_STRATEGY_SIMPLE,
        hyphenationFrequency: Int = Layout.HYPHENATION_FREQUENCY_NONE
    ) {

        val cacheKey = "$text-$start-$end-$textPaint-$width-$alignment-$textDir-" +
                "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize-" +
                "$maxLines-$breakStrategy-$hyphenationFrequency"

        val staticLayout = StaticLayoutCache[cacheKey] ?: StaticLayout.Builder.obtain(
            text,
            start,
            end,
            textPaint,
            width
        )
            .setAlignment(alignment)
            .setTextDirection(textDir)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(includePad)
            .setEllipsizedWidth(ellipsizedWidth)
            .setEllipsize(ellipsize)
            .setMaxLines(maxLines)
            .setBreakStrategy(breakStrategy)
            .setHyphenationFrequency(hyphenationFrequency)
            .build().apply { StaticLayoutCache[cacheKey] = this }

        staticLayout.draw(this, x, y)
    }

    fun Canvas.drawMultilineText(
        text: CharSequence,
        textPaint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        start: Int = 0,
        end: Int = text.length,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        spacingMult: Float = 1f,
        spacingAdd: Float = 0f,
        includePad: Boolean = true,
        ellipsizedWidth: Int = width,
        ellipsize: TextUtils.TruncateAt? = null
    ) {

        val cacheKey = "$text-$start-$end-$textPaint-$width-$alignment-" +
                "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize"

        // The public constructor was deprecated in API level 28,
        // but the builder is only available from API level 23 onwards
        val staticLayout =
            StaticLayoutCache[cacheKey] ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, start, end, textPaint, width)
                    .setAlignment(alignment)
                    .setLineSpacing(spacingAdd, spacingMult)
                    .setIncludePad(includePad)
                    .setEllipsizedWidth(ellipsizedWidth)
                    .setEllipsize(ellipsize)
                    .build()
            } else {
                StaticLayout(
                    text, start, end, textPaint, width, alignment,
                    spacingMult, spacingAdd, includePad, ellipsize, ellipsizedWidth
                )
                    .apply { StaticLayoutCache[cacheKey] = this }
            }

        staticLayout.draw(this, x, y)
    }

    private fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
        canvas.withTranslation(x, y) {
            draw(this)
        }
    }

    fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.card_detail_dialog, null))
                // Add action buttons
                .setPositiveButton(R.string.dialog_ok,
                    DialogInterface.OnClickListener { dialog, id ->
                        // sign in the user ...
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private object StaticLayoutCache {

        private const val MAX_SIZE = 50 // Arbitrary max number of cached items
        private val cache = lruCache<String, StaticLayout>(MAX_SIZE)

        operator fun set(key: String, staticLayout: StaticLayout) {
            cache.put(key, staticLayout)
        }

        operator fun get(key: String): StaticLayout? {
            return cache[key]
        }
    }

    private fun onboarding(step: Int = 1) {
        phraseView.text = getString(R.string.onboarding_phrase)
        var curOverlay: ConstraintLayout? = null;
        when (step) {
            1 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_1)
                    .also { curOverlay = it }
            }
            2 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_1)?.visibility = View.GONE
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_2)
                    .also { curOverlay = it }
            }
            3 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_2)?.visibility = View.GONE
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_3)
                    .also { curOverlay = it }
            }
            4 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_3)?.visibility = View.GONE
                val edit = PreferenceManager.getDefaultSharedPreferences(context).edit()
                edit.putBoolean("firstStart", false).apply()
                return
            }
        }
        curOverlay?.visibility = VISIBLE
        curOverlay?.setOnClickListener {
            onboarding(step + 1)
        }
    }


}
