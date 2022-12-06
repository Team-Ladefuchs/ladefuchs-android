package app.ladefuchs.android.ui.chargecards

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.ladefuchs.android.R


class ChargeCardDetail : Fragment() {
    companion object {
        fun newInstance() = ChargeCardDetail()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.card_detail_dialog, container, false)
    }
}