package com.safistep.data.remote.api

import com.safistep.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SafiStepApi {

    // ── Auth ─────────────────────────────────────────────────

    @POST("api/v1/auth/request-otp")
    suspend fun requestOtp(@Body body: RequestOtpRequest): Response<MessageResponse>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("api/v1/auth/set-password")
    suspend fun setPassword(@Body body: SetPasswordRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<MessageResponse>

    // ── Blacklist ────────────────────────────────────────────

    @GET("api/v1/blacklist/version")
    suspend fun getBlacklistVersion(): Response<BlacklistVersionResponse>

    @GET("api/v1/blacklist")
    suspend fun getBlacklist(): Response<BlacklistResponse>

    // ── Subscriptions ────────────────────────────────────────

    @GET("api/v1/subscriptions/status")
    suspend fun getSubscriptionStatus(): Response<SubscriptionStatusResponse>

    @POST("api/v1/subscriptions/trial")
    suspend fun startTrial(): Response<StartTrialResponse>

    @POST("api/v1/subscriptions/initiate")
    suspend fun initiateSubscription(@Body body: InitiateSubscriptionRequest): Response<InitiateSubscriptionResponse>

    // ── Reports ──────────────────────────────────────────────

    @POST("api/v1/reports")
    suspend fun submitReport(@Body body: BlockedReportRequest): Response<MessageResponse>

    @GET("api/v1/reports/me")
    suspend fun getMyReports(@Query("page") page: Int = 1): Response<PaginatedReports>
}