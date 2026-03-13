package com.cleanx.lcx.ui.dashboard

import com.cleanx.lcx.core.model.Ticket
import com.cleanx.lcx.core.model.TicketStatus
import com.cleanx.lcx.feature.checklist.data.Checklist
import com.cleanx.lcx.feature.checklist.data.ChecklistStatus
import com.cleanx.lcx.feature.tickets.data.InventoryCatalogRecord
import com.cleanx.lcx.feature.water.data.WaterLevelWithUser
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

data class DashboardSnapshot(
    val operatorName: String,
    val branchName: String?,
    val routine: DashboardRoutineSection,
    val pendingTickets: DashboardPendingTicketsSection,
    val supplyNeeds: DashboardSupplyNeedsSection,
)

data class DashboardRoutineSection(
    val entryGroup: DashboardRoutineGroup,
    val exitGroup: DashboardRoutineGroup,
)

data class DashboardRoutineGroup(
    val title: String,
    val completedCount: Int,
    val totalCount: Int,
    val items: List<DashboardRoutineItem>,
)

data class DashboardRoutineItem(
    val title: String,
    val state: DashboardRoutineState,
    val detail: String,
)

enum class DashboardRoutineState {
    DONE,
    IN_PROGRESS,
    PENDING,
}

data class DashboardPendingTicketsSection(
    val totalCount: Int = 0,
    val items: List<DashboardTicketItem> = emptyList(),
    val error: String? = null,
)

data class DashboardTicketItem(
    val id: String,
    val customerName: String,
    val status: TicketStatus,
    val relativeAgeLabel: String,
    val priority: DashboardTicketPriority,
)

enum class DashboardTicketPriority {
    NORMAL,
    HIGH,
}

data class DashboardSupplyNeedsSection(
    val totalCount: Int = 0,
    val items: List<DashboardSupplyNeed> = emptyList(),
    val error: String? = null,
)

data class DashboardSupplyNeed(
    val id: String,
    val itemName: String,
    val quantity: Int,
    val minQuantity: Int,
    val severity: DashboardSupplySeverity,
)

enum class DashboardSupplySeverity {
    CRITICAL,
    LOW,
}

fun isOperationalPendingTicket(status: TicketStatus): Boolean = status != TicketStatus.DELIVERED

fun DashboardRoutineState.isDone(): Boolean = this == DashboardRoutineState.DONE

fun checklistRoutineState(checklist: Checklist?): DashboardRoutineState {
    return when (checklist?.status) {
        ChecklistStatus.COMPLETED -> DashboardRoutineState.DONE
        ChecklistStatus.IN_PROGRESS -> DashboardRoutineState.IN_PROGRESS
        ChecklistStatus.PENDING,
        null,
        -> DashboardRoutineState.PENDING
    }
}

fun checklistRoutineDetail(checklist: Checklist?): String {
    return when (checklist?.status) {
        ChecklistStatus.COMPLETED -> "Completado hoy"
        ChecklistStatus.IN_PROGRESS -> "En progreso"
        ChecklistStatus.PENDING -> "Pendiente por completar"
        null -> "Aun no iniciado hoy"
    }
}

fun waterRoutineDetail(
    latestLevel: WaterLevelWithUser?,
    recordedToday: Boolean,
): String {
    if (!recordedToday) {
        return "Sin registro de agua hoy"
    }

    val percentage = latestLevel?.levelPercentage
    val statusLabel = latestLevel?.status?.name?.lowercase()
    return if (percentage != null && statusLabel != null) {
        "$percentage% - $statusLabel"
    } else {
        "Nivel validado hoy"
    }
}

fun cashOpeningRoutineDetail(openingRegistered: Boolean): String {
    return if (openingRegistered) {
        "Apertura registrada hoy"
    } else {
        "Pendiente de registrar apertura"
    }
}

fun cashClosingRoutineState(
    openingRegistered: Boolean,
    closingRegistered: Boolean,
): DashboardRoutineState {
    return when {
        closingRegistered -> DashboardRoutineState.DONE
        openingRegistered -> DashboardRoutineState.IN_PROGRESS
        else -> DashboardRoutineState.PENDING
    }
}

fun cashClosingRoutineDetail(
    openingRegistered: Boolean,
    closingRegistered: Boolean,
): String {
    return when {
        closingRegistered -> "Corte de caja registrado hoy"
        openingRegistered -> "Apertura registrada, falta corte"
        else -> "Aun no hay apertura de caja hoy"
    }
}

fun ticketPriorityFor(
    createdAt: String?,
    now: Instant = Instant.now(),
): DashboardTicketPriority {
    val created = createdAt.toInstantOrNull() ?: return DashboardTicketPriority.NORMAL
    val waitedMinutes = Duration.between(created, now).toMinutes()
    return if (waitedMinutes >= 30) {
        DashboardTicketPriority.HIGH
    } else {
        DashboardTicketPriority.NORMAL
    }
}

fun formatRelativeTicketAge(
    createdAt: String?,
    now: Instant = Instant.now(),
): String {
    val created = createdAt.toInstantOrNull() ?: return "Fecha desconocida"
    val elapsed = Duration.between(created, now)
    val minutes = elapsed.toMinutes()
    return when {
        minutes < 1 -> "Hace menos de 1 min"
        minutes < 60 -> "Hace $minutes min"
        minutes < 1_440 -> "Hace ${elapsed.toHours()} h"
        else -> "Hace ${elapsed.toDays()} d"
    }
}

fun supplySeverityFor(
    quantity: Int,
    minQuantity: Int,
): DashboardSupplySeverity? {
    if (quantity > minQuantity) return null
    if (quantity <= 0) return DashboardSupplySeverity.CRITICAL

    if (minQuantity <= 0) {
        return DashboardSupplySeverity.LOW
    }

    val ratio = quantity.toDouble() / minQuantity.toDouble()
    return if (ratio <= 0.25) {
        DashboardSupplySeverity.CRITICAL
    } else {
        DashboardSupplySeverity.LOW
    }
}

fun Ticket.toDashboardTicketItem(now: Instant = Instant.now()): DashboardTicketItem {
    return DashboardTicketItem(
        id = id,
        customerName = customerName,
        status = status,
        relativeAgeLabel = formatRelativeTicketAge(createdAt, now),
        priority = ticketPriorityFor(createdAt, now),
    )
}

fun InventoryCatalogRecord.toDashboardSupplyNeed(): DashboardSupplyNeed? {
    val severity = supplySeverityFor(quantity, minQuantity) ?: return null
    return DashboardSupplyNeed(
        id = id,
        itemName = itemName,
        quantity = quantity,
        minQuantity = minQuantity,
        severity = severity,
    )
}

private fun String?.toInstantOrNull(): Instant? {
    if (this.isNullOrBlank()) return null
    return try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        runCatching { OffsetDateTime.parse(this).toInstant() }.getOrNull()
    }
}
