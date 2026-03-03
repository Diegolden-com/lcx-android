package com.cleanx.lcx.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a row in the Supabase `checklist_items` table.
 *
 * The `notes` column stores JSON metadata (templateId, category, required)
 * in the database; it is kept as a nullable String here and can be parsed
 * by consumers as needed.
 *
 * Column mapping mirrors the PWA's direct-table access so that both
 * clients share the same contract.
 */
@Serializable
data class ChecklistItem(
    val id: String? = null,
    @SerialName("checklist_id") val checklistId: String,
    @SerialName("item_description") val itemDescription: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val notes: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)
