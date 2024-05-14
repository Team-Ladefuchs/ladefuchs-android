package app.ladefuchs.android.ui.chargecards

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.StaticLayout
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import android.widget.ImageView.ScaleType
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.Banner
import app.ladefuchs.android.dataClasses.Operator
import app.ladefuchs.android.helper.*
import com.aigestudio.wheelpicker.WheelPicker
import kotlinx.coroutines.delay
import java.net.URL
import kotlin.concurrent.thread

class ChargeCardFragment : Fragment() {
    private var onboarding: Boolean = true
    private var pocOperatorList: List<Operator> = listOf()
    private var currentPoc: Operator? = null
    private var prefs: SharedPreferences? = null
    private var cardsNeedRefresh: Boolean = false
    private lateinit var phraseView: TextView
    private lateinit var swipetorefresh: SwipeRefreshLayout

    object StaticLayoutCache {
        private const val MAX_SIZE = 50 // Arbitrary max number of cached items
        private val cache = lruCache<String, StaticLayout>(MAX_SIZE)
        operator fun set(key: String, staticLayout: StaticLayout) {
            cache.put(key, staticLayout)
        }

        operator fun get(key: String): StaticLayout? {
            return cache[key]
        }
    }

    /**
     * This is the initialisation function that will be called on creation
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefs = PreferenceManager.getDefaultSharedPreferences(inflater.context)
        onboarding = prefs?.getBoolean("firstStart", true) ?: true

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chargecards, container, false)
    }

    /**
     * This function is called after creation and initialises the UI
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        phraseView = view.findViewById(R.id.phraseView) as TextView
        val nerdGlasses = view.findViewById<ImageView>(R.id.nerd_glasses)
        // check whether onboarding should be shown
        // check whether the beta API is used
        if (prefs?.getBoolean("useBetaAPI", false) == true) {
            useBeta()
            phraseView.text = getString(R.string.betaInfoText)
            // add cool nerd glasses
            nerdGlasses.visibility = VISIBLE
        }

        val settingsButton = view.findViewById<ImageButton>(R.id.settingsButton)

        settingsButton.setOnClickListener {
            context?.let { createSettingsPopup(it, view) }
        }

        // retrieve what shall be shown in the footer
        Thread {
            retrieveFooterContent(view)
        }.start()

        // retrieve all operators
        pocOperatorList = retrieveOperatorList(view.context)

        printLog("Operator List $pocOperatorList")

        // initialize picker
        val wheelPicker = view.findViewById<WheelPicker>(R.id.pocSelector)
        //Switch to a more 3D, iOS-style Look
        wheelPicker.setAtmospheric(true)
        wheelPicker.isCurved = true
        wheelPicker.data = pocOperatorList

        if (pocOperatorList.isEmpty()) {
            return
        }

        downloadAllCards(pocOperatorList, view.context)

        currentPoc = pocOperatorList[0]
        // add easterEggOnclickListener
        easterEgg(view)
        //initialize Price List
        printLog("Triggering Refresh with $currentPoc")

        cardsNeedRefresh =
            getPricesByOperatorId(
                currentPoc!!,
                requireContext(),
                view,
            )
        if (cardsNeedRefresh) {
            printLog("Triggering view Refresh @124")
            refreshCardView(currentPoc!!)
        }

        swipetorefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipetorefresh)

        // Loading the pocList into the Picker Library
        wheelPicker.setOnItemSelectedListener { _, data, _ ->
            view.findViewById<ScrollView>(R.id.cardScroller).fullScroll(ScrollView.FOCUS_UP)
            handleOperatorSelected(data as Operator, view)
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
            if (currentPoc != null) {
                printLog("Swipe to Refresh selected operator ${currentPoc!!.displayName}")
                cardsNeedRefresh = getPricesByOperatorId(
                    currentPoc!!.copy(),
                    requireContext(),
                    view,
                    forceDownload = true
                )
                if (cardsNeedRefresh) {
                    refreshCardView(currentPoc!!)
                }
            }
            swipetorefresh.isRefreshing = false
        }
        // check whether onboarding should be shown
        if (onboarding) {
            onboarding()
        }

        Thread {
            Thread.sleep(10_000)
            cleanupOldImageFiles(view.context)
        }.start()

        legacyCardsNotice()
    }

    /**
     * check if user previously selected cards
     */
    private fun legacyCardsNotice() {

        val selectedCards = prefs?.getString("selectedChargeCards", null)
        printLog("Selected Cards: $selectedCards")
        if (selectedCards != null) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Abschied an den Tariffilter")
            builder.setMessage(
                "Lieber Fuchs, liebe Füchsin,\n" +
                        "wir wissen, dass du bisher den Filter für eigene Tarife benutzt hast. \n" +
                        "Der Ladefuchs soll dir aber immer alle günstigen Preise zeigen, damit du die Möglichkeit hast, dir auch den neuen heissen Tarif zu holen, welcher deine Ladung noch etwas günstiger macht.\n" +
                        "Daher gibt es genau diesen Filter ab sofort nicht mehr.\n" +
                        "Sei stark, wir arbeiten an anderen Möglichkeiten der Personalisierung."
            )
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
            }
            builder.show()
            val preferences: SharedPreferences.Editor? = prefs?.edit()
            preferences?.remove("selectedChargeCards")?.apply()
        }
    }

    private fun handleOperatorSelected(operator: Operator, view: View) {
        currentPoc = operator
        printLog("CPO selected: ${operator.displayName}")
        getPricesByOperatorId(
            operator.copy(),
            requireContext(),
            view,
        )

        printLog("Picker Switched to CPO: $operator")
        if (cardsNeedRefresh) {
            refreshCardView(operator)
        }
    }

    private fun refreshCardView(CPOSelected: Operator) {
        printLog("Refreshing Charge Card View for $CPOSelected")
        view?.let {
            getPricesByOperatorId(
                CPOSelected.copy(),
                requireContext(),
                view = it,
            )

        }
        cardsNeedRefresh = false
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
                val useBetaAPI = prefs?.getBoolean("useBetaAPI", false) ?: false
                if (useBetaAPI) {
                    useBeta()
                    phraseView.text =
                        getString(R.string.betaInfoText)
                    nerdGlasses.visibility = VISIBLE
                } else {
                    phraseView.text =
                        getString(R.string.prodInfoText)
                    useProd()
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
    @RequiresApi(Build.VERSION_CODES.R)
    private fun retrieveFooterContent(view: View) {
        val sharedPref = view.context.getSharedPreferences("ladefuchs", Context.MODE_PRIVATE)
        val nextAssertiveBanner = sharedPref.getBoolean("next_assertive_banner", false)
        sharedPref.edit().putBoolean("next_assertive_banner", !nextAssertiveBanner).apply()

        // set the next displayed banner to charge price
        drawBanner(view, retrieveBanners(
            context = view.context,
            nextAssertiveBanner = nextAssertiveBanner
        ))
    }

    /**
     * This function draws the banner content
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun drawBanner(
        view: View,
        banner: Banner?,
    ) {
        if (banner == null || banner.image.isEmpty()) {
            return
        }

        val bannerFilePath = getImagePath(URL(banner.image), view.context);
        if (!bannerFilePath.exists()) {
            return
        }

        val viewWidth = getScreenWidth()
        val viewHeight = 240 * viewWidth / 1100
        val phraseContainer = view.findViewById(R.id.phraseContainer) as LinearLayout
        phraseContainer.removeView(phraseView)
        val phraseContainerParams = phraseContainer.layoutParams
        phraseContainerParams.height = viewHeight - 35
        phraseContainer.setBackgroundColor(Color.parseColor("#FFCEC0AC"))
        phraseContainer.layoutParams = phraseContainerParams

        val bannerButton = view.findViewById<ImageButton>(R.id.bannerImage)

        val bitmapImage = BitmapFactory.decodeFile(bannerFilePath.toString())
        val drawable = BitmapDrawable(resources, bitmapImage)
        bannerButton.setImageDrawable(drawable)
        val targetHeight = if (viewHeight + 55 <= bitmapImage.height) {
            viewHeight + 55
        } else {
            bitmapImage.height
        }
        val drawableImage = BitmapDrawable(
            resources,
            Bitmap.createBitmap(
                bitmapImage,
                70,
                0,
                bitmapImage.width - 130,
                targetHeight
            )
        )
        val bannerView = view.findViewById(R.id.bannerView) as LinearLayout
        bannerView.visibility = VISIBLE
        bannerButton.setImageDrawable(
            drawableImage
        )

        bannerButton.requestLayout()
        val color = Color.parseColor("#00FFFFFF")
        bannerView.setBackgroundColor(color)
        val buttonURL = Uri.parse(banner.link)
        val bannerParams = bannerButton.layoutParams
        bannerParams.width = viewWidth
        bannerParams.height = viewHeight
        bannerButton.scaleType = ScaleType.CENTER_INSIDE
        bannerButton.setBackgroundColor(color)
        bannerButton.layoutParams = bannerParams

        bannerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = buttonURL
            startActivity(intent)
        }
    }


    private fun onboarding(step: Int = 1) {
        phraseView.text = getString(R.string.onboarding_phrase)
        var curOverlay: ConstraintLayout? = null
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
                val edit = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
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
