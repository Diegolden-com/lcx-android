package com.cleanx.lcx.feature.payments.data

import android.content.Context
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder bound when the build expects the real Zettle backend but the SDK
 * has not been wired into the app yet.
 */
@Singleton
class UnavailableZettlePaymentManager @Inject constructor() : PaymentManager {

    override suspend fun initialize(context: Context) {
        Timber.w("USE_REAL_ZETTLE=true but the real Zettle SDK is not integrated in this build")
    }

    override fun isInitialized(): Boolean = false

    override fun capability(): PaymentCapability = PaymentCapability(
        backendType = PaymentBackendType.ZETTLE_REAL,
        backendLabel = "SDK real no integrado",
        canAcceptPayments = false,
        isInitialized = false,
        statusMessage =
            "USE_REAL_ZETTLE=true, pero Android aun no integra el SDK real de Zettle. " +
                "Faltan dependencias del SDK, OAuth/callback y credenciales validas; " +
                "tarjeta real no disponible en este build.",
    )

    override suspend fun requestPayment(amount: Double, reference: String): PaymentResult {
        val capability = capability()
        Timber.w(
            "Rejecting payment request amount=%.2f ref=%s because real Zettle backend is unavailable",
            amount,
            reference,
        )
        return PaymentResult.Failed(
            errorCode = "ZETTLE_SDK_NOT_INTEGRATED",
            message = capability.statusMessage,
            reference = reference,
        )
    }
}
