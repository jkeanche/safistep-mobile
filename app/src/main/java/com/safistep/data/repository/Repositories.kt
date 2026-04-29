package com.safistep.data.repository

import com.safistep.data.local.SafiStepPreferences
import com.safistep.data.local.dao.BlockHistoryDao
import com.safistep.data.local.dao.PlatformDao
import com.safistep.data.local.dao.SyncMetaDao
import com.safistep.data.local.entity.BlockHistoryEntity
import com.safistep.data.local.entity.PlatformEntity
import com.safistep.data.local.entity.SyncMetaEntity
import com.safistep.data.remote.api.SafiStepApi
import com.safistep.data.remote.dto.*
import com.safistep.utils.ApiResult
import com.safistep.utils.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// ── Auth Repository ───────────────────────────────────────────
@Singleton
class AuthRepository @Inject constructor(
    private val api: SafiStepApi,
    private val prefs: SafiStepPreferences
) {
    suspend fun requestOtp(phone: String, purpose: String = "registration"): ApiResult<MessageResponse> =
        safeApiCall { api.requestOtp(RequestOtpRequest(phone, purpose)) }

    suspend fun verifyOtp(phone: String, code: String): ApiResult<VerifyOtpResponse> =
        safeApiCall { api.verifyOtp(VerifyOtpRequest(phone, code)) }

    suspend fun setPassword(tempToken: String, password: String, name: String?): ApiResult<AuthResponse> {
        val result = safeApiCall {
            api.setPassword(SetPasswordRequest(tempToken, name, password, password))
        }
        if (result is ApiResult.Success) {
            val user = result.data.user
            prefs.saveAuthSession(result.data.token, user.id, user.phone, user.name)
            // subscription_status may be null for brand-new users — default to "inactive"
            prefs.saveSubscription(
                user.subscriptionStatus ?: "inactive",
                user.subscriptionExpiresAt
            )
            prefs.clearTempSession()
        }
        return result
    }

    suspend fun login(phone: String, password: String): ApiResult<AuthResponse> {
        val result = safeApiCall { api.login(LoginRequest(phone, password)) }
        if (result is ApiResult.Success) {
            val user = result.data.user
            prefs.saveAuthSession(result.data.token, user.id, user.phone, user.name)
            prefs.saveSubscription(
                user.subscriptionStatus ?: "inactive",
                user.subscriptionExpiresAt
            )
        }
        return result
    }

    suspend fun logout(): ApiResult<MessageResponse> {
        val result = safeApiCall { api.logout() }
        prefs.clearSession()
        return result
    }

    suspend fun saveTempSession(tempToken: String, phone: String) =
        prefs.saveTempSession(tempToken, phone)

    suspend fun getTempToken() = prefs.getTempToken()
    suspend fun getTempPhone() = prefs.getTempPhone()
}

