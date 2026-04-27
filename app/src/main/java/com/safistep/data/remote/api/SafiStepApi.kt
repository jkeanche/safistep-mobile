package com.safistep.data.remote.api

import com.safistep.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SafiStepApi {

    // ── Auth ─────────────────────────────────────────────────

    @POST("auth/request-otp")
    suspend fun requestOtp(@Body body: RequestOtpRequest): Response<MessageResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("auth/set-password")
    suspend fun setPassword(@Body body: SetPasswordRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    // ── Blacklist ────────────────────────────────────────────

    @GET("blacklist/version")
    suspend fun getBlacklistVersion(): Response<BlacklistVersionResponse>

    @GET("blacklist")
    suspend fun getBlacklist(): Response<BlacklistResponse>

    // ── Subscriptions ────────────────────────────────────────

    @GET("subscriptions/status")
    suspend fun getSubscriptionStatus(): Response<SubscriptionStatusResponse>

    @POST("subscriptions/initiate")
    suspend fun initiateSubscription(): Response<InitiateSubscriptionResponse>

    // ── Reports ──────────────────────────────────────────────

    @POST("reports")
    suspend fun submitReport(@Body body: BlockedReportRequest): Response<MessageResponse>

    @GET("reports/me")
    suspend fun getMyReports(@Query("page") page: Int = 1): Response<PaginatedReports>
}
