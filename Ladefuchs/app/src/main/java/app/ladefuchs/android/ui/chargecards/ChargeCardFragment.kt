package app.ladefuchs.android.ui.chargecards

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
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
import android.view.ViewGroup
import android.widget.*
import android.widget.ImageView.ScaleType
import androidx.annotation.Keep
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
import com.aigestudio.wheelpicker.WheelPicker
import com.beust.klaxon.Klaxon
import com.makeramen.roundedimageview.RoundedImageView
import kotlinx.android.synthetic.main.fragment_chargecards.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.random.Random.Default.nextFloat
import kotlin.random.Random.Default.nextInt


//import com.tylerthrailkill.helpers.prettyprint

@Keep
class ChargeCards(
    val identifier: String,
    val name: String,
    val provider: String,
    val price: Float,
    val updated: Long
)

@Keep
class MaingauPrices(val acPrice: Float, val dcPrice: Float, val ionityPrice: Float)

@Keep
class CardMetadata(
    val identifier: String,
    val name: String,
    val backgroundColor: String,
    val textColor: String,
    val cardImageFile: String,
    val borderColor: String
)

@Keep
class Operators(
    val name: String,
    val displayName: String,
    val identifier: String
)

class ChargeCardFragment : Fragment() {
    var hasCustomerMaingauPrices: Boolean = false
    var hasADACPrices: Boolean = false
    var useBetaAPI: Boolean = false
    var cardWidth: Int = 0
    var cardHeight: Int = 0
    val cardMargin: Int = 20
    var shopPromo: Float = 0.5F
    val apiToken: String = BuildConfig.apiKey
    var apiBaseURL: String = "https://api.ladefuchs.app/"
    var apiBaseRegularURL: String = "https://api.ladefuchs.app/"
    var apiVersionRegularPath: String = ""
    var apiVersionPath: String = ""
    val apiBaseBetaURL: String = "https://beta.api.ladefuchs.app/"
    val apiVersionBetaPath: String = ""
    val apiImageBasePath: String = "images/cards/"
    var pocOperatorList: List<String> = listOf("Allego") //first standard value will be altered during runtime
    var currentPoc: String = pocOperatorList[0].lowercase()
    var firstStart: Boolean = true

