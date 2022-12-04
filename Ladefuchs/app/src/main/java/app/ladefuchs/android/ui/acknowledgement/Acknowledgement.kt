package app.ladefuchs.android.ui.acknowledgement

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import app.ladefuchs.android.R


class Acknowledgement : Fragment() {
    companion object {
        fun newInstance() = Acknowledgement()
    }

    private lateinit var viewModel: AcknowledgementViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_acknowledgement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val acknowledgementText = view.findViewById(R.id.acknowledgement_text) as TextView
        acknowledgementText.movementMethod = LinkMovementMethod.getInstance()

        view.findViewById<ImageButton>(app.ladefuchs.android.R.id.back_button).setOnClickListener {
            findNavController().navigate(app.ladefuchs.android.R.id.action_acknowledgement_to_navigation_about)
        }

    }
}