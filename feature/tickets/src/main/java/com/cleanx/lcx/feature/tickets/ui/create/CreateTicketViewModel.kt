package com.cleanx.lcx.feature.tickets.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanx.lcx.core.model.Ticket
import com.cleanx.lcx.core.network.TicketDraft
import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.ErrorMessages
import com.cleanx.lcx.feature.tickets.data.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class CreateTicketUiState(
    val customerName: String = "",
    val customerPhone: String = "",
    val serviceType: String = "wash-fold",
    val service: String = "",
    val weight: String = "",
    val notes: String = "",
    val promisedPickupDate: String = "",
    val specialInstructions: String = "",
    val addOns: String = "",
    val addOnsTotal: String = "",
    val totalAmount: String = "",
    val paymentStatus: String = "pending",
    val paymentMethod: String = "card",
    val paidAmount: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdTicket: Ticket? = null,
)

@HiltViewModel
class CreateTicketViewModel @Inject constructor(
    private val repository: TicketRepository,
) : ViewModel() {

    private sealed interface ParseDoubleResult {
        data class Success(val value: Double?) : ParseDoubleResult
        data object Invalid : ParseDoubleResult
    }

    private val _uiState = MutableStateFlow(CreateTicketUiState())
    val uiState: StateFlow<CreateTicketUiState> = _uiState.asStateFlow()

    fun onCustomerNameChanged(value: String) {
        _uiState.update { it.copy(customerName = value, error = null) }
    }

    fun onCustomerPhoneChanged(value: String) {
        _uiState.update { it.copy(customerPhone = value, error = null) }
    }

    fun onServiceTypeChanged(value: String) {
        _uiState.update { it.copy(serviceType = value, error = null) }
    }

    fun onServiceChanged(value: String) {
        _uiState.update { it.copy(service = value, error = null) }
    }

    fun onWeightChanged(value: String) {
        _uiState.update { it.copy(weight = value, error = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun onPromisedPickupDateChanged(value: String) {
        _uiState.update { it.copy(promisedPickupDate = value, error = null) }
    }

    fun onSpecialInstructionsChanged(value: String) {
        _uiState.update { it.copy(specialInstructions = value, error = null) }
    }

    fun onAddOnsChanged(value: String) {
        _uiState.update { it.copy(addOns = value, error = null) }
    }

    fun onAddOnsTotalChanged(value: String) {
        _uiState.update { it.copy(addOnsTotal = value, error = null) }
    }

    fun onTotalAmountChanged(value: String) {
        _uiState.update { it.copy(totalAmount = value, error = null) }
    }

    fun onPaymentStatusChanged(value: String) {
        _uiState.update {
            it.copy(
                paymentStatus = value,
                error = null,
                paidAmount = if (value == "pending") "" else it.paidAmount,
            )
        }
    }

    fun onPaymentMethodChanged(value: String) {
        _uiState.update { it.copy(paymentMethod = value, error = null) }
    }

    fun onPaidAmountChanged(value: String) {
        _uiState.update { it.copy(paidAmount = value, error = null) }
    }

    fun submit() {
        val state = _uiState.value

        if (state.isSubmitting) return

        if (state.customerName.isBlank()) {
            _uiState.update { it.copy(error = "El nombre del cliente es obligatorio.") }
            return
        }
        if (state.customerPhone.isBlank()) {
            _uiState.update { it.copy(error = "El telefono del cliente es obligatorio.") }
            return
        }
        if (state.service.isBlank()) {
            _uiState.update { it.copy(error = "El servicio es obligatorio.") }
            return
        }

        val parsedWeight = when (
            val result = parseOptionalDouble(
            value = state.weight,
            errorMessage = "El peso debe ser un numero valido.",
        )
        ) {
            is ParseDoubleResult.Success -> result.value
            ParseDoubleResult.Invalid -> return
        }

        val parsedAddOnsTotal = when (
            val result = parseOptionalDouble(
            value = state.addOnsTotal,
            errorMessage = "El monto de add-ons debe ser un numero valido.",
        )
        ) {
            is ParseDoubleResult.Success -> result.value
            ParseDoubleResult.Invalid -> return
        }

        val parsedTotalAmount = when (
            val result = parseOptionalDouble(
            value = state.totalAmount,
            errorMessage = "El monto total debe ser un numero valido.",
        )
        ) {
            is ParseDoubleResult.Success -> result.value
            ParseDoubleResult.Invalid -> return
        }

        val parsedPaidAmount = when (
            val result = parseOptionalDouble(
            value = state.paidAmount,
            errorMessage = "El monto pagado debe ser un numero valido.",
        )
        ) {
            is ParseDoubleResult.Success -> result.value
            ParseDoubleResult.Invalid -> return
        }

        val promisedPickupDate = state.promisedPickupDate.trim().ifBlank { null }
        if (promisedPickupDate != null && !isIsoLocalDate(promisedPickupDate)) {
            _uiState.update { it.copy(error = "La fecha promesa debe usar formato AAAA-MM-DD.") }
            return
        }

        if (
            parsedAddOnsTotal != null &&
            parsedTotalAmount != null &&
            parsedAddOnsTotal > parsedTotalAmount
        ) {
            _uiState.update { it.copy(error = "Los add-ons no pueden exceder el total.") }
            return
        }

        val effectivePaidAmount = when (state.paymentStatus) {
            "pending" -> null
            else -> parsedPaidAmount ?: parsedTotalAmount
        }

        if (state.paymentStatus != "pending" && effectivePaidAmount == null) {
            _uiState.update {
                it.copy(error = "Captura el monto pagado o el total del encargo.")
            }
            return
        }

        val addOns = state.addOns
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { emptyList() }

        val subtotal = if (parsedTotalAmount != null && parsedAddOnsTotal != null) {
            (parsedTotalAmount - parsedAddOnsTotal).takeIf { it >= 0.0 }
        } else {
            null
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val draft = TicketDraft(
                customerName = state.customerName.trim(),
                customerPhone = state.customerPhone.trim(),
                serviceType = state.serviceType,
                service = state.service.trim(),
                weight = parsedWeight,
                notes = state.notes.trim().ifBlank { null },
                totalAmount = parsedTotalAmount,
                subtotal = subtotal,
                addOnsTotal = parsedAddOnsTotal,
                addOns = addOns.ifEmpty { null },
                promisedPickupDate = promisedPickupDate,
                specialInstructions = state.specialInstructions.trim().ifBlank { null },
                paymentMethod = if (state.paymentStatus == "pending") null else state.paymentMethod,
                paymentStatus = state.paymentStatus,
                paidAmount = effectivePaidAmount,
                prepaidAmount = if (state.paymentStatus == "prepaid") effectivePaidAmount else null,
            )

            when (val result = repository.createTickets(source = "encargo", tickets = listOf(draft))) {
                is ApiResult.Success -> {
                    val ticket = result.data.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = null,
                            createdTicket = ticket,
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = ErrorMessages.forCode(result.code, result.message),
                        )
                    }
                }
            }
        }
    }

    fun clearCreated() {
        _uiState.update { it.copy(createdTicket = null) }
    }

    private fun parseOptionalDouble(
        value: String,
        errorMessage: String,
    ): ParseDoubleResult {
        if (value.isBlank()) {
            return ParseDoubleResult.Success(null)
        }

        val parsed = value.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = errorMessage) }
            return ParseDoubleResult.Invalid
        }

        return ParseDoubleResult.Success(parsed)
    }

    private fun isIsoLocalDate(value: String): Boolean {
        return try {
            LocalDate.parse(value)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }
}
