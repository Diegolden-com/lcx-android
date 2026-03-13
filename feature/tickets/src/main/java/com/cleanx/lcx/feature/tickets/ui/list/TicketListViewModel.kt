package com.cleanx.lcx.feature.tickets.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanx.lcx.core.model.Ticket
import com.cleanx.lcx.core.session.SessionManager
import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.ErrorMessages
import com.cleanx.lcx.feature.tickets.data.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketListUiState(
    val isLoading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TicketListViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: TicketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketListUiState())
    val uiState: StateFlow<TicketListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            when (val result = repository.getTickets()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tickets = result.data,
                            error = null,
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = ErrorMessages.forCode(result.code, result.message),
                        )
                    }
                }
            }
        }
    }

    fun addCreatedTicket(ticket: Ticket) {
        _uiState.update { state ->
            val withoutDuplicate = state.tickets.filterNot { it.id == ticket.id }
            state.copy(
                tickets = listOf(ticket) + withoutDuplicate,
                error = null,
            )
        }
    }

    fun updateTicketInList(updated: Ticket) {
        _uiState.update { state ->
            val exists = state.tickets.any { it.id == updated.id }
            state.copy(
                tickets = if (exists) {
                    state.tickets.map { if (it.id == updated.id) updated else it }
                } else {
                    listOf(updated) + state.tickets
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onSignedOut()
        }
    }
}