// ── Blacklist Repository ──────────────────────────────────────
@Singleton
class BlacklistRepository @Inject constructor(
    private val api: SafiStepApi,
    private val platformDao: PlatformDao,
    private val syncMetaDao: SyncMetaDao,
    private val prefs: SafiStepPreferences
) {
    val platforms: Flow<List<PlatformEntity>> = platformDao.getAllFlow()

    suspend fun syncIfNeeded(): SyncResult {
        return try {
            val versionResult = safeApiCall { api.getBlacklistVersion() }
            if (versionResult !is ApiResult.Success) return SyncResult.Error("Network unavailable")

            val remoteVersion = versionResult.data.version
            val localVersion  = syncMetaDao.get(SyncMetaEntity.KEY_BLACKLIST_VERSION)

            if (remoteVersion == localVersion) return SyncResult.UpToDate

            val listResult = safeApiCall { api.getBlacklist() }
            if (listResult !is ApiResult.Success) return SyncResult.Error("Failed to download blacklist")

            val entities = listResult.data.platforms.map { dto ->
                PlatformEntity(
                    id          = dto.id,
                    name        = dto.name,
                    category    = dto.category,
                    keywords    = dto.keywords.joinToString(","),
                    versionHash = dto.versionHash,
                    updatedAt   = dto.updatedAt
                )
            }
            platformDao.replaceAll(entities)
            syncMetaDao.set(SyncMetaEntity.KEY_BLACKLIST_VERSION, remoteVersion)
            syncMetaDao.set(SyncMetaEntity.KEY_LAST_SYNC, System.currentTimeMillis().toString())
            prefs.setLastSync(System.currentTimeMillis())

            SyncResult.Updated(entities.size)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun findMatchingPlatform(stkText: String): PlatformEntity? {
        val lower = stkText.lowercase()
        return platformDao.getAllForMatching().firstOrNull { platform ->
            platform.keywordList().any { keyword -> lower.contains(keyword) }
        }
    }
}

sealed class SyncResult {
    object UpToDate : SyncResult()
    data class Updated(val count: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

// ── Subscription Repository ───────────────────────────────────
@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: SafiStepApi,
    private val prefs: SafiStepPreferences
) {
    val subscriptionStatus: Flow<String>   = prefs.subscriptionStatus
    val subscriptionExpires: Flow<String?> = prefs.subscriptionExpires

    suspend fun getStatus(): ApiResult<SubscriptionStatusResponse> {
        val result = safeApiCall { api.getSubscriptionStatus() }
        if (result is ApiResult.Success) {
            prefs.saveSubscription(
                result.data.subscriptionStatus ?: "inactive",
                result.data.subscriptionExpiresAt
            )
        }
        return result
    }

    /** Start a 3-day free trial (no payment needed) */
    suspend fun startTrial(): ApiResult<StartTrialResponse> {
        val result = safeApiCall { api.startTrial() }
        if (result is ApiResult.Success) {
            prefs.saveSubscription(result.data.subscriptionStatus, result.data.subscriptionExpiresAt)
        }
        return result
    }

    /** Initiate a paid subscription (monthly or yearly) via M-Pesa STK push */
    suspend fun initiate(plan: String, phone: String): ApiResult<InitiateSubscriptionResponse> =
        safeApiCall { api.initiateSubscription(InitiateSubscriptionRequest(plan = plan, phone = phone)) }

    /** Poll status after STK push — updates local prefs on success */
    suspend fun pollStatus(): Boolean {
        val result = safeApiCall { api.getSubscriptionStatus() }
        if (result is ApiResult.Success && result.data.subscriptionStatus == "active") {
            prefs.saveSubscription("active", result.data.subscriptionExpiresAt)
            return true
        }
        return false
    }
}

// ── Report Repository ─────────────────────────────────────────
@Singleton
class ReportRepository @Inject constructor(
    private val api: SafiStepApi,
    private val blockHistoryDao: BlockHistoryDao
) {
    val recentHistory: Flow<List<BlockHistoryEntity>> = blockHistoryDao.getRecentFlow(50)
    val blockedCount: Flow<Int>    = blockHistoryDao.getBlockedCountFlow()
    val totalSaved: Flow<Double?>  = blockHistoryDao.getTotalSavedFlow()

    suspend fun recordBlock(
        platformId: Long?,
        platformName: String?,
        matchedKeyword: String?,
        amountAttempted: Double?,
        rawStkText: String?,
        paybillDetected: String?,
        actionTaken: String = "blocked"
    ): Long = blockHistoryDao.insert(
        BlockHistoryEntity(
            platformId      = platformId,
            platformName    = platformName,
            matchedKeyword  = matchedKeyword,
            amountAttempted = amountAttempted,
            rawStkText      = rawStkText,
            paybillDetected = paybillDetected,
            actionTaken     = actionTaken,
            synced          = false
        )
    )

    suspend fun syncPendingReports() {
        val unsynced = blockHistoryDao.getUnsynced()
        val synced   = mutableListOf<Long>()
        unsynced.forEach { event ->
            val result = safeApiCall {
                api.submitReport(
                    BlockedReportRequest(
                        platformId      = event.platformId,
                        matchedKeyword  = event.matchedKeyword,
                        amountAttempted = event.amountAttempted,
                        rawStkText      = event.rawStkText,
                        paybillDetected = event.paybillDetected,
                        actionTaken     = event.actionTaken
                    )
                )
            }
            if (result is ApiResult.Success) synced.add(event.id)
        }
        if (synced.isNotEmpty()) blockHistoryDao.markSynced(synced)
    }

    suspend fun getMyReports(page: Int = 1): ApiResult<PaginatedReports> =
        safeApiCall { api.getMyReports(page) }
}