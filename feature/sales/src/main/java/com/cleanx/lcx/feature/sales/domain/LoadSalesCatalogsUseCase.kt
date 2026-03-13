package com.cleanx.lcx.feature.sales.domain

import com.cleanx.lcx.feature.tickets.data.ApiResult
import com.cleanx.lcx.feature.tickets.data.ErrorMessages
import com.cleanx.lcx.feature.tickets.data.TicketCreationRepository
import javax.inject.Inject

class LoadSalesCatalogsUseCase @Inject constructor(
    private val repository: TicketCreationRepository,
) {

    suspend operator fun invoke(): LoadSalesCatalogsResult {
        return when (val result = repository.loadCatalogs()) {
            is ApiResult.Success -> LoadSalesCatalogsResult.Success(
                snapshot = SalesCatalogSnapshot(
                    equipmentServices = result.data.services.filter(::isEquipmentService),
                    productAddOns = result.data.addOns.filter(::isProductAddOn),
                    inventoryItems = result.data.inventoryItems,
                ),
            )

            is ApiResult.Error -> LoadSalesCatalogsResult.Failure(
                message = ErrorMessages.forCode(result.code, result.message),
            )
        }
    }
}
