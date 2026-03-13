package com.cleanx.lcx.ui.dashboard

import com.cleanx.lcx.feature.checklist.data.Checklist
import com.cleanx.lcx.feature.checklist.data.ChecklistStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DashboardModelsTest {

    @Test
    fun `ticket priority turns high after thirty minutes`() {
        val now = Instant.parse("2026-03-13T22:00:00Z")

        assertEquals(
            DashboardTicketPriority.NORMAL,
            ticketPriorityFor("2026-03-13T21:45:30Z", now),
        )
        assertEquals(
            DashboardTicketPriority.HIGH,
            ticketPriorityFor("2026-03-13T21:29:59Z", now),
        )
    }

    @Test
    fun `supply severity only flags low stock rows`() {
        assertNull(supplySeverityFor(quantity = 8, minQuantity = 5))
        assertEquals(
            DashboardSupplySeverity.LOW,
            supplySeverityFor(quantity = 2, minQuantity = 5),
        )
        assertEquals(
            DashboardSupplySeverity.CRITICAL,
            supplySeverityFor(quantity = 0, minQuantity = 5),
        )
    }

    @Test
    fun `checklist status maps to routine state and detail`() {
        val completed = Checklist(
            id = "chk-1",
            status = ChecklistStatus.COMPLETED,
            date = "2026-03-13",
        )
        val inProgress = Checklist(
            id = "chk-2",
            status = ChecklistStatus.IN_PROGRESS,
            date = "2026-03-13",
        )

        assertEquals(DashboardRoutineState.DONE, checklistRoutineState(completed))
        assertEquals("Completado hoy", checklistRoutineDetail(completed))
        assertEquals(DashboardRoutineState.IN_PROGRESS, checklistRoutineState(inProgress))
        assertEquals("En progreso", checklistRoutineDetail(inProgress))
        assertEquals(DashboardRoutineState.PENDING, checklistRoutineState(null))
        assertEquals("Aun no iniciado hoy", checklistRoutineDetail(null))
    }
}
