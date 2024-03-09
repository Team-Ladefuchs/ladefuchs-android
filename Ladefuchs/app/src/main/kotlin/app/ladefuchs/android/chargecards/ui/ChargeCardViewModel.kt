package app.ladefuchs.android.chargecards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ladefuchs.android.chargecards.data.model.Operator
import app.ladefuchs.android.chargecards.data.model.PlugType
import app.ladefuchs.android.chargecards.domain.GetCardsUseCase
import app.ladefuchs.android.chargecards.domain.GetOperatorsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChargeCardViewModel(
    private val getOperatorsUseCase: GetOperatorsUseCase,
    private val getCardsUseCase: GetCardsUseCase,
) : ViewModel() {
    val uiState = MutableStateFlow(ChargeCardUiState())

    init {
        loadPoCOperators()
    }

    fun handleEvent(event: ChargeCardEvent) {
        when (event) {
            ChargeCardEvent.LogoClicked -> increaseNerdCount()
            is ChargeCardEvent.SelectOperator -> selectOperator(event.operator)
        }
    }

    /**
     * Load enabled operators initially
     */
    private fun loadPoCOperators() {
        viewModelScope.launch(Dispatchers.IO) {
            getOperatorsUseCase().collect { operators ->
                operators.getOrNull(0)?.let {
                    selectOperator(it)
                }
                withContext(Dispatchers.Main) {
                    uiState.update {
                        it.copy(pocOperators = operators)
                    }
                }
            }
        }
    }

    /**
     * select the current operator and load its cards
     */
    private fun selectOperator(operator: Operator) {
        viewModelScope.launch(Dispatchers.IO) {
            // fetch fees for operator
            getCardsUseCase(operatorId = operator.identifier).collect { cards ->
                withContext(Dispatchers.Main) {
                    uiState.update {
                        it.copy(
                            acItems = cards[PlugType.AC] ?: emptyList(),
                            dcItems = cards[PlugType.DC] ?: emptyList()
                        )
                    }
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
