package com.cleanx.lcx.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified error type for Supabase operations.
 *
 * Feature modules should map these to their own UI-layer error models
 * if needed, but the sealed hierarchy gives enough granularity for
 * common cases (auth expired, not found, network issues, etc.).
 */
sealed class SupabaseError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /** The session token was null or rejected by the server (401/403). */
    class Unauthorized(
        message: String = "Not authenticated",
        cause: Throwable? = null,
    ) : SupabaseError(message, cause)

    /** The server rejected the request with a non-auth client error (4xx). */
    class BadRequest(
        message: String,
        val statusCode: Int? = null,
        cause: Throwable? = null,
    ) : SupabaseError(message, cause)

    /** The requested row(s) could not be found. */
    class NotFound(
        message: String = "Resource not found",
        cause: Throwable? = null,
    ) : SupabaseError(message, cause)

    /** A server-side error (5xx). */
    class ServerError(
        message: String,
        val statusCode: Int? = null,
        cause: Throwable? = null,
    ) : SupabaseError(message, cause)

    /** A network / transport-level error. */
    class NetworkError(
        message: String = "Network error",
        cause: Throwable? = null,
    ) : SupabaseError(message, cause)

    /** Catch-all for unexpected exceptions. */
    class Unknown(
        message: String = "Unknown error",
        cause: Throwable? = null,
    ) : SupabaseError(message, cause)
}

/**
 * Thin wrapper around the Supabase Kotlin SDK that provides:
 *
 * 1. Typed, suspend-based CRUD helpers for any Supabase table.
 * 2. Consistent error mapping to [SupabaseError].
 * 3. Logging via Timber.
 *
 * This is **additive** -- it does not replace the existing Retrofit/OkHttp
 * stack used by [TicketApi].  Feature modules that need direct table access
 * (water, checklists) inject this class; everything else keeps using Retrofit.
 *
 * Authentication is handled transparently: the underlying [SupabaseClient]
 * is configured with an `accessToken` lambda that reads the current JWT
 * from [SessionManager] via [TokenProvider].
 */
