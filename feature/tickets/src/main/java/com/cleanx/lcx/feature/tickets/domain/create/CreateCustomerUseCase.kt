package com.cleanx.lcx.feature.tickets.domain.create

import com.cleanx.lcx.feature.tickets.data.CustomerCreateInput
import com.cleanx.lcx.feature.tickets.data.CustomerRecord
import com.cleanx.lcx.feature.tickets.data.CustomerValidationErrors
import com.cleanx.lcx.feature.tickets.data.TicketCreationRepository
import javax.inject.Inject

sealed interface SaveCustomerResult {
    data class Success(val customer: CustomerRecord) : SaveCustomerResult
    data class RequiresDuplicateConfirmation(
        val input: CustomerCreateInput,
        val matches: List<CustomerRecord>,
        val validationErrors: CustomerValidationErrors,
    ) : SaveCustomerResult

    data class Failure(
        val message: String,
        val validationErrors: CustomerValidationErrors,
    ) : SaveCustomerResult
}

class CreateCustomerUseCase @Inject constructor(
    private val repository: TicketCreationRepository,
) {

    suspend operator fun invoke(
        input: CustomerCreateInput,
        allowDuplicatePhone: Boolean,
    ): SaveCustomerResult {
        val result = repository.createCustomer(
            input = input,
            allowDuplicatePhone = allowDuplicatePhone,
        )

        return when {
            result.customer != null -> SaveCustomerResult.Success(result.customer)
            result.requiresDuplicateConfirmation -> SaveCustomerResult.RequiresDuplicateConfirmation(
                input = input,
                matches = result.duplicatePhoneMatches,
                validationErrors = result.validationErrors,
            )

            else -> SaveCustomerResult.Failure(
                message = result.errorMessage ?: "No se pudo crear el cliente.",
                validationErrors = result.validationErrors,
            )
        }
    }
}
