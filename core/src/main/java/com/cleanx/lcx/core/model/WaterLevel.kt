package com.cleanx.lcx.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Status of the water tank.
 * Maps to the `status` column in the `water_levels` table.
 */
@Serializable
enum class WaterLevelStatus {
    @SerialName("critical") CRITICAL,
    @SerialName("low") LOW,
    @SerialName("normal") NORMAL,
    @SerialName("optimal") OPTIMAL,
}

/**
 * Represents a row in the Supabase `water_levels` table.
 *
 * Column mapping mirrors the PWA's direct-table access so that both
 * clients share the same contract.
 */
@Serializable
data class WaterLevel(
    val id: String? = null,
    @SerialName("level_percentage") val levelPercentage: Int,
    val liters: Int? = null,
    @SerialName("tank_capacity") val tankCapacity: Int? = null,
    val status: WaterLevelStatus? = null,
    val action: String? = null,
    val notes: String? = null,
    @SerialName("provider_id") val providerId: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    val branch: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
