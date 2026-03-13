package com.cleanx.lcx.feature.sales.domain

import com.cleanx.lcx.core.model.PaymentMethod
import com.cleanx.lcx.core.model.Ticket
import com.cleanx.lcx.feature.tickets.data.AddOnCatalogRecord
import com.cleanx.lcx.feature.tickets.data.CustomerDraft
import com.cleanx.lcx.feature.tickets.data.InventoryCatalogRecord
import com.cleanx.lcx.feature.tickets.data.ServiceCatalogRecord

const val AnonymousSalesCustomerName = "Cliente anónimo"
const val AnonymousSalesCustomerPhone = "0000000000"

data class SalesCatalogSnapshot(
    val equipmentServices: List<ServiceCatalogRecord>,
    val productAddOns: List<AddOnCatalogRecord>,
    val inventoryItems: List<InventoryCatalogRecord>,
)

data class SalesCheckoutRequest(
    val customer: CustomerDraft,
    val cart: Map<String, Int>,
    val catalogs: SalesCatalogSnapshot,
    val paymentMethod: PaymentMethod,
)

data class CardCapturedCreateFailure(
    val transactionId: String,
    val amount: Double,
    val correlationId: String,
    val message: String,
)

sealed interface LoadSalesCatalogsResult {
    data class Success(val snapshot: SalesCatalogSnapshot) : LoadSalesCatalogsResult
    data class Failure(val message: String) : LoadSalesCatalogsResult
}

sealed interface SalesCheckoutResult {
    data class Success(
        val tickets: List<Ticket>,
        val transactionId: String? = null,
        val correlationId: String,
    ) : SalesCheckoutResult

    data class ValidationFailure(val message: String) : SalesCheckoutResult
    data class PaymentCancelled(val message: String) : SalesCheckoutResult
    data class PaymentFailed(val message: String) : SalesCheckoutResult
    data class CreateFailed(val message: String) : SalesCheckoutResult
    data class CardCapturedCreateFailed(
        val failure: CardCapturedCreateFailure,
    ) : SalesCheckoutResult
}

fun anonymousSalesCustomer(): CustomerDraft {
    return CustomerDraft(
        customerId = null,
        fullName = AnonymousSalesCustomerName,
        phone = AnonymousSalesCustomerPhone,
        email = "",
    )
}

fun isEquipmentService(service: ServiceCatalogRecord): Boolean {
    if (service.category == "MAQUINARIA") {
        return true
    }

    val normalizedName = service.name.uppercase()
    return normalizedName.contains("LAVADORA") ||
        normalizedName.contains("SECADORA") ||
        normalizedName.contains("COMBO") ||
        normalizedName.contains("CENTRIFUGADO")
}

fun isProductAddOn(addOn: AddOnCatalogRecord): Boolean {
    val normalizedName = addOn.name.lowercase()
    return !normalizedName.contains("edredón") &&
        !normalizedName.contains("edredon") &&
        !normalizedName.contains("cobertor") &&
        !normalizedName.contains("colcha")
}
