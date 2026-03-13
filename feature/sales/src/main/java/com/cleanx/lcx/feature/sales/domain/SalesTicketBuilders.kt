package com.cleanx.lcx.feature.sales.domain

import com.cleanx.lcx.core.model.PaymentMethod
import com.cleanx.lcx.core.network.TicketDraft
import com.cleanx.lcx.feature.tickets.data.AddOnCatalogRecord
import com.cleanx.lcx.feature.tickets.data.InventoryCatalogRecord
import com.cleanx.lcx.feature.tickets.data.ServiceCatalogRecord
import com.cleanx.lcx.feature.tickets.data.TicketCustomerFields
import com.cleanx.lcx.feature.tickets.data.buildTicketCustomerFields
import com.cleanx.lcx.feature.tickets.data.filterSellableInventoryItems
import java.time.Instant

private data class SalesPaymentValues(
    val paymentMethod: String,
    val paidAt: String,
)

fun calculateSalesCartTotal(
    cart: Map<String, Int>,
    equipmentServices: List<ServiceCatalogRecord>,
    productAddOns: List<AddOnCatalogRecord>,
    inventoryItems: List<InventoryCatalogRecord>,
): Double {
    var total = 0.0
    val equipmentById = equipmentServices.associateBy { it.id }
    val addOnsById = productAddOns.associateBy { it.id }
    val inventoryById = filterSellableInventoryItems(inventoryItems).associateBy { it.id }

    cart.forEach { (id, quantity) ->
        when {
            equipmentById[id] != null -> total += equipmentById.getValue(id).price * quantity
            addOnsById[id] != null -> total += addOnsById.getValue(id).price * quantity
            inventoryById[id] != null -> total += inventoryById.getValue(id).price * quantity
        }
    }

    return total
}

fun buildSalesTicketDrafts(
    customer: com.cleanx.lcx.feature.tickets.data.CustomerDraft,
    cart: Map<String, Int>,
    equipmentServices: List<ServiceCatalogRecord>,
    productAddOns: List<AddOnCatalogRecord>,
    inventoryItems: List<InventoryCatalogRecord>,
    paymentMethod: PaymentMethod,
    chargedAt: Instant,
): List<TicketDraft> {
    val customerFields = buildTicketCustomerFields(customer)
    val payment = SalesPaymentValues(
        paymentMethod = paymentMethod.toWireValue(),
        paidAt = chargedAt.toString(),
    )

    val equipmentTickets = buildSalesEquipmentTickets(
        customer = customerFields,
        cart = cart,
        equipmentServices = equipmentServices,
        payment = payment,
    )
    val productsTicket = buildSalesProductsTicket(
        customer = customerFields,
        cart = cart,
        productAddOns = productAddOns,
        inventoryItems = inventoryItems,
        payment = payment,
    )

    return if (productsTicket != null) {
        equipmentTickets + productsTicket
    } else {
        equipmentTickets
    }
}

fun buildSalesEquipmentTickets(
    customer: TicketCustomerFields,
    cart: Map<String, Int>,
    equipmentServices: List<ServiceCatalogRecord>,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    chargedAt: Instant = Instant.now(),
): List<TicketDraft> {
    return buildSalesEquipmentTickets(
        customer = customer,
        cart = cart,
        equipmentServices = equipmentServices,
        payment = SalesPaymentValues(
            paymentMethod = paymentMethod.toWireValue(),
            paidAt = chargedAt.toString(),
        ),
    )
}

fun buildSalesProductsTicket(
    customer: TicketCustomerFields,
    cart: Map<String, Int>,
    productAddOns: List<AddOnCatalogRecord>,
    inventoryItems: List<InventoryCatalogRecord>,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    chargedAt: Instant = Instant.now(),
): TicketDraft? {
    return buildSalesProductsTicket(
        customer = customer,
        cart = cart,
        productAddOns = productAddOns,
        inventoryItems = inventoryItems,
        payment = SalesPaymentValues(
            paymentMethod = paymentMethod.toWireValue(),
            paidAt = chargedAt.toString(),
        ),
    )
}

private fun buildSalesEquipmentTickets(
    customer: TicketCustomerFields,
    cart: Map<String, Int>,
    equipmentServices: List<ServiceCatalogRecord>,
    payment: SalesPaymentValues,
): List<TicketDraft> {
    val tickets = mutableListOf<TicketDraft>()
    val equipmentById = equipmentServices.associateBy { it.id }

    cart.forEach { (itemId, quantity) ->
        val service = equipmentById[itemId] ?: return@forEach

        repeat(quantity.coerceAtLeast(0)) {
            tickets += TicketDraft(
                customerName = customer.customerName,
                customerPhone = customer.customerPhone,
                customerEmail = customer.customerEmail,
                customerId = customer.customerId,
                serviceType = "in-store",
                service = service.name,
                weight = 1.0,
                status = "delivered",
                totalAmount = service.price,
                subtotal = service.price,
                addOnsTotal = 0.0,
                addOns = emptyList(),
                promisedPickupDate = payment.paidAt,
                paymentMethod = payment.paymentMethod,
                paymentStatus = "paid",
                paidAmount = service.price,
                paidAt = payment.paidAt,
            )
        }
    }

    return tickets
}

private fun buildSalesProductsTicket(
    customer: TicketCustomerFields,
    cart: Map<String, Int>,
    productAddOns: List<AddOnCatalogRecord>,
    inventoryItems: List<InventoryCatalogRecord>,
    payment: SalesPaymentValues,
): TicketDraft? {
    val addOnIds = mutableListOf<String>()
    var totalAmount = 0.0

    val addOnsById = productAddOns.associateBy { it.id }
    val inventoryById = filterSellableInventoryItems(inventoryItems).associateBy { it.id }

    cart.forEach { (itemId, quantity) ->
        val normalizedQuantity = quantity.coerceAtLeast(0)
        when {
            addOnsById[itemId] != null -> {
                val item = addOnsById.getValue(itemId)
                totalAmount += item.price * normalizedQuantity
                repeat(normalizedQuantity) { addOnIds += itemId }
            }

            inventoryById[itemId] != null -> {
                val item = inventoryById.getValue(itemId)
                totalAmount += item.price * normalizedQuantity
                repeat(normalizedQuantity) { addOnIds += itemId }
            }
        }
    }

    if (addOnIds.isEmpty()) {
        return null
    }

    return TicketDraft(
        customerName = customer.customerName,
        customerPhone = customer.customerPhone,
        customerEmail = customer.customerEmail,
        customerId = customer.customerId,
        serviceType = "in-store",
        service = "Venta Productos",
        weight = 0.0,
        status = "delivered",
        totalAmount = totalAmount,
        subtotal = 0.0,
        addOnsTotal = totalAmount,
        addOns = addOnIds,
        promisedPickupDate = payment.paidAt,
        paymentMethod = payment.paymentMethod,
        paymentStatus = "paid",
        paidAmount = totalAmount,
        paidAt = payment.paidAt,
    )
}

private fun PaymentMethod.toWireValue(): String {
    return when (this) {
        PaymentMethod.CASH -> "cash"
        PaymentMethod.CARD -> "card"
        PaymentMethod.TRANSFER -> "transfer"
    }
}
