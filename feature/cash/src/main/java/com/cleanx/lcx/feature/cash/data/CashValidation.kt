package com.cleanx.lcx.feature.cash.data

/**
 * Shared validation and calculation rules for cash register flows.
 *
 * Centralizing these rules avoids drift between ViewModel behavior and unit tests.
 */
object CashValidation {

    const val ERROR_AMOUNT_REQUIRED = "El monto debe ser mayor a cero"
    const val ERROR_EXPENSE_NOTES_REQUIRED = "Por favor describe el gasto realizado"
    const val ERROR_CANNOT_CLOSE = "No se puede cerrar la caja en este momento"
    const val ERROR_NEGATIVE_SALES = "Las ventas del dia no pueden ser negativas"
    const val REASON_ALREADY_CLOSED = "La caja ya fue cerrada hoy"
    const val REASON_NO_OPENING = "No hay apertura de caja para hoy"

    fun validateSubmission(
        type: MovementType,
        amount: Double,
        notes: String,
        canClose: Boolean,
        canCloseReason: String?,
        totalSalesForDay: Double?,
    ): String? {
        if (amount <= 0) return ERROR_AMOUNT_REQUIRED
        if (type == MovementType.EXPENSE && notes.isBlank()) return ERROR_EXPENSE_NOTES_REQUIRED
        if (type == MovementType.CLOSING && !canClose) return canCloseReason ?: ERROR_CANNOT_CLOSE
        if (type == MovementType.CLOSING && totalSalesForDay != null && totalSalesForDay < 0) {
            return ERROR_NEGATIVE_SALES
        }
        return null
    }

    fun calculateExpectedClosing(
        openingAmount: Double,
        totalSalesForDay: Double,
        totalIncome: Double,
        totalExpenses: Double,
    ): Double = openingAmount + totalSalesForDay + totalIncome - totalExpenses

    fun calculateDiscrepancy(
        actualAmount: Double,
        openingAmount: Double,
        totalSalesForDay: Double?,
        totalIncome: Double,
        totalExpenses: Double,
    ): Triple<Double, Double, String>? {
        val sales = totalSalesForDay ?: return null
        val expected = calculateExpectedClosing(
            openingAmount = openingAmount,
            totalSalesForDay = sales,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
        )
        val diff = actualAmount - expected
        val type = when {
            diff > 0 -> "overage"
            diff < 0 -> "shortage"
            else -> "balanced"
        }
        return Triple(expected, diff, type)
    }

    fun discrepancyTypeForMetadata(discrepancy: Double): String = when {
        discrepancy > 0 -> "sobrante"
        discrepancy < 0 -> "faltante"
        else -> "exacto"
    }

    fun canCloseRegister(hasOpening: Boolean, hasClosure: Boolean): Pair<Boolean, String?> = when {
        hasClosure -> false to REASON_ALREADY_CLOSED
        !hasOpening -> false to REASON_NO_OPENING
        else -> true to null
    }
}
