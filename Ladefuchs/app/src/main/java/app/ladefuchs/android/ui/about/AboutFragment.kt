package app.ladefuchs.android.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.R


class AboutFragment : Fragment() {

    private lateinit var aboutViewModel: AboutViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        aboutViewModel =
                ViewModelProviders.of(this).get(AboutViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_about, container, false)

        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<ImageView>(R.id.back_button).setOnClickListener {
            findNavController().navigate(R.id.action_navigation_about_to_navigation_chargecards)
        }

        view.findViewById<Button>(app.ladefuchs.android.R.id.imprint_button).setOnClickListener {
            findNavController().navigate(app.ladefuchs.android.R.id.action_navigation_about_to_imprint)
        }
/*
        view.findViewById<ImageView>(R.id.bug_icon).setOnClickListener {
            findNavController().navigate(R.id.action_navigation_about_to_feedbackfuchs)
        }
*/
        /*
        val webView: WebView = view.findViewById<WebView>(R.id.about_us)

        webView.loadUrl("file:///android_asset/aboutus.html")
        */

        //Hiding settings and making them collapsible
        val settingsFragmentView = view.findViewById<View>(R.id.settingsFragment)
        val settingsDisclosureTriangle = view.findViewById<ImageView>(R.id.disclosure_triangle)
        view.findViewById<LinearLayout>(R.id.einstellungen).removeView(settingsFragmentView)

        view.findViewById<TextView>(R.id.einstellungenHeaderText).setOnClickListener {
            if(view.findViewById<View>(R.id.settingsFragment) != null) {
                view.findViewById<LinearLayout>(R.id.einstellungen).removeView(settingsFragmentView)
                settingsDisclosureTriangle.rotation = 0F
            } else {
                view.findViewById<LinearLayout>(R.id.einstellungen).addView(settingsFragmentView)
                settingsDisclosureTriangle.rotation = 180F
            }
        }

        view.findViewById<ImageView>(R.id.disclosure_triangle).setOnClickListener {
            if(view.findViewById<View>(R.id.settingsFragment) != null) {
                view.findViewById<LinearLayout>(R.id.einstellungen).removeView(settingsFragmentView)
                settingsDisclosureTriangle.rotation = 0F
            } else {
                view.findViewById<LinearLayout>(R.id.einstellungen).addView(settingsFragmentView)
                settingsDisclosureTriangle.rotation = 180F
            }
        }

        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        val VersionHolder: TextView = view.findViewById<TextView>(R.id.version_info)
        VersionHolder.text = "Version " + versionName.toString()

        // Making Links in Textviews Clickable... well... really...
        val schlingelSL2 = view.findViewById(R.id.bastiSL2) as TextView
        schlingelSL2.movementMethod = LinkMovementMethod.getInstance()

        val malikSL2 = view.findViewById(R.id.malikSL2) as TextView
        malikSL2.movementMethod = LinkMovementMethod.getInstance()

        val illuSL2 = view.findViewById(R.id.illufuchsSL) as TextView
        illuSL2.movementMethod = LinkMovementMethod.getInstance()

        // On Click Listeners for Images
        val chargePriceLogo = view.findViewById(R.id.chargeprice_logo) as ImageView
        chargePriceLogo.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(it.tag.toString())
            startActivity(intent)
        }

        val audiodDumpLogo = view.findViewById(R.id.podcast_audiodump) as ImageView
        audiodDumpLogo.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(it.tag.toString())
            startActivity(intent)
        }

        val cleanElectricLogo = view.findViewById(R.id.podcast_cleanelectric) as ImageView
        cleanElectricLogo.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(it.tag.toString())
            startActivity(intent)
        }

        val bitsundsoLogo = view.findViewById(R.id.podcast_bitsundso) as ImageView
        bitsundsoLogo.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(it.tag.toString())
            startActivity(intent)
        }

    }

    fun toggleSettings () {

    }



}
