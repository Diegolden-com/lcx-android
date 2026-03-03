package com.cleanx.lcx.core.navigation

import com.cleanx.lcx.core.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAccessTest {

    // ── Employee: all operator screens ─────────────────────────────────

    @Test
    fun `employee can access all standard operator screens`() {
        val operatorScreens = listOf(
            Screen.Dashboard,
            Screen.TicketList,
            Screen.CreateTicket,
            Screen.TicketDetail(ticketId = "t1"),
            Screen.TicketPreset(preset = "active"),
            Screen.Water,
            Screen.Checklist,
            Screen.Cash,
            Screen.CashRegister,
            Screen.CashHistory,
            Screen.Sales,
            Screen.IncidentsNew,
            Screen.IncidentsHistory,
            Screen.ShiftsControl,
            Screen.ShiftsHistory,
            Screen.ShiftsSchedule,
            Screen.DamagedClothingNew,
            Screen.DamagedClothingHistory,
            Screen.SuppliesInventory,
            Screen.SuppliesLabels,
            Screen.SuppliesBrotherDebug,
            Screen.Vacations,
            Screen.CalendarMonthly,
            Screen.CalendarEvents,
            Screen.BestPractices,
            Screen.Help,
            Screen.Charge(ticketId = "t1"),
            Screen.Print(ticketId = "t1"),
            Screen.Transaction,
            Screen.PaymentDiagnostics,
        )

        operatorScreens.forEach { screen ->
            assertTrue(
                "Employee should access ${screen::class.simpleName}",
                RouteAccess.canAccess(UserRole.EMPLOYEE, screen),
            )
        }
    }

    // ── Employee: restricted screens ───────────────────────────────────

    @Test
    fun `employee cannot access manager-only screens`() {
        val restricted = listOf(
            Screen.ShiftsReports,
            Screen.SuppliesReports,
        )

        restricted.forEach { screen ->
            assertFalse(
                "Employee should NOT access ${screen::class.simpleName}",
                RouteAccess.canAccess(UserRole.EMPLOYEE, screen),
            )
        }
    }

    // ── Manager: all operator + manager screens ────────────────────────

    @Test
    fun `manager can access all operator screens`() {
        assertTrue(RouteAccess.canAccess(UserRole.MANAGER, Screen.Dashboard))
        assertTrue(RouteAccess.canAccess(UserRole.MANAGER, Screen.Sales))
        assertTrue(RouteAccess.canAccess(UserRole.MANAGER, Screen.Help))
    }

    @Test
    fun `manager can access manager-restricted screens`() {
        assertTrue(RouteAccess.canAccess(UserRole.MANAGER, Screen.ShiftsReports))
        assertTrue(RouteAccess.canAccess(UserRole.MANAGER, Screen.SuppliesReports))
    }

    // ── Superadmin: everything ─────────────────────────────────────────

    @Test
    fun `superadmin can access all screens`() {
        val allScreens = listOf(
            Screen.Dashboard,
            Screen.TicketList,
            Screen.CreateTicket,
            Screen.TicketDetail(ticketId = "t1"),
            Screen.TicketPreset(preset = "active"),
            Screen.Water,
            Screen.Checklist,
            Screen.Cash,
            Screen.Sales,
            Screen.ShiftsReports,
            Screen.SuppliesReports,
            Screen.Help,
        )

        allScreens.forEach { screen ->
            assertTrue(
                "Superadmin should access ${screen::class.simpleName}",
                RouteAccess.canAccess(UserRole.SUPERADMIN, screen),
            )
        }
    }

    // ── Null role: no access ───────────────────────────────────────────

    @Test
    fun `null role cannot access any screen`() {
        assertFalse(RouteAccess.canAccess(null, Screen.Dashboard))
        assertFalse(RouteAccess.canAccess(null, Screen.Sales))
        assertFalse(RouteAccess.canAccess(null, Screen.ShiftsReports))
    }

    // ── Tab access ─────────────────────────────────────────────────────

    @Test
    fun `employee can access all bottom nav tabs`() {
        val tabs = listOf(
            Screen.DashboardGraph,
            Screen.TicketsGraph,
            Screen.WaterGraph,
            Screen.ChecklistGraph,
            Screen.CashGraph,
            Screen.MoreGraph,
        )

        tabs.forEach { tab ->
            assertTrue(
                "Employee should see tab ${tab::class.simpleName}",
                RouteAccess.canAccessTab(UserRole.EMPLOYEE, tab),
            )
        }
    }

    @Test
    fun `null role cannot access any tab`() {
        assertFalse(RouteAccess.canAccessTab(null, Screen.DashboardGraph))
        assertFalse(RouteAccess.canAccessTab(null, Screen.MoreGraph))
    }

    // ── allowedRoles ───────────────────────────────────────────────────

    @Test
    fun `allowedRoles returns correct sets`() {
        val dashboardRoles = RouteAccess.allowedRoles(Screen.Dashboard)
        assertTrue(UserRole.EMPLOYEE in dashboardRoles)
        assertTrue(UserRole.MANAGER in dashboardRoles)
        assertTrue(UserRole.SUPERADMIN in dashboardRoles)

        val reportsRoles = RouteAccess.allowedRoles(Screen.ShiftsReports)
        assertFalse(UserRole.EMPLOYEE in reportsRoles)
        assertTrue(UserRole.MANAGER in reportsRoles)
        assertTrue(UserRole.SUPERADMIN in reportsRoles)
    }
}
