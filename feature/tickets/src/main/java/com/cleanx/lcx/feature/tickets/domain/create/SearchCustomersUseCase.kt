package com.cleanx.lcx.feature.tickets.domain.create

import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.CustomerRecord
import com.cleanx.lcx.feature.tickets.data.ErrorMessages
import com.cleanx.lcx.feature.tickets.data.TicketCreationRepository
import javax.inject.Inject

sealed interface SearchCustomersResult {
    data class Success(
        val query: String,
        val customers: List<CustomerRecord>,
    ) : SearchCustomersResult

    data class Failure(
        val query: String,
        val message: String,
    ) : SearchCustomersResult
}

class SearchCustomersUseCase @Inject constructor(
    private val repository: TicketCreationRepository,
) {

    suspend operator fun invoke(query: String): SearchCustomersResult {
        return when (val result = repository.searchCustomers(query)) {
            is ApiResult.Success -> SearchCustomersResult.Success(
                query = query,
                customers = result.data,
            )

            is ApiResult.Error -> SearchCustomersResult.Failure(
                query = query,
                message = ErrorMessages.forCode(result.code, result.message),
            )
        }
    }
}
