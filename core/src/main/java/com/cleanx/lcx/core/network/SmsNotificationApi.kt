package com.cleanx.lcx.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

@Serializable
data class SmsSendRequest(
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("to") val to: String,
    @SerialName("message") val message: String,
    @SerialName("requested_by") val requestedBy: String? = null,
    val metadata: Map<String, String?>? = null,
)

@Serializable
data class SmsSendResponseData(
    @SerialName("notification_id") val notificationId: String,
    val status: String,
    val provider: String,
    val idempotent: Boolean,
)

@Serializable
data class SmsSendResponse(
    val data: SmsSendResponseData? = null,
    val error: String? = null,
    val code: String? = null,
    val details: String? = null,
    @SerialName("correlation_id") val correlationId: String? = null,
)

interface SmsNotificationApi {
    @POST
    suspend fun sendSms(
        @Url url: String,
        @Body request: SmsSendRequest,
    ): Response<SmsSendResponse>
}
