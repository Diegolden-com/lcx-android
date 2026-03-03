package com.cleanx.lcx.feature.cash.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashValidationTest {

    @Test
    fun `expense without notes returns error`() {
        val error = CashValidation.validateSubmission(
            type = MovementType.EXPENSE,
            amount = 100.0,
            notes = "",
            canClose = true,
            canCloseReason = null,
            totalSalesForDay = null,
        )
        assertEquals(CashValidation.ERROR_EXPENSE_NOTES_REQUIRED, error)
    }

    @Test
    fun `expense with notes returns null`() {
        val error = CashValidation.validateSubmission(
            type = MovementType.EXPENSE,
            amount = 100.0,
            notes = "Compra de insumos",
            canClose = true,
            canCloseReason = null,
            totalSalesForDay = null,
        )
        assertNull(error)
    }

    @Test
    fun `closing blocked returns canCloseReason`() {
        val error = CashValidation.validateSubmission(
            type = MovementType.CLOSING,
            amount = 1000.0,
            notes = "",
            canClose = false,
            canCloseReason = "La caja ya fue cerrada hoy",
            totalSalesForDay = 100.0,
        )
        assertEquals("La caja ya fue cerrada hoy", error)
    }

    @Test
    fun `closing with negative sales returns error`() {
        val error = CashValidation.validateSubmission(
            type = MovementType.CLOSING,
            amount = 1000.0,
            notes = "",
            canClose = true,
            canCloseReason = null,
            totalSalesForDay = -1.0,
        )
        assertEquals(CashValidation.ERROR_NEGATIVE_SALES, error)
    }

    @Test
    fun `any type with non-positive amount returns error`() {
        for (type in MovementType.values()) {
            val error = CashValidation.validateSubmission(
                type = type,
                amount = 0.0,
                notes = "ok",
                canClose = true,
                canCloseReason = null,
                totalSalesForDay = 0.0,
            )
            assertEquals(CashValidation.ERROR_AMOUNT_REQUIRED, error)
        }
    }

    @Test
    fun `calculateExpectedClosing uses opening plus sales plus income minus expenses`() {
        val expected = CashValidation.calculateExpectedClosing(
            openingAmount = 500.0,
            totalSalesForDay = 1500.0,
            totalIncome = 200.0,
            totalExpenses = 350.0,
        )
        assertEquals(1850.0, expected, 0.001)
    }

    @Test
    fun `calculateDiscrepancy returns null when sales missing`() {
        val result = CashValidation.calculateDiscrepancy(
            actualAmount = 5000.0,
            openingAmount = 2000.0,
            totalSalesForDay = null,
            totalIncome = 500.0,
            totalExpenses = 100.0,
        )
        assertNull(result)
    }

    @Test
    fun `calculateDiscrepancy returns balanced for exact match`() {
        val result = CashValidation.calculateDiscrepancy(
            actualAmount = 5300.0,
            openingAmount = 2000.0,
            totalSalesForDay = 3000.0,
            totalIncome = 500.0,
            totalExpenses = 200.0,
        )
        assertNotNull(result)
        assertEquals(5300.0, result!!.first, 0.001)
        assertEquals(0.0, result.second, 0.001)
        assertEquals("balanced", result.third)
    }

    @Test
    fun `calculateDiscrepancy returns overage when actual exceeds expected`() {
        val result = CashValidation.calculateDiscrepancy(
            actualAmount = 3500.0,
            openingAmount = 1000.0,
            totalSalesForDay = 2000.0,
            totalIncome = 0.0,
            totalExpenses = 0.0,
        )
        assertNotNull(result)
        assertEquals(3000.0, result!!.first, 0.001)
        assertEquals(500.0, result.second, 0.001)
        assertEquals("overage", result.third)
    }

    @Test
    fun `calculateDiscrepancy returns shortage when actual is lower`() {
        val result = CashValidation.calculateDiscrepancy(
            actualAmount = 2800.0,
            openingAmount = 1000.0,
            totalSalesForDay = 2000.0,
            totalIncome = 300.0,
            totalExpenses = 100.0,
        )
        assertNotNull(result)
        assertEquals(3200.0, result!!.first, 0.001)
        assertEquals(-400.0, result.second, 0.001)
        assertEquals("shortage", result.third)
    }

    @Test
    fun `discrepancyTypeForMetadata maps values to labels`() {
        assertEquals("sobrante", CashValidation.discrepancyTypeForMetadata(1.0))
        assertEquals("faltante", CashValidation.discrepancyTypeForMetadata(-1.0))
        assertEquals("exacto", CashValidation.discrepancyTypeForMetadata(0.0))
    }

    @Test
    fun `canCloseRegister true with opening and no closure`() {
        val (canClose, reason) = CashValidation.canCloseRegister(
            hasOpening = true,
            hasClosure = false,
        )
        assertTrue(canClose)
        assertNull(reason)
    }

    @Test
    fun `canCloseRegister false when already closed`() {
        val (canClose, reason) = CashValidation.canCloseRegister(
            hasOpening = true,
            hasClosure = true,
        )
        assertTrue(!canClose)
        assertEquals(CashValidation.REASON_ALREADY_CLOSED, reason)
    }

    @Test
    fun `canCloseRegister false when no opening`() {
        val (canClose, reason) = CashValidation.canCloseRegister(
            hasOpening = false,
            hasClosure = false,
        )
        assertTrue(!canClose)
        assertEquals(CashValidation.REASON_NO_OPENING, reason)
    }
}
