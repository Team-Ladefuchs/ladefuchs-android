package app.ladefuchs.android.ui.chargecards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChargeCardViewModel : ViewModel() {
    val uiState = MutableStateFlow(ChargeCardUiState())

    init {
        loadChargingFees()
    }

    fun handleEvent(event: ChargeCardEvent) {
        when (event) {
            ChargeCardEvent.LogoClicked -> increaseNerdCount()
        }
    }

    /**
     * Fetch newest data from backend
     */
    private fun loadChargingFees() {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: fetch data
            withContext(Dispatchers.Main) {
                uiState.update {
                    // TODO: update lists
                    it.copy(
                        acItems = emptyList(),
                        dcItems = emptyList()
                    )
                }
            }
        }
    }

    private fun increaseNerdCount() {
        uiState.update {
            it.copy(
                easterEggCount = it.easterEggCount + 1,
                isEasterEggEnabled = it.easterEggCount >= 20
            )
        }
    }
}
