package app.ladefuchs.android.ui.chargecards

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.core.text.HtmlCompat.fromHtml
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.helper.printLog


class ChargeCardDetail : Fragment() {
    companion object {
        fun newInstance() = ChargeCardDetail()
    }

    private lateinit var viewModel: ChargeCardDetailViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val data = requireArguments().getParcelable<ChargeCards>("cardData")!!
        val layout: View = inflater.inflate(R.layout.card_detail_dialog, container, false)
        layout.findViewById<TextView>(R.id.detail_header).text = data.name
        val textView = layout.findViewById<TextView>(R.id.textView2)
        var textViewText =
        """
        Provider: ${data.provider}
        Preis pro kWh: ${String.format("%.2f", data.price)}  
        ${fromHtml("Link zur Karte: ${data.url}",FROM_HTML_MODE_COMPACT)}
        """
        if (data.blockingFeeStart != 0)
            textViewText+="Start der Blockiergebühr nach ${data.blockingFeeStart} Minuten"
        if (data.monthlyFee != 0.0f)
            textViewText+="Monatliche Gebühr:${String.format(" % .2f",data.monthlyFee)}"
        textView.text = textViewText
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            findNavController().navigate(R.id.action_detail_to_main_screen)
        }

        view.findViewById<TextView>(R.id.button_ok).setOnClickListener {
            findNavController().navigate(R.id.action_detail_to_main_screen)
        }

    }
}