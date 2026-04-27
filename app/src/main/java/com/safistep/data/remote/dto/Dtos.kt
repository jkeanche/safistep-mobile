package com.safistep.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Auth DTOs ────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RequestOtpRequest(
    val phone: String,
    val purpose: String = "registration"
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    val phone: String,
    val code: String
)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
    val message: String,
    @Json(name = "temp_token") val tempToken: String,
    val phone: String
)

@JsonClass(generateAdapter = true)
data class SetPasswordRequest(
    @Json(name = "temp_token") val tempToken: String,
    val name: String?,
    val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val phone: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Long,
    val phone: String,
    val name: String?,
    @Json(name = "subscription_status") val subscriptionStatus: String,
    @Json(name = "subscription_expires_at") val subscriptionExpiresAt: String?,
    @Json(name = "is_verified") val isVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MessageResponse(val message: String)

// ── Blacklist DTOs ───────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BlacklistVersionResponse(
    val version: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class BlacklistResponse(
    @Json(name = "global_version") val globalVersion: String,
    val platforms: List<PlatformDto>
)

@JsonClass(generateAdapter = true)
data class PlatformDto(
    val id: Long,
    val name: String,
    val category: String,
    val keywords: List<String>,
    @Json(name = "version_hash") val versionHash: String,
    @Json(name = "updated_at") val updatedAt: String
)

// ── Subscription DTOs ────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SubscriptionStatusResponse(
    @Json(name = "is_subscribed") val isSubscribed: Boolean,
    @Json(name = "subscription_status") val subscriptionStatus: String,
    @Json(name = "subscription_expires_at") val subscriptionExpiresAt: String?
)

@JsonClass(generateAdapter = true)
data class InitiateSubscriptionResponse(
    val message: String,
    @Json(name = "checkout_request_id") val checkoutRequestId: String
)

// ── Report DTOs ──────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BlockedReportRequest(
    @Json(name = "platform_id") val platformId: Long?,
    @Json(name = "matched_keyword") val matchedKeyword: String?,
    @Json(name = "amount_attempted") val amountAttempted: Double?,
    @Json(name = "raw_stk_text") val rawStkText: String?,
    @Json(name = "paybill_detected") val paybillDetected: String?,
    @Json(name = "action_taken") val actionTaken: String = "blocked"
)

@JsonClass(generateAdapter = true)
data class ReportItemDto(
    val id: Long,
    val platform: PlatformMinDto?,
    @Json(name = "matched_keyword") val matchedKeyword: String?,
    @Json(name = "amount_attempted") val amountAttempted: String?,
    @Json(name = "action_taken") val actionTaken: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class PlatformMinDto(
    val id: Long,
    val name: String,
    val category: String
)

@JsonClass(generateAdapter = true)
data class PaginatedReports(
    @Json(name = "current_page") val currentPage: Int,
    val data: List<ReportItemDto>,
    val total: Int,
    @Json(name = "per_page") val perPage: Int
)

// ── Error ────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>? = null,
    val code: String? = null
)
