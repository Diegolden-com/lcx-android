package com.cleanx.lcx.feature.tickets.domain.create

import com.cleanx.lcx.core.model.PaymentMethod
import com.cleanx.lcx.feature.tickets.data.ServiceCatalogRecord
import com.cleanx.lcx.feature.tickets.data.TicketPricingSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BuildEncargoDraftUseCaseTest {

    private val useCase = BuildEncargoDraftUseCase()

    private val baseService = ServiceCatalogRecord(
        id = "service-base",
        name = "Lavado por KG",
        description = "Servicio base",
        category = "Lavado",
        price = 50.0,
        unit = "kg",
        active = true,
    )

    @Test
    fun `build draft maps paid encargo metadata and pricing`() {
        val state = CreateTicketUiState(
            isLoadingCatalogs = false,
            services = listOf(baseService),
            selectedBaseServiceId = baseService.id,
            weight = "4.5",
            pickupEstimate = "2026-03-15T18:30",
            pickupTargetHours = 24,
            useSharedMachinePool = true,
            selectedSpecialItemIds = listOf("delicadas", "oscuras"),
            specialItemNotes = "Separar por color",
            paymentChoice = EncargoPaymentChoice.PAID,
            paymentMethod = PaymentMethod.TRANSFER,
            pricing = TicketPricingSummary(
                subtotal = 225.0,
                addOnsTotal = 33.0,
                total = 258.0,
            ),
            customer = CustomerPickerUiState(
                selectedCustomer = com.cleanx.lcx.feature.tickets.data.CustomerDraft(
                    customerId = "customer-1",
                    fullName = "Maria Perez",
                    phone = "5512345678",
                    email = "maria@example.com",
                ),
            ),
        )

        val result = useCase(
            state = state,
            now = Instant.parse("2026-03-13T21:00:00Z"),
        )

        assertTrue(result is BuildEncargoDraftResult.Success)
        val draft = (result as BuildEncargoDraftResult.Success).draft
        assertEquals("Maria Perez", draft.customerName)
        assertEquals("5512345678", draft.customerPhone)
        assertEquals("maria@example.com", draft.customerEmail)
        assertEquals("customer-1", draft.customerId)
        assertEquals("wash-fold", draft.serviceType)
        assertEquals("Lavado por KG", draft.service)
        assertEquals(4.5, draft.weight!!, 0.001)
        assertEquals(258.0, draft.totalAmount!!, 0.001)
        assertEquals("paid", draft.paymentStatus)
        assertEquals("transfer", draft.paymentMethod)
        assertEquals(258.0, draft.paidAmount!!, 0.001)
        assertEquals("2026-03-13T21:00:00Z", draft.paidAt)
        assertEquals("pickup_target_hours=24 ; shared_machine_pool=yes ; special_items=delicadas|oscuras", draft.notes)
        assertTrue(draft.specialInstructions.orEmpty().contains("Separar prendas especiales"))
        assertTrue(draft.specialInstructions.orEmpty().contains("Separar por color"))
        assertTrue(draft.specialInstructions.orEmpty().contains("pool de maquinas"))
    }

    @Test
    fun `build draft returns customer validation errors when customer is incomplete`() {
        val state = CreateTicketUiState(
            isLoadingCatalogs = false,
            services = listOf(baseService),
            selectedBaseServiceId = baseService.id,
            weight = "4",
            pickupEstimate = "2026-03-15T18:30",
            customer = CustomerPickerUiState(
                selectedCustomer = com.cleanx.lcx.feature.tickets.data.CustomerDraft(
                    fullName = "A",
                    phone = "123",
                    email = "bad-mail",
                ),
            ),
        )

        val result = useCase(state)

        assertTrue(result is BuildEncargoDraftResult.ValidationFailure)
        val failure = result as BuildEncargoDraftResult.ValidationFailure
        assertEquals("Selecciona o crea un cliente valido con nombre y telefono.", failure.message)
        assertEquals(
            "Ingresa un nombre completo valido (minimo 2 caracteres).",
            failure.customerValidationErrors?.fullName,
        )
        assertEquals(
            "Ingresa un telefono valido (minimo 7 digitos).",
            failure.customerValidationErrors?.phone,
        )
        assertEquals(
            "Ingresa un correo valido o dejalo en blanco.",
            failure.customerValidationErrors?.email,
        )
    }

    @Test
    fun `build draft rejects invalid pickup estimate`() {
        val state = CreateTicketUiState(
            isLoadingCatalogs = false,
            services = listOf(baseService),
            selectedBaseServiceId = baseService.id,
            weight = "4",
            pickupEstimate = "2026/03/15 18:30",
            customer = CustomerPickerUiState(
                selectedCustomer = com.cleanx.lcx.feature.tickets.data.CustomerDraft(
                    customerId = "customer-1",
                    fullName = "Maria Perez",
                    phone = "5512345678",
                    email = "",
                ),
            ),
        )

        val result = useCase(state)

        assertTrue(result is BuildEncargoDraftResult.ValidationFailure)
        val failure = result as BuildEncargoDraftResult.ValidationFailure
        assertEquals("La fecha promesa debe usar formato AAAA-MM-DDTHH:MM.", failure.message)
        assertNull(failure.customerValidationErrors)
    }
}
