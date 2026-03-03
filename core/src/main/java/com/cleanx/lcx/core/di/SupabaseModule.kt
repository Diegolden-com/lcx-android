package com.cleanx.lcx.core.di

import com.cleanx.lcx.core.config.BuildConfigProvider
import com.cleanx.lcx.core.network.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import timber.log.Timber
import javax.inject.Singleton

/**
 * Provides the Supabase [SupabaseClient] configured with:
 *
 * - The project URL and anon key from [BuildConfigProvider].
 * - An `accessToken` lambda that reads the current JWT from the
 *   existing [TokenProvider] (backed by [DataStoreSessionStore]).
 * - The [Postgrest] plugin for direct table access.
 *
 * This module is **additive** -- it does not touch the Retrofit/OkHttp
 * graph defined in [NetworkModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        config: BuildConfigProvider,
        tokenProvider: TokenProvider,
    ): SupabaseClient {
        Timber.d(
            "Initialising Supabase client → url=%s",
            config.supabaseUrl,
        )
        return createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseAnonKey,
        ) {
            // Use the existing session token instead of GoTrue/Auth plugin.
            // This keeps auth management in the existing SessionManager flow.
            accessToken = {
                tokenProvider.getAccessToken()
                    ?: throw IllegalStateException(
                        "No access token available – user is not authenticated"
                    )
            }

            install(Postgrest) {
                defaultSchema = "public"
                // Use kotlinx.serialization @SerialName annotations for column
                // name mapping instead of the default camelCase-to-snake_case
                // conversion.  This matches how the data classes are defined.
                propertyConversionMethod = PropertyConversionMethod.SERIAL_NAME
            }
        }
    }
}
