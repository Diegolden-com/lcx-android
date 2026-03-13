package com.cleanx.lcx.feature.sales.domain

import com.cleanx.lcx.feature.tickets.data.AddOnCatalogRecord
import com.cleanx.lcx.feature.tickets.data.ServiceCatalogRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesCatalogRulesTest {

    @Test
    fun `isEquipmentService matches maquinaria and self-service names`() {
        assertTrue(
            isEquipmentService(
                ServiceCatalogRecord(
                    id = "1",
                    name = "Lavadora 18kg",
                    description = null,
                    category = "SERVICIOS",
                    price = 50.0,
                    unit = "pieza",
                    active = true,
                ),
            ),
        )

        assertTrue(
            isEquipmentService(
                ServiceCatalogRecord(
                    id = "2",
                    name = "Servicio cualquiera",
                    description = null,
                    category = "MAQUINARIA",
                    price = 30.0,
                    unit = "pieza",
                    active = true,
                ),
            ),
        )
    }

    @Test
    fun `isProductAddOn excludes bedding-like names from sales products`() {
        assertTrue(
            isProductAddOn(
                AddOnCatalogRecord(
                    id = "soap",
                    name = "Jabón premium",
                    description = null,
                    price = 12.0,
                    active = true,
                ),
            ),
        )

        assertFalse(
            isProductAddOn(
                AddOnCatalogRecord(
                    id = "bedding",
                    name = "Edredón King",
                    description = null,
                    price = 140.0,
                    active = true,
                ),
            ),
        )
    }
}