    private lateinit var chargeCardViewModel: ChargeCardViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        context
        val prefs =  PreferenceManager.getDefaultSharedPreferences(context)
        this.firstStart = prefs.getBoolean("firstStart", true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chargecards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Get Preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        hasADACPrices = prefs.getBoolean("specialEnbwAdac", false)
        hasCustomerMaingauPrices = prefs.getBoolean("specialMaingauCustomer", false)
        useBetaAPI = prefs.getBoolean("useBetaAPI", false)

        //Set API type
        val nerdGlasses = view.findViewById<ImageView>(R.id.nerd_glasses)
        if(useBetaAPI) {
            apiBaseURL = apiBaseBetaURL
            apiVersionPath = apiVersionBetaPath
            nerdGlasses.visibility = View.VISIBLE
        }

        if(firstStart){
            shopPromo=0.0F
        }
        else {
            val editor = prefs.edit()

            //Check if we should do Store Promo
            shopPromo = prefs.getFloat("shopPromo", 0.0F)
            checkShopPromoLevel(editor)
        }

        view.findViewById<ImageButton>(R.id.aboutButton).setOnClickListener {
            findNavController().navigate(action_navigation_chargecards_to_navigation_about)
        }

        //calculating Card Dimensions
        cardWidth = getScreenWidth() / 4
        cardHeight = 176 * cardWidth / 280

        //Load Bottom Phrases, select random string and override footer
        view.findViewById<ImageButton>(R.id.ladefuchs_hoodie).visibility = View.INVISIBLE
        val appContext = activity?.applicationContext
        val phrasesFile = "phrases.txt"
        val phraseView = view.findViewById<TextView>(R.id.phraseView) as TextView
        val phrases = appContext?.assets?.open(phrasesFile)?.bufferedReader().use { it?.readLines() }
        var currentPhrase: String = ""
        if (phrases != null) {
            if (nextFloat() <= shopPromo) {
                currentPhrase = "Du liebst den Ladefuchs? \nBesuche unseren Shop️"
                view.findViewById<ImageButton>(R.id.ladefuchs_hoodie).visibility = View.VISIBLE
                val hoodieGlow = view.findViewById<ImageView>(R.id.ladefuchs_hoodie_glow)
                hoodieGlow.visibility = View.VISIBLE
                val pulseHoodie: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    hoodieGlow,
                    PropertyValuesHolder.ofFloat("alpha", 0f)
                )
                pulseHoodie.duration = 1200
                pulseHoodie.repeatCount = ObjectAnimator.INFINITE
                pulseHoodie.repeatMode = ObjectAnimator.REVERSE
                pulseHoodie.start()

                val shopButton = view.findViewById(R.id.ladefuchs_hoodie) as ImageButton
                shopButton.setOnClickListener {
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                    intent.data = Uri.parse(it.tag.toString())
                    startActivity(intent)
                }
            } else {
                currentPhrase = phrases[nextInt(phrases.size)]
            }
            phraseView.text = currentPhrase
        }
        retrieveOperatorList()
        //EasterEgg
        var easterEggClickCounter = 0
        view.findViewById<ImageView>(R.id.ladefuchs_logo).setOnClickListener {
            easterEggClickCounter++
            if (easterEggClickCounter == 42) {
                phraseView.text = "EY! LASS DEN FUCHS IN RUHE! WAS HAT ER DIR GETAN?! FERKEL!"
                easterEggClickCounter = 0
            } else if (easterEggClickCounter == 10) {
                useBetaAPI = !useBetaAPI
                if(useBetaAPI) {
                    phraseView.text =
                        "Der Fuchs benutzt jetzt die Beta API, was soll schon schief gehen."
                    apiBaseURL = apiBaseBetaURL
                    apiVersionPath = apiVersionBetaPath
                    val nerdGlasses = view.findViewById<ImageView>(R.id.nerd_glasses)
                    nerdGlasses.visibility = View.VISIBLE
                } else {
                    phraseView.text =
                        "Der Fuchs hat den API-Sicherheitsgurt wieder angelegt."
                    apiBaseURL = apiBaseRegularURL
                    apiVersionPath = apiVersionRegularPath
                    nerdGlasses.visibility = View.INVISIBLE
                }
                with (prefs.edit()) {
                    putBoolean("useBetaAPI", useBetaAPI)
                    apply()
                }
                easterEggClickCounter = 0
            }

        }

        //initialize Price List
        getPrices(
            currentPoc,
            launchedAfterDownload = false,
            forceDownload = false
        )

        //Loading the pocList into the Picker Library
        val wheelPicker = view.findViewById(R.id.pocSelector) as WheelPicker
        //Switch to a more 3D, iOS-style Look
        wheelPicker.setAtmospheric(true)
        wheelPicker.isCurved = true
        wheelPicker.data = pocOperatorList.toMutableList()
        wheelPicker.setOnItemSelectedListener(WheelPicker.OnItemSelectedListener { picker, data, position ->
            view.findViewById<ScrollView>(R.id.cardScroller).fullScroll(ScrollView.FOCUS_UP)
            getPrices(
                data.toString().lowercase(),
                launchedAfterDownload = false,
                forceDownload = false
            )
            currentPoc = data.toString().lowercase()

        })

        //PullToRefresh Handler
        //** Set the colors of the Pull To Refresh View
        context?.let {
            ContextCompat.getColor(
                it, R.color.colorPrimary
            )
        }?.let { swipetorefresh.setProgressBackgroundColorSchemeColor(it) }
        swipetorefresh.setColorSchemeColors(Color.WHITE)

