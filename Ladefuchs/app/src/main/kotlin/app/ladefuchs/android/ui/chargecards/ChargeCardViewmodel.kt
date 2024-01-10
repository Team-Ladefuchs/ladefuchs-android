package app.ladefuchs.android.ui.chargecards

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ChargeCardViewmodel : ViewModel() {
    val uiState = MutableStateFlow(ChargeCardUiState())
}
