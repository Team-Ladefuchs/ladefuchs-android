package app.ladefuchs.android.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.R


class AboutFragment : Fragment() {

    private lateinit var aboutViewModel: AboutViewModel

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        aboutViewModel =
            ViewModelProviders.of(this).get(AboutViewModel::class.java)

        return inflater.inflate(R.layout.fragment_about, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<ImageView>(R.id.back_button).setOnClickListener {
            findNavController().navigate(R.id.action_navigation_about_to_navigation_chargecards)
        }

        view.findViewById<Button>(R.id.imprint_button).setOnClickListener {
            findNavController().navigate(R.id.action_navigation_about_to_imprint)
        }

        view.findViewById<Button>(app.ladefuchs.android.R.id.ack_button).setOnClickListener {
            findNavController().navigate(app.ladefuchs.android.R.id.action_navigation_about_to_acknowledgement)
        }

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
        val VersionHolder: TextView = view.findViewById(R.id.version_info)
        VersionHolder.text = "Version $versionName"


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

}
