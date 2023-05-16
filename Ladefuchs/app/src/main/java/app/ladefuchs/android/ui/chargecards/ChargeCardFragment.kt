package app.ladefuchs.android.ui.chargecards

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
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
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.ladefuchs.android.R
import app.ladefuchs.android.R.id.action_navigation_chargecards_to_navigation_about
import app.ladefuchs.android.dataClasses.Banner
import app.ladefuchs.android.dataClasses.Operator
import app.ladefuchs.android.helper.*
import com.aigestudio.wheelpicker.WheelPicker
import java.io.File
import java.nio.file.Paths

//import com.tylerthrailkill.helpers.prettyprint

class ChargeCardFragment : Fragment() {
    private var useBetaAPI: Boolean = false
    private var onboarding: Boolean = true
    private var showBanner: Boolean = true
    private var pocOperatorList: List<Operator> = listOf()
    private var currentPoc: Operator? = null
    private var api: API? = null
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
        // Update vars to represent User Preferences
        api = API(requireContext().applicationContext)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        useBetaAPI = prefs!!.getBoolean("useBetaAPI", false)
        onboarding = prefs!!.getBoolean("firstStart", true)
        showBanner = prefs!!.getBoolean("showBanner", true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chargecards, container, false)
    }

    /**
     * This function is called after creation and initialises the UI
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        phraseView = view.findViewById(R.id.phraseView) as TextView
        swipetorefresh = view.findViewById(R.id.swipetorefresh) as SwipeRefreshLayout
        val nerdGlasses = view.findViewById<ImageView>(R.id.nerd_glasses)
        // check whether onboarding should be shown
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

        printLog("Operator List $pocOperatorList")

        if (pocOperatorList.isEmpty())
            return

        currentPoc = pocOperatorList[0]
        // add easterEggOnclickListener
        easterEgg(view)
        //initialize Price List
        printLog("Triggering Refresh with $currentPoc")
        cardsNeedRefresh = getPrices(
            currentPoc!!,
            forceDownload = false,
            requireContext(),
            api!!,
            view,
            resources
        )
        if (cardsNeedRefresh) {
            printLog("Triggering view Refresh @124")
            refreshCardView(currentPoc!!)
        }
        // initialize picker
        val wheelPicker = view.findViewById(R.id.pocSelector) as WheelPicker
        //Switch to a more 3D, iOS-style Look
        wheelPicker.setAtmospheric(true)
        wheelPicker.isCurved = true
        wheelPicker.data = pocOperatorList.toMutableList()
        // Loading the pocList into the Picker Library
        wheelPicker.setOnItemSelectedListener { _, data, _ ->
            view.findViewById<ScrollView>(R.id.cardScroller).fullScroll(ScrollView.FOCUS_UP)
            printLog("CPO selected: $data")
            cardsNeedRefresh = getPrices(
                data as Operator,
                forceDownload = false,
                requireContext(),
                api!!,
                view,
                resources
            )
            printLog("Picker Switched to CPO: $data")
            if (cardsNeedRefresh) {
                refreshCardView(currentPoc!!)
            }
            currentPoc = data
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
            printLog("Swipe to Refresh with $currentPoc")
            getPrices(currentPoc!!, forceDownload = true, requireContext(), api!!, view, resources)
            swipetorefresh.isRefreshing = false
        }
        // check whether onboarding should be shown
        if (onboarding) {
            onboarding()
        }

        //check if user previously selected cards
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val selectedCards = sharedPreferences.getString("selectedChargeCards", null)
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
            val preferences: SharedPreferences.Editor? = sharedPreferences.edit()
            preferences?.remove("selectedChargeCards")?.apply()
        }

    }

    private fun refreshCardView(CPOSelected: Operator) {
        printLog("Refreshing Charge Card View with $CPOSelected")
        view?.let {
            getPrices(
                CPOSelected, false, requireContext(), api!!,
                it, resources, true
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
    @RequiresApi(Build.VERSION_CODES.R)
    private fun retrieveFooterContent(view: View) {
        if (showBanner) {
            val banner: Banner = api!!.retrieveBanners()
            if (File("${requireContext().filesDir}/${banner.filename}").exists()) {
                drawPromoBanner(
                    view,
                    banner
                )
                return
            }
        }
        val phrases =
            requireContext().applicationContext.assets?.open("phrases.txt")?.bufferedReader()
                .use { it?.readLines() }
        printLog("Falling back on your mom", "debug")
        phraseView.text = phrases?.random() ?: ""

    }

    /**
     * This function draws the banner content
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun drawPromoBanner(
        view: View,
        banner: Banner
    ) {
        val viewWidth = getScreenWidth()
        val viewHeight = 280 * viewWidth / 1100
        val phraseContainer = view.findViewById(R.id.phraseContainer) as LinearLayout
        phraseContainer.removeView(phraseView)
        val phraseContainerParams = phraseContainer.layoutParams
        phraseContainerParams.height = viewHeight - 35
        phraseContainer.setBackgroundColor(Color.parseColor("#FFCEC0AC"))
        phraseContainer.layoutParams = phraseContainerParams

        val bannerView = view.findViewById(R.id.bannerView) as LinearLayout
        bannerView.visibility = VISIBLE
        val bannerButton = view.findViewById(R.id.bannerImage) as ImageButton
        val bitmapImage = Drawable.createFromPath(
            Paths.get(requireContext().filesDir.toString() + "/" + banner.filename)
                .toString()
        )!! as BitmapDrawable
        val drawableImage = BitmapDrawable(
            resources,
            Bitmap.createBitmap(
                bitmapImage.bitmap,
                70,
                0,
                bitmapImage.bitmap.width - 130,
                viewHeight + 55
            )
        )

        bannerButton.setImageDrawable(
            drawableImage
        )

        bannerButton.requestLayout()
        bannerView.setBackgroundColor(Color.parseColor("#00FFFFFF"))
        val buttonURL = Uri.parse(banner.link)
        val bannerParams = bannerButton.layoutParams
        bannerParams.width = viewWidth
        bannerParams.height = viewHeight
        bannerButton.scaleType = ScaleType.CENTER_INSIDE
        bannerButton.setBackgroundColor(Color.parseColor("#00FFFFFF"))
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
