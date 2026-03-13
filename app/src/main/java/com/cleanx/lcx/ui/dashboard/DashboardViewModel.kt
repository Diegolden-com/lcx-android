package com.cleanx.lcx.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val snapshot: DashboardSnapshot? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasSnapshot = _uiState.value.snapshot != null
            _uiState.update { state ->
                state.copy(
                    isLoading = !hasSnapshot,
                    isRefreshing = hasSnapshot,
                    error = if (hasSnapshot) state.error else null,
                )
            }

            runCatching { repository.loadSnapshot() }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            snapshot = snapshot,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "No se pudo cargar el dashboard operativo.",
                        )
                    }
                }
        }
    }
}
