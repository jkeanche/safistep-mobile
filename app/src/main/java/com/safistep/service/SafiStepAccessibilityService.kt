package com.safistep.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.safistep.data.local.SafiStepPreferences
import com.safistep.data.repository.BlacklistRepository
import com.safistep.data.repository.ReportRepository
import com.safistep.utils.extractAmount
import com.safistep.utils.extractPaybill
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SafiStepAccessibilityService : AccessibilityService() {

    @Inject lateinit var blacklistRepo: BlacklistRepository
    @Inject lateinit var reportRepo: ReportRepository
    @Inject lateinit var prefs: SafiStepPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Packages that trigger STK prompts
    private val stkPackages = setOf(
        "com.android.phone",
        "com.mediatek.phone",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "net.one97.paytm",           // some ROM overlays
        "com.safaricom.mysafaricom"
    )

    // Window titles/text patterns that indicate an STK dialog
    private val stkTriggerKeywords = listOf(
        "paybill", "pay bill", "buy goods", "lipa na m-pesa",
        "send money", "m-pesa", "mpesa", "stk", "safaricom"
    )

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Only react to window state changes (dialog appearing)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        scope.launch {
            // Check subscription — unsubscribed users don't get protection
            val subStatus = prefs.getSubStatusSync()
            if (subStatus != "active") return@launch

            val rootNode = rootInActiveWindow ?: return@launch
            val fullText = extractAllText(rootNode)

            if (fullText.isBlank()) return@launch

            // Filter: only process if this looks like an STK prompt
            val isStk = stkTriggerKeywords.any { keyword ->
                fullText.lowercase().contains(keyword)
            }
            if (!isStk) return@launch

            // Check against blacklist
            val matchedPlatform = blacklistRepo.findMatchingPlatform(fullText)
            matchedPlatform ?: return@launch

            val paybill = extractPaybill(fullText)
            val amount  = extractAmount(fullText)
            val matchedKeyword = matchedPlatform.keywordList()
                .firstOrNull { fullText.lowercase().contains(it) }

            // Show overlay on main thread
            withContext(Dispatchers.Main) {
                showBlockingOverlay(
                    platformName    = matchedPlatform.name,
                    platformId      = matchedPlatform.id,
                    platformCategory = matchedPlatform.category,
                    matchedKeyword  = matchedKeyword,
                    amount          = amount,
                    paybill         = paybill,
                    rawText         = fullText.take(300)
                )
            }
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo?): String {
        node ?: return ""
        val sb = StringBuilder()
        if (!node.text.isNullOrBlank()) sb.append(node.text).append(" ")
        if (!node.contentDescription.isNullOrBlank()) sb.append(node.contentDescription).append(" ")
        for (i in 0 until node.childCount) {
            sb.append(extractAllText(node.getChild(i)))
        }
        return sb.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBlockingOverlay(
        platformName: String,
        platformId: Long,
        platformCategory: String,
        matchedKeyword: String?,
        amount: Double?,
        paybill: String?,
        rawText: String
    ) {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_PLATFORM_NAME, platformName)
            putExtra(OverlayService.EXTRA_PLATFORM_ID, platformId)
            putExtra(OverlayService.EXTRA_PLATFORM_CATEGORY, platformCategory)
            putExtra(OverlayService.EXTRA_MATCHED_KEYWORD, matchedKeyword)
            putExtra(OverlayService.EXTRA_AMOUNT, amount ?: 0.0)
            putExtra(OverlayService.EXTRA_PAYBILL, paybill)
            putExtra(OverlayService.EXTRA_RAW_TEXT, rawText)
        }
        startForegroundService(intent)
    }

    override fun onInterrupt() {
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
