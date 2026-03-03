package com.cleanx.lcx.feature.water.data

/**
 * Hardcoded water delivery providers matching the PWA's provider list.
 */
data class WaterProvider(
    val id: String,
    val name: String,
    val price: Int,
    val deliveryTime: String,
    val rating: Double,
    val phone: String,
)

val WATER_PROVIDERS: List<WaterProvider> = listOf(
    WaterProvider(
        id = "aguafina",
        name = "Aguafina Express",
        price = 180,
        deliveryTime = "2-3 horas",
        rating = 4.8,
        phone = "555-0123",
    ),
    WaterProvider(
        id = "cristal",
        name = "Agua Cristal",
        price = 165,
        deliveryTime = "3-4 horas",
        rating = 4.6,
        phone = "555-0456",
    ),
    WaterProvider(
        id = "pureza",
        name = "Pureza Natural",
        price = 175,
        deliveryTime = "1-2 horas",
        rating = 4.9,
        phone = "555-0789",
    ),
)
