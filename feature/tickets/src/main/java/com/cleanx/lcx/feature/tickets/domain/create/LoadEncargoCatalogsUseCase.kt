package com.cleanx.lcx.feature.tickets.domain.create

import com.cleanx.lcx.feature.tickets.data.AddOnCatalogRecord
import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.ErrorMessages
import com.cleanx.lcx.feature.tickets.data.InventoryCatalogRecord
import com.cleanx.lcx.feature.tickets.data.ServiceCatalogRecord
import com.cleanx.lcx.feature.tickets.data.TicketCreationRepository
import com.cleanx.lcx.feature.tickets.data.isBaseWashByKilo
import com.cleanx.lcx.feature.tickets.data.isBeddingService
import com.cleanx.lcx.feature.tickets.data.isExtraAddOn
import javax.inject.Inject

data class EncargoCatalogSnapshot(
    val services: List<ServiceCatalogRecord>,
    val beddingItems: List<ServiceCatalogRecord>,
    val extraItems: List<AddOnCatalogRecord>,
    val inventoryItems: List<InventoryCatalogRecord>,
    val defaultBaseServiceId: String?,
)

sealed interface LoadEncargoCatalogsResult {
    data class Success(val snapshot: EncargoCatalogSnapshot) : LoadEncargoCatalogsResult
    data class Failure(val message: String) : LoadEncargoCatalogsResult
}

class LoadEncargoCatalogsUseCase @Inject constructor(
    private val repository: TicketCreationRepository,
) {

    suspend operator fun invoke(): LoadEncargoCatalogsResult {
        return when (val result = repository.loadCatalogs()) {
            is ApiResult.Success -> {
                val services = result.data.services
                val beddingItems = services.filter(::isBeddingService)
                val extraItems = result.data.addOns.filter(::isExtraAddOn)
                val inventoryItems = result.data.inventoryItems

                LoadEncargoCatalogsResult.Success(
                    snapshot = EncargoCatalogSnapshot(
                        services = services,
                        beddingItems = beddingItems,
                        extraItems = extraItems,
                        inventoryItems = inventoryItems,
                        defaultBaseServiceId = services.firstOrNull(::isBaseWashByKilo)?.id
                            ?: services.firstOrNull()?.id,
                    ),
                )
            }

            is ApiResult.Error -> {
                LoadEncargoCatalogsResult.Failure(
                    message = ErrorMessages.forCode(result.code, result.message),
                )
            }
        }
    }
}
