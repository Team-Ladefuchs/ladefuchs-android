package app.ladefuchs.android.ui.feedbackfuchs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.R


class Feedbackfuchs : Fragment() {

    companion object {
        fun newInstance() = Feedbackfuchs()
    }

    private lateinit var viewModel: FeedbackfuchsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_feedbackfuchs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<ImageButton>(app.ladefuchs.android.R.id.back_button).setOnClickListener {
            findNavController().navigate(app.ladefuchs.android.R.id.action_feedbackfuchs_to_navigation_about)
        }

        val webView: WebView = view.findViewById<WebView>(app.ladefuchs.android.R.id.webView)
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("https://gitlab.schlingel.io/ladefuchs/feedbackfuchs/-/issues")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return false
            }
        }


    }

}