package com.cleanx.lcx.ui.dashboard

import com.cleanx.lcx.core.network.SupabaseTableClient
import com.cleanx.lcx.core.session.SessionProfileRepository
import com.cleanx.lcx.feature.checklist.data.ChecklistRepository
import com.cleanx.lcx.feature.checklist.data.ChecklistType
import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.InventoryCatalogRecord
import com.cleanx.lcx.feature.tickets.data.TicketRepository
import com.cleanx.lcx.feature.water.data.WaterRepository
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val sessionProfileRepository: SessionProfileRepository,
    private val checklistRepository: ChecklistRepository,
    private val waterRepository: WaterRepository,
    private val ticketRepository: TicketRepository,
    private val supabase: SupabaseTableClient,
) {

    suspend fun loadSnapshot(now: Instant = Instant.now()): DashboardSnapshot = coroutineScope {
        val profile = sessionProfileRepository.getCurrentProfile()

        val routineDeferred = async { loadRoutine(branch = profile.branch) }
        val ticketsDeferred = async { loadPendingTickets(now = now) }
        val suppliesDeferred = async { loadSupplyNeeds(branch = profile.branch) }

        DashboardSnapshot(
            operatorName = profile.fullName?.trim().takeUnless { it.isNullOrEmpty() } ?: "Operador",
            branchName = profile.branch?.trim()?.takeIf { it.isNotEmpty() },
            routine = routineDeferred.await(),
            pendingTickets = ticketsDeferred.await(),
            supplyNeeds = suppliesDeferred.await(),
        )
    }

    private suspend fun loadRoutine(branch: String?): DashboardRoutineSection {
        val latestLevel = waterRepository.getCurrentWaterLevel(branch).getOrNull()
        val waterRecordedToday = checklistRepository.hasWaterLevelToday(branch)
        val openingRegisteredToday = checklistRepository.hasCashMovementToday("opening")
        val closingRegisteredToday = checklistRepository.hasCashMovementToday("closing")
        val entryChecklist = checklistRepository.getTodayChecklistSnapshot(ChecklistType.ENTRADA).getOrNull()
        val exitChecklist = checklistRepository.getTodayChecklistSnapshot(ChecklistType.SALIDA).getOrNull()

        val entryItems = listOf(
            DashboardRoutineItem(
                title = "Checklist de entrada",
                state = checklistRoutineState(entryChecklist),
                detail = checklistRoutineDetail(entryChecklist),
            ),
            DashboardRoutineItem(
                title = "Nivel de agua",
                state = if (waterRecordedToday) {
                    DashboardRoutineState.DONE
                } else {
                    DashboardRoutineState.PENDING
                },
                detail = waterRoutineDetail(
                    latestLevel = latestLevel,
                    recordedToday = waterRecordedToday,
                ),
            ),
            DashboardRoutineItem(
                title = "Apertura de caja",
                state = if (openingRegisteredToday) {
                    DashboardRoutineState.DONE
                } else {
                    DashboardRoutineState.PENDING
                },
                detail = cashOpeningRoutineDetail(openingRegisteredToday),
            ),
        )

        val exitItems = listOf(
            DashboardRoutineItem(
                title = "Checklist de salida",
                state = checklistRoutineState(exitChecklist),
                detail = checklistRoutineDetail(exitChecklist),
            ),
            DashboardRoutineItem(
                title = "Corte de caja",
                state = cashClosingRoutineState(
                    openingRegistered = openingRegisteredToday,
                    closingRegistered = closingRegisteredToday,
                ),
                detail = cashClosingRoutineDetail(
                    openingRegistered = openingRegisteredToday,
                    closingRegistered = closingRegisteredToday,
                ),
            ),
        )

        return DashboardRoutineSection(
            entryGroup = DashboardRoutineGroup(
                title = "Inicio de turno",
                completedCount = entryItems.count { it.state.isDone() },
                totalCount = entryItems.size,
                items = entryItems,
            ),
            exitGroup = DashboardRoutineGroup(
                title = "Cierre de turno",
                completedCount = exitItems.count { it.state.isDone() },
                totalCount = exitItems.size,
                items = exitItems,
            ),
        )
    }

    private suspend fun loadPendingTickets(now: Instant): DashboardPendingTicketsSection {
        return when (val result = ticketRepository.getTickets(limit = 200)) {
            is ApiResult.Success -> {
                val pendingTickets = result.data
                    .filter { isOperationalPendingTicket(it.status) }
                    .sortedByDescending { it.createdAt.orEmpty() }

                DashboardPendingTicketsSection(
                    totalCount = pendingTickets.size,
                    items = pendingTickets
                        .take(3)
                        .map { it.toDashboardTicketItem(now = now) },
                )
            }

            is ApiResult.Error -> DashboardPendingTicketsSection(
                error = result.message,
            )
        }
    }

    private suspend fun loadSupplyNeeds(branch: String?): DashboardSupplyNeedsSection {
        val inventory = supabase.selectWithRequest<InventoryCatalogRecord>("inventory") {
            order("quantity", Order.ASCENDING)
            limit(200)
        }.getOrElse { error ->
            return DashboardSupplyNeedsSection(
                error = error.message ?: "No se pudo cargar el inventario.",
            )
        }

        val scopedInventory = if (branch == null) {
            inventory
        } else {
            inventory.filter { item ->
                item.branch.isNullOrBlank() || item.branch.equals(branch, ignoreCase = true)
            }
        }

        val needs = scopedInventory
            .mapNotNull { it.toDashboardSupplyNeed() }
            .sortedWith(
                compareBy<DashboardSupplyNeed>(
                    { it.severity != DashboardSupplySeverity.CRITICAL },
                    { it.quantity },
                    { it.itemName.lowercase() },
                ),
            )

        return DashboardSupplyNeedsSection(
            totalCount = needs.size,
            items = needs.take(3),
        )
    }
}
