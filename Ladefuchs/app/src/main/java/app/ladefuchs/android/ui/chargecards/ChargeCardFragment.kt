package app.ladefuchs.android.ui.chargecards

import android.content.Intent
import android.content.Intent.getIntent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
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
import app.ladefuchs.android.R
import app.ladefuchs.android.R.id.action_navigation_chargecards_to_navigation_about
import app.ladefuchs.android.helper.*
import com.aigestudio.wheelpicker.WheelPicker
import kotlinx.android.synthetic.main.fragment_chargecards.*
import kotlinx.android.synthetic.main.fragment_chargecards.view.*

//import com.tylerthrailkill.helpers.prettyprint

class ChargeCardFragment : Fragment() {
    private var useBetaAPI: Boolean = false
    private var onboarding: Boolean = true
    private var showBanner: Boolean = true
    private var promoProbabilities: Array<String> = arrayOf(
        "quote",
        "twitter", "twitter",
        "shop", "shop", "shop",
        "thg", "thg", "thg", "thg", "thg",
    )
    private var pocOperatorList: List<String> = listOf()
    private var currentPoc: String = ""
    private var api: API? = null
    private var prefs: SharedPreferences? = null
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

        printLog("Operator List $pocOperatorList")

        if (pocOperatorList.isEmpty())
            return

        currentPoc = pocOperatorList[0]
        printLog("First Operator is $currentPoc")
        // add easterEggOnclickListener
        easterEgg(view)
        //initialize Price List
        getPrices(
            currentPoc,
            forceDownload = false,
            requireContext(),
            api!!,
            view,
            resources
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
                forceDownload = false,
                requireContext(),
                api!!,
                view,
                resources
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
            getPrices(currentPoc, forceDownload = true, requireContext(), api!!, view, resources)
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
    @RequiresApi(Build.VERSION_CODES.R)
    private fun drawPromoBanner(view: View, promoType: String, promoURL: String) {
        val viewWidth = getScreenWidth()
        val viewHeight = 280 * viewWidth!! / 1170
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
            val intent = Intent(Intent.ACTION_VIEW)
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = buttonURL
            startActivity(intent)
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
