package com.cleanx.lcx.feature.water.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanx.lcx.core.model.WaterLevelStatus
import com.cleanx.lcx.feature.water.data.TANK_CAPACITY_LITERS
import com.cleanx.lcx.feature.water.data.WATER_PROVIDERS
import com.cleanx.lcx.feature.water.data.WaterLevelWithUser
import com.cleanx.lcx.feature.water.data.WaterProvider
import com.cleanx.lcx.feature.water.data.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class WaterUiState(
    // Loading / error
    val isLoading: Boolean = true,
    val error: String? = null,

    // Current level
    val currentPercentage: Int = 0,
    val currentLiters: Int = 0,
    val currentStatus: WaterLevelStatus = WaterLevelStatus.CRITICAL,

    // Input fields (slider/manual entry)
    val inputPercentage: Int = 0,
    val inputLiters: Int = 0,
    val percentageText: String = "0",
    val litersText: String = "0",

    // Saving state
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,

    // History
    val isLoadingHistory: Boolean = true,
    val history: List<WaterLevelWithUser> = emptyList(),
    val historyError: String? = null,

    // Order water
    val selectedProviderId: String? = null,
    val isOrdering: Boolean = false,
    val orderSuccess: Boolean = false,
    val orderError: String? = null,

    // Tab
    val selectedTab: Int = 0,
) {
    val inputStatus: WaterLevelStatus
        get() = WaterRepository.percentageToStatus(inputPercentage)

    val isCritical: Boolean
        get() = currentPercentage < 20

    val selectedProvider: WaterProvider?
        get() = WATER_PROVIDERS.find { it.id == selectedProviderId }
}

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val repository: WaterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    init {
        loadCurrentLevel()
        loadHistory()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    // -- Current level --------------------------------------------------------

    fun loadCurrentLevel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getCurrentWaterLevel()
                .onSuccess { record ->
                    val percentage = record?.levelPercentage ?: 0
                    val liters = record?.liters ?: 0
                    val status = record?.status ?: WaterLevelStatus.CRITICAL
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentPercentage = percentage,
                            currentLiters = liters,
                            currentStatus = status,
                            inputPercentage = percentage,
                            inputLiters = liters,
                            percentageText = percentage.toString(),
                            litersText = liters.toString(),
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load current water level")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Error al cargar nivel de agua",
                        )
                    }
                }
        }
    }

    // -- Slider / manual input ------------------------------------------------

    fun onSliderChange(percentage: Int) {
        val clamped = percentage.coerceIn(0, 100)
        val liters = (clamped * TANK_CAPACITY_LITERS) / 100
        _uiState.update {
            it.copy(
                inputPercentage = clamped,
                inputLiters = liters,
                percentageText = clamped.toString(),
                litersText = liters.toString(),
                saveSuccess = false,
            )
        }
    }

    fun onPercentageTextChange(text: String) {
        val cleaned = text.filter { it.isDigit() }
        val value = cleaned.toIntOrNull() ?: 0
        val clamped = value.coerceIn(0, 100)
        val liters = (clamped * TANK_CAPACITY_LITERS) / 100
        _uiState.update {
            it.copy(
                percentageText = cleaned,
                inputPercentage = clamped,
                inputLiters = liters,
                litersText = liters.toString(),
                saveSuccess = false,
            )
        }
    }

    fun onLitersTextChange(text: String) {
        val cleaned = text.filter { it.isDigit() }
        val value = cleaned.toIntOrNull() ?: 0
        val clamped = value.coerceIn(0, TANK_CAPACITY_LITERS)
        val percentage = (clamped * 100) / TANK_CAPACITY_LITERS
        _uiState.update {
            it.copy(
                litersText = cleaned,
                inputLiters = clamped,
                inputPercentage = percentage,
                percentageText = percentage.toString(),
                saveSuccess = false,
            )
        }
    }

    // -- Save level -----------------------------------------------------------

    fun saveLevel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null, saveSuccess = false) }
            val state = _uiState.value
            repository.recordWaterLevel(
                percentage = state.inputPercentage,
            ).onSuccess {
                Timber.d("Water level saved: %d%%", state.inputPercentage)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        currentPercentage = state.inputPercentage,
                        currentLiters = state.inputLiters,
                        currentStatus = state.inputStatus,
                    )
                }
                // Refresh history after save
                loadHistory()
            }.onFailure { e ->
                Timber.e(e, "Failed to save water level")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = e.message ?: "Error al guardar nivel",
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    // -- History --------------------------------------------------------------

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, historyError = null) }
            repository.getWaterLevelHistory()
                .onSuccess { records ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            history = records,
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load water level history")
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            historyError = e.message ?: "Error al cargar historial",
                        )
                    }
                }
        }
    }

    // -- Order water ----------------------------------------------------------

    fun selectProvider(providerId: String) {
        _uiState.update {
            it.copy(
                selectedProviderId = providerId,
                orderSuccess = false,
                orderError = null,
            )
        }
    }

    fun orderWater() {
        val provider = _uiState.value.selectedProvider ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isOrdering = true, orderError = null, orderSuccess = false) }
            repository.recordWaterOrder(
                provider = provider,
                currentPercentage = _uiState.value.currentPercentage,
            ).onSuccess {
                Timber.d("Water order recorded: %s", provider.name)
                _uiState.update {
                    it.copy(
                        isOrdering = false,
                        orderSuccess = true,
                    )
                }
                loadHistory()
            }.onFailure { e ->
                Timber.e(e, "Failed to record water order")
                _uiState.update {
                    it.copy(
                        isOrdering = false,
                        orderError = e.message ?: "Error al registrar pedido",
                    )
                }
            }
        }
    }

    fun clearOrderSuccess() {
        _uiState.update { it.copy(orderSuccess = false) }
    }

    // -- Error clearing -------------------------------------------------------

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun clearOrderError() {
        _uiState.update { it.copy(orderError = null) }
    }
}
