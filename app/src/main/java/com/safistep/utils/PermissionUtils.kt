package com.safistep.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

private const val TAG = "PermissionUtils"

// ── Accessibility ─────────────────────────────────────────────

/**
 * Opens the accessibility settings page, trying the most direct route first
 * and falling back gracefully so the user always lands somewhere useful.
 *
 * Priority:
 *  1. Direct deep-link to SafiStep's own accessibility detail page (API 26+)
 *  2. Generic ACTION_ACCESSIBILITY_SETTINGS
 *  3. App's own settings page (last resort)
 */
fun openAccessibilitySettings(context: Context) {
    val serviceComponent = ComponentName(
        context.packageName,
        "com.safistep.service.SafiStepAccessibilityService"
    )

    // Try 1: direct deep-link to this specific service (Android 8+ / API 26+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // This extra scrolls directly to our service on supported ROMs
                val bundle = android.os.Bundle()
                bundle.putString(
                    ":settings:fragment_args_key",
                    serviceComponent.flattenToString()
                )
                putExtra(":settings:fragment_args_key", serviceComponent.flattenToString())
                putExtra(":settings:show_fragment_args", bundle)
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened accessibility settings (deep-link attempt)")
            return
        } catch (e: Exception) {
            Log.w(TAG, "Deep-link accessibility failed: ${e.message}")
        }
    }

    // Try 2: generic accessibility settings
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened generic accessibility settings")
        return
    } catch (e: Exception) {
        Log.w(TAG, "Generic accessibility settings failed: ${e.message}")
    }

    // Try 3: application details settings (user can navigate from there)
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened app details settings as fallback")
    } catch (e: Exception) {
        Log.e(TAG, "All accessibility setting attempts failed: ${e.message}")
    }
}

// ── Overlay / Draw Over Other Apps ────────────────────────────

/**
 * Opens the overlay permission settings, trying the most direct route first.
 *
 * Priority:
 *  1. ACTION_MANAGE_OVERLAY_PERMISSION scoped to this package
 *  2. ACTION_MANAGE_OVERLAY_PERMISSION without scope
 *  3. App's application detail settings
 *  4. ACTION_MANAGE_ALL_APPS_ACCESSIBILITY (some ROMs)
 *  5. ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION (Android 11+ alternative)
 *  6. Special manufacturer intents (Samsung, Xiaomi, etc.)
 *  7. General settings root
 */
fun openOverlaySettings(context: Context) {
    // Try 1: scoped overlay permission intent (standard)
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened scoped overlay settings")
        return
    } catch (e: Exception) {
        Log.w(TAG, "Scoped overlay settings failed: ${e.message}")
    }

    // Try 2: unscoped overlay permission intent
    try {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened unscoped overlay settings")
        return
    } catch (e: Exception) {
        Log.w(TAG, "Unscoped overlay settings failed: ${e.message}")
    }

    // Try 3: app details (some ROMs allow overlay toggle from here)
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened app details as overlay fallback")
        return
    } catch (e: Exception) {
        Log.w(TAG, "App details fallback failed: ${e.message}")
    }

    // Try 4: Special apps permission screen (works on some Android 11+ devices)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened special apps permission screen")
            return
        } catch (e: Exception) {
            Log.w(TAG, "Special apps permission failed: ${e.message}")
        }
    }

    // Try 5: Manufacturer-specific intents
    try {
        // Samsung devices
        val samsungIntent = Intent().apply {
            action = "com.samsung.android.sm.ACTION_MANAGE_OVERLAY_PERMISSION"
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (samsungIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(samsungIntent)
            Log.d(TAG, "Opened Samsung overlay settings")
            return
        }
    } catch (e: Exception) {
        Log.w(TAG, "Samsung overlay settings failed: ${e.message}")
    }

    try {
        // Xiaomi devices
        val xiaomiIntent = Intent().apply {
            action = "miui.intent.action.APP_PERM_EDITOR"
            putExtra("extra_pkgname", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (xiaomiIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(xiaomiIntent)
            Log.d(TAG, "Opened Xiaomi permission editor")
            return
        }
    } catch (e: Exception) {
        Log.w(TAG, "Xiaomi permission editor failed: ${e.message}")
    }

    try {
        // Huawei devices
        val huaweiIntent = Intent().apply {
            component = ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity")
            putExtra("package", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (huaweiIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(huaweiIntent)
            Log.d(TAG, "Opened Huawei permission manager")
            return
        }
    } catch (e: Exception) {
        Log.w(TAG, "Huawei permission manager failed: ${e.message}")
    }

    // Try 6: Write settings (sometimes enables overlay as side effect)
    try {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened write settings as overlay alternative")
        return
    } catch (e: Exception) {
        Log.w(TAG, "Write settings fallback failed: ${e.message}")
    }

    // Try 7: root settings (last resort)
    try {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened root settings as last resort")
    } catch (e: Exception) {
        Log.e(TAG, "All overlay setting attempts failed: ${e.message}")
    }
}

// ── Permission state checks ───────────────────────────────────

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.safistep.service.SafiStepAccessibilityService"
    return try {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabled.split(":").any { it.equals(service, ignoreCase = true) }
    } catch (e: Exception) {
        Log.w(TAG, "isAccessibilityServiceEnabled check failed: ${e.message}")
        false
    }
}

fun isOverlayPermissionGranted(context: Context): Boolean =
    Settings.canDrawOverlays(context)

// ── Device detection helpers ────────────────────────────────

/**
 * Returns the device manufacturer for providing targeted instructions
 */
fun getDeviceManufacturer(): String {
    return Build.MANUFACTURER.lowercase()
}

/**
 * Returns whether this device likely needs special handling for overlay permissions
 */
fun needsSpecialOverlayHandling(): Boolean {
    val manufacturer = getDeviceManufacturer()
    return manufacturer.contains("samsung") || 
           manufacturer.contains("xiaomi") || 
           manufacturer.contains("huawei") ||
           manufacturer.contains("oppo") ||
           manufacturer.contains("vivo") ||
           manufacturer.contains("oneplus") ||
           manufacturer.contains("realme")
}

/**
 * Returns user-friendly instructions for the specific device manufacturer
 */
fun getOverlayInstructions(): String {
    return when (getDeviceManufacturer()) {
        "samsung" -> "Tap ⋮ menu → 'Allow restricted settings' if overlay toggle is disabled"
        "xiaomi" -> "Go to Settings → Apps → Manage Apps → SafiStep → Permissions → Display over other apps"
        "huawei" -> "Go to Settings → Apps → Apps → SafiStep → Permissions → Display over other apps"
        "oppo", "vivo", "oneplus", "realme" -> "Look for 'Display over other apps' in App Permissions or Special Access"
        else -> "If the toggle is disabled, look for 'Allow restricted settings' in the menu"
    }
}