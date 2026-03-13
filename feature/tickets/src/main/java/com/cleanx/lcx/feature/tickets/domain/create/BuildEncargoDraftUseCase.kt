package com.cleanx.lcx.feature.tickets.domain.create

import com.cleanx.lcx.core.model.PaymentMethod
import com.cleanx.lcx.core.network.TicketDraft
import com.cleanx.lcx.feature.tickets.data.CustomerValidationErrors
import com.cleanx.lcx.feature.tickets.data.buildCustomerValidationErrors
import com.cleanx.lcx.feature.tickets.data.buildEncargoNotes
import com.cleanx.lcx.feature.tickets.data.buildEncargoSpecialInstructions
import com.cleanx.lcx.feature.tickets.data.buildTicketCustomerFields
import com.cleanx.lcx.feature.tickets.data.isCustomerDraftValid
import com.cleanx.lcx.feature.tickets.data.normalizePhone
import com.cleanx.lcx.feature.tickets.data.parsePickupEstimateToIso
import java.time.Instant
import javax.inject.Inject

sealed interface BuildEncargoDraftResult {
    data class Success(val draft: TicketDraft) : BuildEncargoDraftResult
    data class ValidationFailure(
        val message: String,
        val customerValidationErrors: CustomerValidationErrors? = null,
    ) : BuildEncargoDraftResult
}

class BuildEncargoDraftUseCase @Inject constructor() {

    operator fun invoke(
        state: CreateTicketUiState,
        now: Instant = Instant.now(),
    ): BuildEncargoDraftResult {
        val service = state.services.firstOrNull { it.id == state.selectedBaseServiceId }
            ?: return BuildEncargoDraftResult.ValidationFailure(
                message = "Selecciona un servicio base valido.",
            )

        val customerDraft = state.customer.selectedCustomer
        if (!isCustomerDraftValid(customerDraft)) {
            return BuildEncargoDraftResult.ValidationFailure(
                message = "Selecciona o crea un cliente valido con nombre y telefono.",
                customerValidationErrors = buildCustomerValidationErrors(
                    fullName = customerDraft.fullName.trim(),
                    normalizedPhone = normalizePhone(customerDraft.phone),
                    email = customerDraft.email.trim().ifBlank { null },
                ),
            )
        }

        val parsedWeight = state.weight.toDecimalOrNull()
        if (parsedWeight == null || parsedWeight <= 0.0) {
            return BuildEncargoDraftResult.ValidationFailure(
                message = "Ingresa un peso valido mayor a 0 kg.",
            )
        }

        val promisedPickupDate = parsePickupEstimateToIso(state.pickupEstimate.trim())
            ?: return BuildEncargoDraftResult.ValidationFailure(
                message = "La fecha promesa debe usar formato AAAA-MM-DDTHH:MM.",
            )

        val customerFields = buildTicketCustomerFields(customerDraft)
        val paymentStatus = when (state.paymentChoice) {
            EncargoPaymentChoice.PENDING -> "pending"
            EncargoPaymentChoice.PAID -> "paid"
        }
        val paymentMethod = if (state.paymentChoice == EncargoPaymentChoice.PAID) {
            state.paymentMethod.toWireValue()
        } else {
            null
        }
        val paidAmount = if (state.paymentChoice == EncargoPaymentChoice.PAID) {
            state.pricing.total
        } else {
            0.0
        }
        val paidAt = if (state.paymentChoice == EncargoPaymentChoice.PAID) {
            now.toString()
        } else {
            null
        }

        return BuildEncargoDraftResult.Success(
            draft = TicketDraft(
                customerName = customerFields.customerName,
                customerPhone = customerFields.customerPhone,
                customerEmail = customerFields.customerEmail,
                customerId = customerFields.customerId,
                serviceType = "wash-fold",
                service = service.name,
                weight = parsedWeight,
                status = "received",
                notes = buildEncargoNotes(
                    pickupTargetHours = state.pickupTargetHours,
                    useSharedMachinePool = state.useSharedMachinePool,
                    selectedSpecialItemIds = state.selectedSpecialItemIds,
                ),
                totalAmount = state.pricing.total,
                subtotal = state.pricing.subtotal,
                addOnsTotal = state.pricing.addOnsTotal,
                addOns = state.selectedAddOnIds(),
                promisedPickupDate = promisedPickupDate,
                specialInstructions = buildEncargoSpecialInstructions(
                    selectedSpecialItemIds = state.selectedSpecialItemIds,
                    specialItemNotes = state.specialItemNotes,
                    useSharedMachinePool = state.useSharedMachinePool,
                    specialItemOptions = state.specialItemOptions,
                ),
                photos = emptyList(),
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                paidAmount = paidAmount,
                paidAt = paidAt,
                prepaidAmount = null,
            ),
        )
    }
}

private fun PaymentMethod.toWireValue(): String {
    return when (this) {
        PaymentMethod.CASH -> "cash"
        PaymentMethod.CARD -> "card"
        PaymentMethod.TRANSFER -> "transfer"
    }
}
