package com.cleanx.lcx.feature.sales.domain

import com.cleanx.lcx.core.model.PaymentMethod
import com.cleanx.lcx.feature.tickets.data.AddOnCatalogRecord
import com.cleanx.lcx.feature.tickets.data.InventoryCatalogRecord
import com.cleanx.lcx.feature.tickets.data.ServiceCatalogRecord
import com.cleanx.lcx.feature.tickets.data.TicketCustomerFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class SalesTicketBuildersTest {

    @Test
    fun `calculateSalesCartTotal keeps mixed cart total and ignores non-sellable inventory`() {
        val cart = mapOf(
            "washer" to 2,
            "soap" to 3,
            "valid-inventory" to 2,
            "hidden-inventory" to 5,
        )

        val total = calculateSalesCartTotal(
            cart = cart,
            equipmentServices = listOf(service(id = "washer", name = "Lavadora 18kg", price = 50.0)),
            productAddOns = listOf(addOn(id = "soap", name = "Jabón", price = 12.0)),
            inventoryItems = listOf(
                inventory(id = "valid-inventory", itemName = "Bolsa", quantity = 3, isForSale = true, price = 9.5),
                inventory(id = "hidden-inventory", itemName = "Oculto", quantity = 10, isForSale = false, price = 7.0),
            ),
        )

        assertEquals(155.0, total, 0.001)
    }

    @Test
    fun `buildSalesEquipmentTickets creates one delivered ticket per equipment quantity`() {
        val tickets = buildSalesEquipmentTickets(
            customer = customerFields(),
            cart = mapOf("washer" to 2, "dryer" to 1),
            equipmentServices = listOf(
                service(id = "washer", name = "Lavadora 18kg", price = 50.0),
                service(id = "dryer", name = "Secadora 18kg", price = 40.0),
            ),
            paymentMethod = PaymentMethod.CARD,
            chargedAt = Instant.parse("2026-03-13T12:00:00Z"),
        )

        assertEquals(3, tickets.size)
        assertEquals("Lavadora 18kg", tickets[0].service)
        assertEquals("delivered", tickets[0].status)
        assertEquals("card", tickets[0].paymentMethod)
        assertEquals("paid", tickets[0].paymentStatus)
        assertEquals(50.0, tickets[0].paidAmount!!, 0.001)
        assertEquals("Secadora 18kg", tickets[2].service)
        assertEquals(40.0, tickets[2].totalAmount!!, 0.001)
    }

    @Test
    fun `buildSalesProductsTicket consolidates products and sellable inventory only`() {
        val ticket = buildSalesProductsTicket(
            customer = customerFields(),
            cart = mapOf(
                "soap" to 2,
                "valid-inventory" to 3,
                "hidden-inventory" to 1,
            ),
            productAddOns = listOf(addOn(id = "soap", name = "Jabón", price = 12.0)),
            inventoryItems = listOf(
                inventory(id = "valid-inventory", itemName = "Bolsa", quantity = 6, isForSale = true, price = 9.5),
                inventory(id = "hidden-inventory", itemName = "Oculto", quantity = 10, isForSale = false, price = 7.0),
            ),
            paymentMethod = PaymentMethod.TRANSFER,
            chargedAt = Instant.parse("2026-03-13T12:00:00Z"),
        )

        assertNotNull(ticket)
        assertEquals("Venta Productos", ticket?.service)
        assertEquals(listOf("soap", "soap", "valid-inventory", "valid-inventory", "valid-inventory"), ticket?.addOns)
        assertEquals(52.5, ticket?.totalAmount ?: 0.0, 0.001)
        assertEquals("transfer", ticket?.paymentMethod)
    }

    @Test
    fun `buildSalesProductsTicket returns null when cart only has non-sellable inventory`() {
        val ticket = buildSalesProductsTicket(
            customer = customerFields(),
            cart = mapOf("hidden-inventory" to 2),
            productAddOns = emptyList(),
            inventoryItems = listOf(
                inventory(id = "hidden-inventory", itemName = "Oculto", quantity = 10, isForSale = false, price = 7.0),
            ),
            paymentMethod = PaymentMethod.CASH,
            chargedAt = Instant.parse("2026-03-13T12:00:00Z"),
        )

        assertNull(ticket)
    }

    private fun customerFields(): TicketCustomerFields {
        return TicketCustomerFields(
            customerName = "Cliente QA",
            customerPhone = "5551234567",
            customerEmail = null,
            customerId = null,
        )
    }

    private fun service(
        id: String,
        name: String,
        price: Double,
        category: String = "MAQUINARIA",
    ): ServiceCatalogRecord {
        return ServiceCatalogRecord(
            id = id,
            name = name,
            description = null,
            category = category,
            price = price,
            unit = "pieza",
            active = true,
        )
    }

    private fun addOn(
        id: String,
        name: String,
        price: Double,
    ): AddOnCatalogRecord {
        return AddOnCatalogRecord(
            id = id,
            name = name,
            description = null,
            price = price,
            active = true,
        )
    }

    private fun inventory(
        id: String,
        itemName: String,
        quantity: Int,
        isForSale: Boolean,
        price: Double,
    ): InventoryCatalogRecord {
        return InventoryCatalogRecord(
            id = id,
            itemName = itemName,
            category = "General",
            quantity = quantity,
            unit = "pieza",
            minQuantity = 0,
            price = price,
            isForSale = isForSale,
            sku = null,
            barcode = null,
            productCode = null,
            branch = "Sucursal Centro",
        )
    }
}
