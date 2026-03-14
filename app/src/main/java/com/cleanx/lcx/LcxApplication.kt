package com.cleanx.lcx

import android.app.Application
import com.cleanx.lcx.feature.payments.data.PaymentManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LcxApplication : Application() {
    @Inject
    lateinit var paymentManager: PaymentManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        runCatching {
            runBlocking {
                paymentManager.initialize(this@LcxApplication)
            }
        }.onFailure { throwable ->
            Timber.e(throwable, "Payment backend initialization failed during app startup")
        }
    }
}