@Singleton
class SupabaseTableClient @Inject constructor(
    @PublishedApi internal val client: SupabaseClient,
) {

    // -- SELECT ---------------------------------------------------------------

    /**
     * Select all rows from [table], optionally filtering via [filterBlock].
     *
     * Example:
     * ```
     * val levels = supabaseTableClient.selectAll<WaterLevel>("water_levels") {
     *     eq("branch", "main-branch")
     * }
     * ```
     */
    suspend inline fun <reified T : Any> selectAll(
        table: String,
        columns: Columns = Columns.ALL,
        noinline filterBlock: (PostgrestFilterBuilder.() -> Unit)? = null,
    ): Result<List<T>> = runCatching(table, "selectAll") {
        val result = client.from(table).select(columns) {
            if (filterBlock != null) filter(filterBlock)
        }
        result.decodeList<T>()
    }

    /**
     * Select a single row from [table] matching [filterBlock].
     * Returns `null` inside the [Result] when no row matches.
     */
    suspend inline fun <reified T : Any> selectSingle(
        table: String,
        columns: Columns = Columns.ALL,
        noinline filterBlock: PostgrestFilterBuilder.() -> Unit,
    ): Result<T?> = runCatching(table, "selectSingle") {
        val result = client.from(table).select(columns) {
            filter(filterBlock)
            limit(1)
        }
        result.decodeList<T>().firstOrNull()
    }

    /**
     * Select rows from [table] with full control over the request builder.
     *
     * Unlike [selectAll], the [requestBlock] lambda receives a
     * [PostgrestRequestBuilder] so callers can use `order`, `limit`,
     * `filter`, etc.
     *
     * Example:
     * ```
     * val latest = supabaseTableClient.selectWithRequest<WaterLevel>("water_levels") {
     *     filter { eq("branch", "main") }
     *     order("created_at", Order.DESCENDING)
     *     limit(1)
     * }
     * ```
     */
    suspend inline fun <reified T : Any> selectWithRequest(
        table: String,
        columns: Columns = Columns.ALL,
        noinline requestBlock: (PostgrestRequestBuilder.() -> Unit)? = null,
    ): Result<List<T>> = runCatching(table, "selectWithRequest") {
        val result = client.from(table).select(columns) {
            if (requestBlock != null) requestBlock()
        }
        result.decodeList<T>()
    }

    // -- INSERT ---------------------------------------------------------------

    /**
     * Insert a single row into [table] and return the inserted row.
     */
    suspend inline fun <reified T : Any> insert(
        table: String,
        value: T,
    ): Result<T> = runCatching(table, "insert") {
        client.from(table).insert(value) {
            select()
        }.decodeSingle<T>()
    }

    /**
     * Insert a payload type [I] and decode the inserted row as [O].
     * Useful when insert DTO and select DTO differ.
     */
    suspend inline fun <reified I : Any, reified O : Any> insertReturning(
        table: String,
        value: I,
    ): Result<O> = runCatching(table, "insertReturning") {
        client.from(table).insert(value) {
            select()
        }.decodeSingle<O>()
    }

    /**
     * Insert multiple rows into [table] and return all inserted rows.
     */
    suspend inline fun <reified T : Any> insertMany(
        table: String,
        values: List<T>,
    ): Result<List<T>> = runCatching(table, "insertMany") {
        client.from(table).insert(values) {
            select()
        }.decodeList<T>()
    }

    /**
     * Insert multiple payload rows of type [I] and decode the inserted rows as [O].
     */
    suspend inline fun <reified I : Any, reified O : Any> insertManyReturning(
        table: String,
        values: List<I>,
    ): Result<List<O>> = runCatching(table, "insertManyReturning") {
        client.from(table).insert(values) {
            select()
        }.decodeList<O>()
    }

    // -- UPDATE ---------------------------------------------------------------

    /**
     * Update rows in [table] matching [filterBlock] and return updated rows.
     */
    suspend inline fun <reified T : Any> update(
        table: String,
        value: T,
        noinline filterBlock: PostgrestFilterBuilder.() -> Unit,
    ): Result<List<T>> = runCatching(table, "update") {
        client.from(table).update(value) {
            select()
            filter(filterBlock)
        }.decodeList<T>()
    }

    /**
     * Update rows in [table] with payload [I] and decode the updated rows as [O].
     * Useful when the patch payload is partial but the caller needs full row models.
     */
    suspend inline fun <reified I : Any, reified O : Any> updateReturning(
        table: String,
        value: I,
        noinline filterBlock: PostgrestFilterBuilder.() -> Unit,
    ): Result<List<O>> = runCatching(table, "updateReturning") {
        client.from(table).update(value) {
            select()
            filter(filterBlock)
        }.decodeList<O>()
    }

    // -- DELETE ---------------------------------------------------------------

    /**
     * Delete rows in [table] matching [filterBlock].
     */
    suspend fun delete(
        table: String,
        filterBlock: PostgrestFilterBuilder.() -> Unit,
    ): Result<Unit> = runCatching(table, "delete") {
        client.from(table).delete {
            filter(filterBlock)
        }
        Unit
    }

    // -- Error handling -------------------------------------------------------

    /**
     * Wraps a Supabase SDK call, catches exceptions, and maps them to
     * [SupabaseError] subtypes.
     */
    @PublishedApi
    internal inline fun <T> runCatching(
        table: String,
        operation: String,
        block: () -> T,
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: RestException) {
            val mapped = mapRestException(e, table, operation)
            Timber.w(mapped, "Supabase %s on '%s' failed", operation, table)
            Result.failure(mapped)
        } catch (e: HttpRequestException) {
            val mapped = SupabaseError.NetworkError(
                message = "Network error during $operation on '$table': ${e.message}",
                cause = e,
            )
            Timber.w(mapped, "Supabase %s on '%s' network error", operation, table)
            Result.failure(mapped)
        } catch (e: IllegalStateException) {
            // Thrown when accessToken lambda cannot provide a token
            val mapped = SupabaseError.Unauthorized(
                message = e.message ?: "Not authenticated",
                cause = e,
            )
            Timber.w(mapped, "Supabase %s on '%s' auth error", operation, table)
            Result.failure(mapped)
        } catch (e: Exception) {
            val mapped = SupabaseError.Unknown(
                message = "Unexpected error during $operation on '$table': ${e.message}",
                cause = e,
            )
            Timber.e(mapped, "Supabase %s on '%s' unexpected error", operation, table)
            Result.failure(mapped)
        }
    }

    @PublishedApi
    internal fun mapRestException(
        e: RestException,
        table: String,
        operation: String,
    ): SupabaseError {
        val statusCode = e.statusCode
        val msg = e.message ?: "REST error during $operation on '$table'"
        return when {
            statusCode == 401 || statusCode == 403 ->
                SupabaseError.Unauthorized(msg, e)
            statusCode == 404 || (e.message?.contains("PGRST116") == true) ->
                SupabaseError.NotFound(msg, e)
            statusCode in 400..499 ->
                SupabaseError.BadRequest(msg, statusCode, e)
            statusCode in 500..599 ->
                SupabaseError.ServerError(msg, statusCode, e)
            else ->
                SupabaseError.Unknown(msg, e)
        }
    }
}
