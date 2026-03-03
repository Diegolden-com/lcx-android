package com.cleanx.lcx.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The type of checklist (opening or closing shift).
 * Maps to the `type` column in the `checklists` table.
 */
@Serializable
enum class ChecklistType {
    @SerialName("entrada") ENTRADA,
    @SerialName("salida") SALIDA,
}

/**
 * Workflow status of a checklist.
 * Maps to the `status` column in the `checklists` table.
 */
@Serializable
enum class ChecklistStatus {
    @SerialName("pending") PENDING,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
}

/**
 * Represents a row in the Supabase `checklists` table.
 *
 * Column mapping mirrors the PWA's direct-table access so that both
 * clients share the same contract.
 */
@Serializable
data class Checklist(
    val id: String? = null,
    val type: ChecklistType,
    val status: ChecklistStatus = ChecklistStatus.PENDING,
    val date: String,
    val notes: String? = null,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val branch: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
