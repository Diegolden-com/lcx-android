package com.cleanx.lcx.feature.tickets.domain.create

import com.cleanx.lcx.core.model.Ticket
import com.cleanx.lcx.core.network.TicketDraft
import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.ErrorMessages
import com.cleanx.lcx.feature.tickets.data.TicketRepository
import javax.inject.Inject

sealed interface CreateEncargoResult {
    data class Success(val ticket: Ticket) : CreateEncargoResult
    data class Failure(val message: String) : CreateEncargoResult
}

class CreateEncargoUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
) {

    suspend operator fun invoke(draft: TicketDraft): CreateEncargoResult {
        return when (
            val result = ticketRepository.createTickets(
                source = "encargo",
                tickets = listOf(draft),
            )
        ) {
            is ApiResult.Success -> {
                val ticket = result.data.firstOrNull()
                if (ticket == null) {
                    CreateEncargoResult.Failure("El servidor no devolvio el ticket creado.")
                } else {
                    CreateEncargoResult.Success(ticket)
                }
            }

            is ApiResult.Error -> CreateEncargoResult.Failure(
                message = ErrorMessages.forCode(result.code, result.message),
            )
        }
    }
}
