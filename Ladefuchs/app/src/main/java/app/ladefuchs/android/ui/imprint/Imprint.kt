package app.ladefuchs.android.ui.imprint

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.R


class Imprint : Fragment() {
    companion object {
        fun newInstance() = Imprint()
    }

    private lateinit var viewModel: ImprintViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<ImageButton>(app.ladefuchs.android.R.id.back_button).setOnClickListener {
            findNavController().navigate(app.ladefuchs.android.R.id.action_imprint_to_navigation_about)
        }

    }
}