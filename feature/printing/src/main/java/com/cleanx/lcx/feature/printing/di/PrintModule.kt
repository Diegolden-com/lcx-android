package com.cleanx.lcx.feature.printing.di

import com.cleanx.lcx.core.config.FeatureFlags
import com.cleanx.lcx.feature.printing.data.BrotherPrinterManager
import com.cleanx.lcx.feature.printing.data.PrinterManager
import com.cleanx.lcx.feature.printing.data.StubPrinterManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Hilt module that wires [PrinterManager] to the correct implementation
 * based on the [FeatureFlags.useRealBrother] flag.
 *
 * - When `useRealBrother=true` and AAR is present: [BrotherPrinterManager]
 * - Otherwise: [StubPrinterManager]
 *
 * The flag is set per build variant in `app/build.gradle.kts`.
 */
@Module
@InstallIn(SingletonComponent::class)
object PrintModule {

    @Provides
    @Singleton
    fun providePrinterManager(
        featureFlags: FeatureFlags,
        stubPrinterManager: StubPrinterManager,
        brotherPrinterManager: BrotherPrinterManager,
    ): PrinterManager {
        return if (featureFlags.useRealBrother) {
            if (brotherPrinterManager.isSdkAvailable()) {
                Timber.i("PrintModule: using BrotherPrinterManager (useRealBrother=true)")
                brotherPrinterManager
            } else {
                Timber.w(
                    "useRealBrother=true but Brother SDK AAR is missing; " +
                        "falling back to StubPrinterManager",
                )
                stubPrinterManager
            }
        } else {
            Timber.d("PrintModule: using StubPrinterManager (useRealBrother=false)")
            stubPrinterManager
        }
    }
}
