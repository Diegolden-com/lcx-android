package com.cleanx.lcx.feature.cash.ui

import com.cleanx.lcx.feature.cash.data.CashSummary
import com.cleanx.lcx.feature.cash.data.MovementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CashUiStateTest {

    @Test
    fun `billsTotal calculates correctly`() {
        val state = CashUiState(
            billCounts = mapOf(
                1000.0 to 2,
                500.0 to 3,
                200.0 to 0,
                100.0 to 5,
                50.0 to 1,
                20.0 to 0,
            ),
        )
        assertEquals(4050.0, state.billsTotal, 0.001)
    }

    @Test
    fun `coinsTotal calculates correctly`() {
        val state = CashUiState(
            coinCounts = mapOf(
                20.0 to 1,
                10.0 to 3,
                5.0 to 2,
                2.0 to 4,
                1.0 to 5,
                0.5 to 6,
            ),
        )
        assertEquals(76.0, state.coinsTotal, 0.001)
    }

    @Test
    fun `grandTotal uses expenseAmount for expense type`() {
        val state = CashUiState(
            selectedType = MovementType.EXPENSE,
            expenseAmount = "350.50",
            billCounts = mapOf(
                1000.0 to 5,
                500.0 to 0,
                200.0 to 0,
                100.0 to 0,
                50.0 to 0,
                20.0 to 0,
            ),
        )
        assertEquals(350.50, state.grandTotal, 0.001)
    }

    @Test
    fun `grandTotal uses denomination totals for non-expense types`() {
        val state = CashUiState(
            selectedType = MovementType.OPENING,
            expenseAmount = "9999.99",
            billCounts = mapOf(
                1000.0 to 1,
                500.0 to 0,
                200.0 to 0,
                100.0 to 0,
                50.0 to 0,
                20.0 to 0,
            ),
            coinCounts = mapOf(
                20.0 to 0,
                10.0 to 0,
                5.0 to 0,
                2.0 to 0,
                1.0 to 0,
                0.5 to 2,
            ),
        )
        assertEquals(1001.0, state.grandTotal, 0.001)
    }

    @Test
    fun `parsedTotalSalesForDay parses valid number and trims spaces`() {
        val state = CashUiState(totalSalesForDay = " 4500.75 ")
        assertNotNull(state.parsedTotalSalesForDay)
        assertEquals(4500.75, state.parsedTotalSalesForDay!!, 0.001)
    }

    @Test
    fun `parsedTotalSalesForDay returns null for empty or invalid values`() {
        assertNull(CashUiState(totalSalesForDay = "").parsedTotalSalesForDay)
        assertNull(CashUiState(totalSalesForDay = "abc").parsedTotalSalesForDay)
    }

    @Test
    fun `expectedClosingFromSales computed only for closing type`() {
        val summary = CashSummary(
            openingAmount = 2000.0,
            totalIncome = 500.0,
            totalExpenses = 200.0,
        )

        val closingState = CashUiState(
            selectedType = MovementType.CLOSING,
            summary = summary,
            totalSalesForDay = "3000",
        )
        assertEquals(5300.0, closingState.expectedClosingFromSales!!, 0.001)

        assertNull(closingState.copy(selectedType = MovementType.OPENING).expectedClosingFromSales)
        assertNull(closingState.copy(selectedType = MovementType.INCOME).expectedClosingFromSales)
        assertNull(closingState.copy(selectedType = MovementType.EXPENSE).expectedClosingFromSales)
    }

    @Test
    fun `expectedClosingFromSales returns null when sales are missing`() {
        val state = CashUiState(
            selectedType = MovementType.CLOSING,
            summary = CashSummary(openingAmount = 1000.0),
            totalSalesForDay = "",
        )
        assertNull(state.expectedClosingFromSales)
    }

    @Test
    fun `discrepancyPreview computed correctly for overage`() {
        val state = CashUiState(
            selectedType = MovementType.CLOSING,
            summary = CashSummary(
                openingAmount = 1000.0,
                totalIncome = 200.0,
                totalExpenses = 100.0,
            ),
            totalSalesForDay = "2000",
            billCounts = mapOf(
                1000.0 to 3,
                500.0 to 1,
                200.0 to 0,
                100.0 to 0,
                50.0 to 0,
                20.0 to 0,
            ),
            coinCounts = mapOf(
                20.0 to 0,
                10.0 to 0,
                5.0 to 0,
                2.0 to 0,
                1.0 to 0,
                0.5 to 0,
            ),
        )
        assertEquals(400.0, state.discrepancyPreview!!, 0.001)
    }

    @Test
    fun `discrepancyPreview returns null for non-closing types`() {
        val state = CashUiState(
            selectedType = MovementType.OPENING,
            totalSalesForDay = "1000",
        )
        assertNull(state.discrepancyPreview)
    }

    @Test
    fun `discrepancyPreview balanced when actual equals expected`() {
        val state = CashUiState(
            selectedType = MovementType.CLOSING,
            summary = CashSummary(
                openingAmount = 500.0,
                totalIncome = 100.0,
                totalExpenses = 50.0,
            ),
            totalSalesForDay = "450",
            billCounts = mapOf(
                1000.0 to 1,
                500.0 to 0,
                200.0 to 0,
                100.0 to 0,
                50.0 to 0,
                20.0 to 0,
            ),
            coinCounts = mapOf(
                20.0 to 0,
                10.0 to 0,
                5.0 to 0,
                2.0 to 0,
                1.0 to 0,
                0.5 to 0,
            ),
        )
        assertEquals(0.0, state.discrepancyPreview!!, 0.001)
    }
}