        //RefreshListener
        swipetorefresh.setOnRefreshListener {
            getPrices(currentPoc, launchedAfterDownload = false, forceDownload = true)
            swipetorefresh.isRefreshing = false
        }
        if (firstStart) {
            onboarding()
        }
    }

    private fun retrieveOperatorList() {
        val JSONUrl = apiBaseURL + "operators/enabled"
        val JSONFileName = "operators.json"
        // download the latest operator list
        downloadJSONToInternalStorage(JSONUrl, JSONFileName, "", false)
        // read list into pocOperatorList variable
        printLog("Reading $JSONFileName")
        var operators:List<Operators>? = null
        try {
            val operatorsFile: File? = File(activity?.getFileStreamPath(JSONFileName).toString())
            operators = operatorsFile?.let { Klaxon().parseArray<Operators>(it) }!!
        }
        catch(e: Exception){
           }
        if(operators == null) {
            operators = activity?.assets?.open(JSONFileName)?.let {
                Klaxon().parseArray<Operators>(
                    it
                )
            }
        }
        if(operators!=null){
            var operatorDisplayNames: List<String> = mutableListOf()
            for(element in operators){
                operatorDisplayNames=operatorDisplayNames.plus(element.displayName)
            }
            pocOperatorList = operatorDisplayNames.sortedBy { it?.lowercase() }
        }

    }

    private fun checkShopPromoLevel(editor: SharedPreferences.Editor) {
        val client = OkHttpClient()
        val url = URL(apiBaseURL+ "shop/promo")
        var apiResponse: Float
        printLog("Trying to get Promo Level", "network")
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiToken")
            .build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                apiResponse = response.body!!.string().toFloat()
                printLog("PromoLevel from API: $apiResponse")
                editor.putFloat("shopPromo", apiResponse)
                editor.commit()
            } catch (e: Exception) {
                printLog("Couldn't get Store Promo Level", "error")
                e.printStackTrace()
            }
        }.start()
    }

    private fun storeFileInInternalStorage(inputStream: InputStream, internalStorageFileName: String) {
        val outputStream = activity?.openFileOutput(internalStorageFileName, Context.MODE_PRIVATE)
        val buffer = ByteArray(1024)
        inputStream.use {
            while (true) {
                val byeCount = it.read(buffer)
                if (byeCount < 0) break
                outputStream?.write(buffer, 0, byeCount)
            }
            outputStream?.close()
            printLog("Writing File: " + internalStorageFileName + " to " + requireContext().filesDir.toString())
        }
    }

    private fun downloadJSONToInternalStorage(JSONUrl: String, JSONFileName: String, pocOperator: String, pocFile: Boolean = true) {

        printLog("Downloading $JSONUrl", "network")
        val client = OkHttpClient()
        val url = URL(JSONUrl)

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer " + apiToken)
            .build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                storeFileInInternalStorage(
                    response.body!!.string().byteInputStream(),
                    JSONFileName
                )
                if (pocFile){
                    activity?.runOnUiThread {
                        printLog("Refreshing UI")
                        getPrices(pocOperator, launchedAfterDownload = true, forceDownload = false)
                    }
                }
            } catch (e: Exception) {
                printLog("Couldn't download JSON Data from $JSONUrl", "error")
                e.printStackTrace()
            }
        }.start()
    }

    private fun downloadImageToInternalStorage(ImageUrl: String, ImageFileName: String ) {

        printLog("Downloading $ImageUrl", "network")
        val url: URL = URL(ImageUrl)
        val storagePath: String = requireContext().filesDir.toString() + "/" + ImageFileName
        Thread {
            try {
                val input = url.openStream()
                try {
                    printLog("Getting Image")
                    val output: OutputStream = FileOutputStream(storagePath)
                    try {
                        val buffer = ByteArray(1024)
                        var bytesRead = 0
                        while (input.read(buffer, 0, buffer.size).also { bytesRead = it } >= 0) {
                            output.write(buffer, 0, bytesRead)
                        }
                    } finally {
                        output.close()
                    }
                } finally {
                    input.close()
                }
            }  catch (e: Exception) {
                printLog("Couldn't open stream $ImageUrl", "error")
                e.printStackTrace()
            }
        }.start()
    }

    private fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    private fun getPrices(
        pocOperator: String,
        launchedAfterDownload: Boolean = false,
        forceDownload: Boolean = false
    ): String {
        //Load Prices JSON from File
        printLog("Getting prices for $pocOperator")
        var chargeCardsAC = readPrices(pocOperator, "ac", launchedAfterDownload, forceDownload)?.sortedBy { it.price }
        var chargeCardsDC = readPrices(pocOperator, "dc", launchedAfterDownload, forceDownload)?.sortedBy { it.price }
        if (chargeCardsAC != null || chargeCardsDC != null) {
            val maxListLength = maxOf(chargeCardsAC!!.size, chargeCardsDC!!.size)
            chargeCardsAC.let { fillCards(pocOperator, "ac", it, maxListLength) }
            chargeCardsDC.let { fillCards(pocOperator, "dc", it, maxListLength) }
        }
        return pocOperator
    }

    @SuppressLint("ResourceAsColor")
    fun fillCards(
        pocOperator: String,
        currentType: String,
        chargeCards: List<ChargeCards>,
        maxListLength: Int,
        paintStroke: Boolean = false
    ) {

        val cardMetadata = readCardMetadata()
        val viewNeedsRefresh: Boolean = false

        // Define Views to attach Card Tables to and required Variables
        var columnSide = "left"
        if (currentType.lowercase() == "dc") {
            columnSide = "right"
        }
        var i = 0
        val columnName = "chargeCardsTableHolder" + currentType.uppercase()
        val chargeCardsColumn =
            view?.findViewById<LinearLayout>(
                resources.getIdentifier(
                    columnName,
                    "id",
                    context?.packageName
                )
            ) as LinearLayout

        chargeCardsColumn.removeAllViews()
        chargeCards.forEach { currentCard ->

            var cardIdentifier = "card_" + currentCard.identifier
            var cardProviderIdentifier = "card_" + currentCard.provider

            //Skip ADAC card if not enabled
            if (currentCard.identifier == "adac" && !hasADACPrices) {
                printLog("ADAC prices will be skipped")
                return@forEach
            }

            //Skip Maingau Prices if personalized processes are available
            if (currentCard.identifier == "maingau_energie" && hasCustomerMaingauPrices == true) {
                return@forEach
            }

            var cardMeta = cardMetadata?.find { it.identifier == cardIdentifier}
            if (cardMeta == null) {
                cardMeta = cardMetadata?.find { it.identifier == "default"}
            }


            // Creating a Holder for Card and Price, to lay them out next to each other
            var CardHolderView: LinearLayout = LinearLayout(context)
            chargeCardsColumn.addView(CardHolderView)
            CardHolderView.gravity = Gravity.CENTER_VERTICAL
            CardHolderView.orientation = LinearLayout.HORIZONTAL

            var backgroundUri:String
            backgroundUri = if (i % 2 == 0) {
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
            var imageView: ImageView = ImageView(context)
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
            val cardImage = File(requireContext().filesDir.toString() + "/card_" + currentCard.identifier + ".jpg" )
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
                if(paintStroke) {
                    imageCardView.borderWidth = 4.toFloat()
                    imageCardView.borderColor = Color.DKGRAY
                }
                imageCardView.mutateBackground(true)

                if (cardImage.exists()) {
                    printLog("Setting " + cardImage.absolutePath.toString() + " as background for card: " + cardProviderIdentifier)
                    printLog("drawable: " + Drawable.createFromPath(cardImage.absolutePath).toString())
                    val cardImageDrawable: Drawable = Drawable.createFromPath(cardImage.absolutePath)!! as BitmapDrawable
                        if (cardImageDrawable != null) {
                            imageCardView.background = Drawable.createFromPath(cardImage.absolutePath)!! as BitmapDrawable
                        }
                } else {
                    resourceIdentifier?.let { imageCardView.setBackgroundResource(it) }
                }
                imageCardView.isOval = false
                imageCardView.tileModeX = Shader.TileMode.CLAMP
                imageCardView.tileModeY = Shader.TileMode.CLAMP
                imageCardView.requestLayout()

            } else {
                downloadImageToInternalStorage(apiBaseURL + apiImageBasePath + currentCard.identifier + ".jpg", "card_"+currentCard.identifier+".jpg")
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
        if(i < maxListLength-1) {
            while (i < maxListLength-1) {
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

    private fun readPrices(
        pocOperator: String,
        currentType: String,
        launchedAfterDownload: Boolean = false,
        forceDownload: Boolean = false
    ): List<ChargeCards>? {

        //Load Prices JSON from File
        var country = "de"
        val replaceRule = Regex("[^A-Za-z0-9.+-]")
        val pocOperatorClean = replaceRule.replace(pocOperator, "")
        printLog("Getting Prices for $pocOperatorClean")
        var JSONFileName = "$country-$pocOperatorClean-$currentType.json"
        var chargeCards: List<ChargeCards> = listOf<ChargeCards>()
        var forceInitialDownload = forceDownload

        // check whether forceDownload was activated
        if(!forceDownload) {
            var JSONFile: File? = File(activity?.getFileStreamPath(JSONFileName).toString())
            val JSONFileExists = JSONFile?.exists()
            if (!JSONFileExists!!) {
                val bundledJSON = activity?.assets?.open("skeleton.json")
                forceInitialDownload = true
                if (bundledJSON != null) {
                    storeFileInInternalStorage(bundledJSON, JSONFileName)
                }
                printLog("Loading $JSONFileName")
                JSONFile = File(activity?.getFileStreamPath(JSONFileName).toString())
            }
            try {
                chargeCards = JSONFile?.let { Klaxon().parseArray<ChargeCards>(it) }!!
            } catch (e: Exception) {
                //e.printStackTrace()
            }
        }
        if ((chargeCards.isNotEmpty() && (System.currentTimeMillis() / 1000L - chargeCards.get(0).updated > 86400)  && !launchedAfterDownload) || forceDownload || forceInitialDownload) {
            val JSONUrl = apiBaseURL + apiVersionPath + "cards/" + country.lowercase() + "/" + pocOperatorClean.lowercase() + "/" + currentType.lowercase()
            printLog("Data in $JSONFileName is outdated or update was forced, Updating from API")
            downloadJSONToInternalStorage(JSONUrl, JSONFileName, pocOperator)
            // load the freshly downloaded JSON file
            var JSONFile = File(activity?.getFileStreamPath(JSONFileName).toString())

            try {
                printLog("Reloading chargeCards after Download")
                chargeCards = JSONFile?.let { Klaxon().parseArray<ChargeCards>(it) }!!
            } catch (e: Exception) {
                //e.printStackTrace()
            }
        }
        chargeCards = chargeCards.toMutableList()
        //Get available chargecards as string list and transform them back to a real list/set
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var selectedChargeCards:Set<String> =
            prefs.getString("selectedChargeCards", "")!!
                .removePrefix("[") // Remove leading bracket from string
                .removeSuffix("]") // Remove trailing bracket from string
                .replace("\\s".toRegex(), "") // strip spaces
                .split(',').toSet() // transform back to list and then to set for more efficient contains

        // if the user hasn't selected any chargeCards keep all
        if (selectedChargeCards.isNotEmpty() && selectedChargeCards.size>1){
            // remove all chargeCards that were deselected
            chargeCards.removeIf {x: ChargeCards -> x.identifier !in selectedChargeCards && x.identifier != "adac"}
        }
        val maingauPrices = getMaingauPrices(currentType, pocOperatorClean)
        if (maingauPrices.name.isNotEmpty() && pocOperatorClean.lowercase() != "ladeverbund+") {
            chargeCards.add(maingauPrices)
        }

        return chargeCards
    }


    private fun readCardMetadata(): List<CardMetadata>? {
        //Load Metadata JSON from File
        var country = "de"
        var JSONFileName = "$country-card_metadata.json"
        printLog("Reading $JSONFileName")
        var cardMetadata = activity?.assets?.open(JSONFileName)?.let {
            Klaxon().parseArray<CardMetadata>(
                it
            )
        }
        return cardMetadata
    }

    private fun getMaingauPrices(type: String, pocOperator: String): ChargeCards {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        //Load Pricetoggle from prefs
        val hasMaingauCustomerPrices = prefs.getBoolean("specialMaingauCustomer", false)

        var maingauIonityPrice: Float = 0.75F
        var maingauAcPrice: Float = 0.49F
        var maingauDcPrice: Float = 0.59F

        var maingauPrice = ChargeCards(
            identifier = "",
            name = "",
            provider = "",
            price = 0.0f,
            updated = System.currentTimeMillis() / 1000L
        )
        if(hasMaingauCustomerPrices) {
            when {
                pocOperator.lowercase() == "ionity" && type == "dc" -> {
                    maingauPrice = ChargeCards(
                        identifier = "maingau_personalized",
                        name = "Einfach Strom Laden",
                        provider = "Maingau",
                        price = maingauIonityPrice,
                        updated = System.currentTimeMillis() / 1000L
                    )
                }
                type == "ac" && pocOperator.lowercase() != "ionity" -> {

                    maingauPrice = ChargeCards(
                        identifier = "maingau_personalized",
                        name = "Einfach Strom Laden",
                        provider = "Maingau",
                        price = maingauAcPrice,
                        updated = System.currentTimeMillis() / 1000L
                    )
                }
                type == "dc" && pocOperator.lowercase() != "ionity" -> {
                    maingauPrice = ChargeCards(
                        identifier = "maingau_personalized",
                        name = "Einfach Strom Laden",
                        provider = "Maingau",
                        price = maingauDcPrice,
                        updated = System.currentTimeMillis() / 1000L
                    )
                }
            }
        }
        return maingauPrice
    }

    private fun drawChargeCard(
        textToDraw: String = "N/A",
        textSize: Float = 8F,
        textColor: Int = Color.BLACK,
        rectangleColor: Int = Color.LTGRAY,
        backgroundColor: Int = Color.WHITE,
        paintStroke: Boolean = false
    ):Bitmap?{
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
        if(paintStroke) {
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)  // stroke
        }

        // text paint to draw text
        val textPaint = TextPaint().apply {
            color = textColor
            textAlign  = Paint.Align.LEFT
            this.textSize = cardTextSize-strokeWidth/scaleFactor
            isAntiAlias = true
        }
        textPaint.typeface = Typeface.create("Roboto",Typeface.BOLD)

        //calculating if breaking is needed in order to correctly position text on the Y Axis
        val numOfChars = textPaint.breakText(
            textToDraw,
            true,
            rectF.width(),
            null
        )

        val textLines = ceil(textToDraw.length.toDouble() / numOfChars.toDouble())

        var textPositionYOffset = canvas.height.toDouble() / 2.0 - (textLines * cardTextSize.toDouble()/2.0 + 2.0 * strokeWidth.toDouble())

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

    fun manipulateColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Math.round(Color.red(color) * factor)
        val g = Math.round(Color.green(color) * factor)
        val b = Math.round(Color.blue(color) * factor)
        return Color.argb(
            a,
            Math.min(r, 255),
            Math.min(g, 255),
            Math.min(b, 255)
        )
    }

    fun printLog(message: String, type: String = "info"){
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

    fun showCardDetails(title: String = "", message: String = ""){
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

        val staticLayout = StaticLayoutCache[cacheKey] ?:
        StaticLayout.Builder.obtain(text, start, end, textPaint, width)
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

        val staticLayout = StaticLayoutCache[cacheKey] ?:
        StaticLayout.Builder.obtain(text, start, end, textPaint, width)
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
        val staticLayout = StaticLayoutCache[cacheKey] ?:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                curOverlay = this.view?.findViewById<ConstraintLayout>(R.id.onboarding_1)
            }
            2 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_1)?.visibility = View.GONE
                curOverlay = this.view?.findViewById<ConstraintLayout>(R.id.onboarding_2)
            }
            3 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_2)?.visibility = View.GONE
                curOverlay = this.view?.findViewById<ConstraintLayout>(R.id.onboarding_3)
            }
            4 -> {
                this.view?.findViewById<ConstraintLayout>(R.id.onboarding_3)?.visibility = View.GONE
                val edit = PreferenceManager.getDefaultSharedPreferences(context).edit()
                edit.putBoolean("firstStart", false).apply()
                return
            }
        }
        curOverlay?.visibility = View.VISIBLE
        curOverlay?.setOnClickListener {
                onboarding(step+1)
        }
    }


}
