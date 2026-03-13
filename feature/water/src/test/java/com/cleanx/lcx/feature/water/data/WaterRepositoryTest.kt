package com.cleanx.lcx.feature.water.data

import com.cleanx.lcx.core.model.WaterLevelStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WaterRepositoryTest {

    @Test
    fun `defaultWaterLevel matches pwa initial snapshot`() {
        val level = defaultWaterLevel(branch = "sucursal-centro")

        assertEquals(DEFAULT_WATER_LEVEL_PERCENTAGE, level.levelPercentage)
        assertEquals(7_500, level.liters)
        assertEquals(TANK_CAPACITY_LITERS, level.tankCapacity)
        assertEquals(WaterLevelStatus.OPTIMAL, level.status)
        assertEquals("Nivel inicial", level.action)
        assertEquals("sucursal-centro", level.branch)
    }

    @Test
    fun `buildWaterLevelInsert persists recorded_by branch and pwa action label`() {
        val insert = buildWaterLevelInsert(
            percentage = 25,
            recordedBy = "user-1",
            branch = "sucursal-centro",
        )

        assertEquals(25, insert.levelPercentage)
        assertEquals(2_500, insert.liters)
        assertEquals(WaterLevelStatus.LOW, insert.status)
        assertEquals("Nivel registrado", insert.action)
        assertEquals("user-1", insert.recordedBy)
        assertEquals("sucursal-centro", insert.branch)
    }

    @Test
    fun `buildWaterOrderInsert persists provider and branch context`() {
        val provider = WATER_PROVIDERS.first { it.id == "cristal" }

        val insert = buildWaterOrderInsert(
            provider = provider,
            currentPercentage = 18,
            recordedBy = "user-2",
            branch = "sucursal-norte",
        )

        assertEquals(18, insert.levelPercentage)
        assertEquals(1_800, insert.liters)
        assertEquals(WaterLevelStatus.CRITICAL, insert.status)
        assertEquals("Agua pedida - ${provider.name}", insert.action)
        assertEquals(provider.id, insert.providerId)
        assertEquals(provider.name, insert.providerName)
        assertEquals("user-2", insert.recordedBy)
        assertEquals("sucursal-norte", insert.branch)
    }
}
