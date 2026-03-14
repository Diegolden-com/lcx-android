package com.cleanx.lcx.feature.payments.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnavailableZettlePaymentManagerTest {

    private val manager = UnavailableZettlePaymentManager()

    @Test
    fun `capability marks real backend as unavailable`() {
        val capability = manager.capability()

        assertEquals(PaymentBackendType.ZETTLE_REAL, capability.backendType)
        assertEquals("SDK real no integrado", capability.backendLabel)
        assertFalse(capability.canAcceptPayments)
        assertFalse(capability.isInitialized)
        assertTrue(capability.statusMessage.contains("USE_REAL_ZETTLE"))
    }

    @Test
    fun `requestPayment returns explicit failure instead of throwing`() = runTest {
        val result = manager.requestPayment(amount = 42.0, reference = "venta-123")

        assertTrue(result is PaymentResult.Failed)
        result as PaymentResult.Failed
        assertEquals("ZETTLE_SDK_NOT_INTEGRATED", result.errorCode)
        assertEquals("venta-123", result.reference)
        assertTrue(result.message.contains("tarjeta real no disponible"))
    }
}
